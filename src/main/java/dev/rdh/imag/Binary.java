package dev.rdh.imag;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static dev.rdh.imag.Binary.OS.*;

public enum Binary {
	OXIPNG,
	ZOPFLI,
	ZOPFLIPNG,
	PNGOUT,
	OPTIVORBIS,
	PNGFIX

	;

	private final Path path;

	private static OS os;
	private static File binariesDir;

	enum OS {
		MAC,
		WINDOWS,
		LINUX;

		@Override
		public String toString() {
			return switch(this) {
				case MAC -> "mac";
				case WINDOWS -> "win";
				case LINUX -> "nix";
			};
		}
	}

	private static void makeStuff() {
		if (os == null) {
			String a = System.getProperty("os.name").toLowerCase();
			if (a.contains("win")) {
				os = WINDOWS;
			} else if (a.contains("mac")) {
				os = MAC;
			} else {
				os = LINUX;
			}
		}
		if(binariesDir == null) {
			 binariesDir = new File(System.getProperty("user.home") + File.separator + ".imag-bin");
		}
	}

	Binary() {
		makeStuff();
		this.path = unpack();
	}

	@Override
	public String toString() {
		return path == null ? null : path.toFile().getAbsolutePath();
	}

	public static void load() {}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private Path unpack() {
		String filename = name().toLowerCase();
		if(os == WINDOWS)
			filename += ".exe";

		var target = new File(binariesDir, filename);

		if(target.exists())
			return target.toPath();

		var resource = "imag/bin/" + os.toString() + "/" + filename;

		try(InputStream stream = Binary.class.getClassLoader()
				.getResourceAsStream(resource)) {
			if(stream == null) {
				Main.logger.error("Could not find binary " + name().toLowerCase() + " in classpath");
				return null;
			}

			binariesDir.mkdirs();

			Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);

			if(os == LINUX || os == MAC) {
				target.setExecutable(true);
			}
			return target.toPath();
		} catch(Exception e) {
			Main.logger.error("Could not unpack binary " + name().toLowerCase(), e);
			return null;
		}
	}
}
