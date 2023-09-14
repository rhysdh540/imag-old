package dev.rdh.imag.util;

import dev.rdh.imag.Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static dev.rdh.imag.util.Utils.err;
import static dev.rdh.imag.util.Utils.log;

public class Versioning {
	public static class Version implements Comparable<Version> {
		private final int major;
		private final int minor;
		private final Character patch;

		private final boolean invalid;

		public Version(String version) {
			if(version == null) {
				invalid = true;
				major = 0;
				minor = 0;
				patch = null;
				return;
			}

			invalid = false;

			String[] parts = version.split("\\.");
			if(parts.length < 2) {
				err("Invalid version found: " + version);
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
		}

		@Override
		public int compareTo(Version o) {
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

	public static String getUrl(String path) {
		return "https://github.com/rhysdh540/imag/releases/latest/download/" + path;
	}

	public static void downloadNewVersionIfNecessary() {
		Version local = Versioning.getLocalVersion();
		Version online = Versioning.getOnlineVersion();
		if(!online.isNewerThan(local)) {
			log("imag is up to date");
			return;
		}

		log("Downloading new version: ${online}");
		try(InputStream stream = Utils.getOnline(getUrl("imag-${online.toString()}.jar"))) {
			if(stream == null) throw new IOException("URL not valid");

			File f = new File(Main.MAINDIR, "imag.jar");
			Files.copy(stream, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			log("imag v${online} downloaded to ${f.getAbsolutePath()}");
			log("Restarting...");
		} catch (IOException e) {
			err("Failed to download new version", e);
		}
	}

	public static Version getLocalVersion() {
		try(InputStream resource = Utils.localResource("imag/version.txt")) {
			if(resource == null) throw new IOException("Resource not found");
			String s = new String(resource.readAllBytes());
			return new Version(s);
		} catch (IOException e) {
			err("Failed to read version.txt: ${e.getMessage()}");
			return new Version("0.0");
		}
	}

	public static Version getOnlineVersion() {
		try(InputStream resource = Utils.getOnline(getUrl("version.txt"))) {
			if(resource == null) throw new IOException("URL not valid");
			String s = new String(resource.readAllBytes());
			return new Version(s);
		} catch (IOException e) {
			err("Failed to read online version.txt: ${e.getMessage()}");
			return new Version("0.0");
		}
	}
}
