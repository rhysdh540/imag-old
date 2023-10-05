package dev.rdh.imag.util;

import dev.rdh.imag.Main;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static dev.rdh.imag.util.Utils.err;
import static dev.rdh.imag.util.Utils.log;

/**
 * A class for handling versioning. Includes functions for downloading the latest version of imag from GitHub releases.
 */
public class Versioning {
	/**
	 * A version of the form {@code major.minor.patch}.
	 */
	public static class Version implements Comparable<Version> {
		private final int major;
		private final int minor;
		private final Character patch;

		private final boolean invalid;

		public static Version from(@Nullable String version) {
			return new Version(version);
		}

		private Version(@Nullable String version) {
			if(version == null) {
				invalid = true;
				major = 0;
				minor = 0;
				patch = null;
				return;
			}

			String[] parts = version.split("\\.");
			if(parts.length < 2) {
				err("Invalid version found: " + version);
				invalid = true;
				major = 0;
				minor = 0;
				patch = null;
				return;
			}

			int temp;
			try {
				temp = Integer.parseInt(parts[0]);
			} catch (NumberFormatException e) {
				err("Invalid major version found: ${parts[0]}");
				temp = 0;
			}
			major = temp;

			try {
				temp = Integer.parseInt(String.valueOf(parts[1].charAt(0)));
			} catch (NumberFormatException e) {
				err("Invalid minor version found: ${parts[1].charAt(0)}");
				temp = 0;
			}
			minor = temp;

			if(parts[1].length() == 1) {
				patch = null;
			} else {
				patch = parts[1].charAt(1);
			}

			invalid = false;
		}

		@Override
		public int compareTo(@NotNull Version o) {
			if(invalid) return o.invalid ? 0 : -1;
			if(major != o.major) return major - o.major;
			if(minor != o.minor) return minor - o.minor;
			if(patch == null) return o.patch == null ? 0 : -1;
			if(o.patch == null) return 1;
			return patch.compareTo(o.patch);
		}

		public boolean isNewerThan(Version o) {
			return compareTo(o) > 0;
		}

		@Override
		public String toString() {
			return major + "." + minor + (patch == null ? "" : patch);
		}
	}

	/**
	 * Get the URL for the latest release of a file.
	 * @param path the path to the file.
	 * @return the URL to the latest release of the file.
	 */
	public static String getUrl(String path) {
		return "https://github.com/rhysdh540/imag/releases/latest/download/" + path;
	}

	/**
	 * Download the latest version of imag if necessary.
	 * <p>This is the main function of this class.</p>
	 */
	public static void downloadNewVersionIfNecessary() {
		Version local = Versioning.getLocalVersion();
		Version online = Versioning.getOnlineVersion();
		if(!online.isNewerThan(local)) {
			log("imag is up to date");
			return;
		}

		log("Downloading imag: ${online}");
		try(InputStream stream = Utils.onlineResource(getUrl("imag-${online.toString()}.jar"))) {
			if(stream == null) throw new IOException("URL not valid or you are offline");

			File f = new File(Main.MAINDIR, "imag.jar");
			Files.copy(stream, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			log("imag v${online} downloaded to ${f.getAbsolutePath()}");
			log("Restarting...");
		} catch (IOException e) {
			err("Failed to download version.txt", e);
		}
	}

	/**
	 * Get the local version of imag.
	 * @return the local version of imag.
	 */
	public static Version getLocalVersion() {
		#if DEV
		//noinspection ConstantConditions
		if(true) return Version.from("100.0");
		#endif
		String iv = Versioning.class.getPackage().getImplementationVersion();
		if(iv == null) {
			err("Could not get local version");
			return Version.from(null);
		}
		iv = String.join("", iv.split("\\[|]|, "));
		return Version.from(iv);
	}

	/**
	 * Get the latest version of imag.
	 * <p>Downloads the file <a href="https://github.com/rhysdh540/imag/releases/latest/download/version.txt">https://github.com/rhysdh540/imag/releases/latest/download/version.txt</a> and checks the version it contains.</p>
	 * @return the latest version of imag.
	 */
	public static Version getOnlineVersion() {
		try(InputStream resource = Utils.onlineResource(getUrl("version.txt"))) {
			if(resource == null) throw new IOException("URL not valid");
			String s = new String(resource.readAllBytes());
			return Version.from(s);
		} catch (IOException e) {
			err("Failed to read online version.txt: ${e.getMessage()}");
			return Version.from(null);
		}
	}

	private Versioning(){}
}
