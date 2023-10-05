package dev.rdh.imag.util;

import dev.rdh.imag.Main;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static dev.rdh.imag.util.Binary.OS.LINUX;
import static dev.rdh.imag.util.Binary.OS.MAC;
import static dev.rdh.imag.util.Binary.OS.WINDOWS;
import static dev.rdh.imag.util.StringUtils.err;

/**
 * Enum of all the binaries used by this program.
 */
public enum Binary {
	OXIPNG, ZOPFLI, ZOPFLIPNG, PNGOUT, OPTIVORBIS, PNGFIX,

	;

	private static OS os;
	private static File binariesDir;
	private final Path path;

	Binary() {
		makeStuff();
		this.path = unpack();
	}

	/**
	 * Sets the OS and binaries directory.
	 */
	private static void makeStuff() {
		if(os == null) {
			String a = System.getProperty("os.name").toLowerCase();
			if(a.contains("win")) {
				os = WINDOWS;
			} else if(a.contains("mac")) {
				os = MAC;
			} else {
				os = LINUX;
			}
		}
		if(binariesDir == null) {
			binariesDir = new File(Main.MAINDIR, "bin");
		}
	}

	public static void load() { }

	public String path() {
		return path == null ? null : path.toFile().getAbsolutePath();
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	/**
	 * Unpacks the binary from the jar to the binaries directory. If the binary already exists, it will not be unpacked.
	 * <p>This is required because the filesystem cannot execute commands that are inside zipped files (like jars). So, we have to take it and move it somewhere else and then run it from there.</p>
	 *
	 * @return The path to the binary.
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	private Path unpack() {
		String filename = name().toLowerCase();
		if(os == WINDOWS) filename += ".exe";

		var target = new File(binariesDir, filename);

		if(target.exists()) return target.toPath();

		var resource = FileUtils.sanitize("bin/" + os.toString() + "/" + filename);

		try(InputStream stream = FileUtils.localResource(resource)) {
			if(stream == null) {
				err("Could not find binary " + name().toLowerCase() + " in classpath");
				return null;
			}

			binariesDir.mkdirs();

			Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);

			target.setExecutable(true);
			return target.toPath();
		} catch (Exception e) {
			err("Could not unpack binary " + name().toLowerCase(), e);
			return null;
		}
	}

	enum OS {
		MAC, WINDOWS, LINUX;

		@Override
		public String toString() {
			return switch(this) {
				case MAC -> "mac";
				case WINDOWS -> "win";
				case LINUX -> "nix";
			};
		}
	}
}
