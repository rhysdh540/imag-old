package dev.rdh.imag.util;

import dev.rdh.imag.Main;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static dev.rdh.imag.util.Binary.OS.*;

/**
 * Enum of all the binaries used by this program.
 */
public enum Binary {
	OXIPNG, ZOPFLI, ZOPFLIPNG, PNGOUT, OPTIVORBIS, PNGFIX,

	;

	private final Path path;
	private final boolean isOSInstalled;

	private static final OS os;
	private static final File binariesDir;

	Binary() {
		boolean temp;
		try {
			temp = isPreInstalled(name().toLowerCase());
		} catch (Exception e) {
			Main.LOGGER.error("Could not check if binary " + this + " is pre-installed", e);
			temp = false;
		}
		isOSInstalled = temp;
		if(isOSInstalled) {
			path = null;
			return;
		}

		path = unpack();
		if(path != null) {
			Main.LOGGER.info("Found binary " + this + " at " + path());
		} else {
			Main.LOGGER.error("Could not find binary " + this);
		}
	}

	static {
		String a = System.getProperty("os.name").toLowerCase();
		if(a.contains("win")) {
			os = WINDOWS;
		} else if(a.contains("mac")) {
			os = MAC;
		} else {
			os = OS.OTHER;
		}

		binariesDir = new File(Main.MAINDIR, "bin");
	}

	public static void load() {
		if(os == OS.OTHER) {
			Main.LOGGER.warn("Unsupported OS: " + System.getProperty("os.name"));
		}
	}

	public String path() {
		return isOSInstalled ? toString()
							 : path == null ? null
							 : path.toAbsolutePath().toString();
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	/**
	 * Unpacks the binary from the jar to the binaries directory. If the binary already exists, it will not be unpacked.
	 * <p>This is required because the filesystem cannot execute commands that are inside zipped files (like jars). So, we have to take it and move it somewhere else and then run it from there.</p>
	 *
	 * @return The path to the binary, or null if it does not exist.
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	private Path unpack() {
		String filename = name().toLowerCase();
		if(os == WINDOWS) filename += ".exe";

		var target = new File(binariesDir, filename);

		if(target.exists()) return target.toPath();

		if(os == OS.OTHER) {
			return null;
		}

		String resource = "bin" + File.separator + filename;

		try {
			InputStream stream = FileUtils.localResource(resource);
			if(stream == null) {
				Main.LOGGER.warn("Could not find binary " + this + " in jar");
				return null;
			}

			binariesDir.mkdirs();
			Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			target.setExecutable(true);
			return target.toPath();
		} catch (Exception e) {
			Main.LOGGER.error("Could not unpack binary " + this, e);
			return null;
		}
	}

	private boolean isPreInstalled(String cmd) throws Exception {
		String which = os == WINDOWS ? "where" : "which";
		ProcessBuilder pb = new ProcessBuilder(which, cmd)
				.redirectError(ProcessBuilder.Redirect.DISCARD);
		Process p = pb.start();
		return p.waitFor() == 0;
	}

	enum OS {
		MAC, WINDOWS, OTHER;

		@Override
		public String toString() {
			return switch(this) {
				case MAC -> "mac";
				case WINDOWS -> "win";
				default -> "other";
			};
		}
	}
}
