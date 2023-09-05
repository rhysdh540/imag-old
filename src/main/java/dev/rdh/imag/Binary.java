package dev.rdh.imag;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static dev.rdh.imag.Binary.OS.*;
import static dev.rdh.imag.Main.err;

public enum Binary {
		OXIPNG,
		ZOPFLI,
		ZOPFLIPNG,
		PNGOUT,
		OPTIVORBIS,
		PNGFIX;

		private final Path path;
		private static OS os;

	static File tempDir;

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
		if(tempDir == null) {
			 tempDir = new File(System.getProperty("user.home") + File.separator + ".imag-bin");
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

		var target = new File(tempDir, filename);

		if(target.exists())
			return target.toPath();

		var resource = "imag/bin/" + os.toString() + "/" + filename;

		try(InputStream stream = Binary.class.getClassLoader()
				.getResourceAsStream(resource)) {
			if(stream == null) {
				err("Could not find binary " + name().toLowerCase() + " in classpath");
				return null;
			}

			tempDir.mkdirs();

			Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);

			if(os == LINUX || os == MAC) {
				target.setExecutable(true);
			}
			return target.toPath();
		} catch(Exception e) {
			err("Could not unpack binary " + name().toLowerCase());
			e.printStackTrace();
			return null;
		}
	}
}
