package dev.rdh.imag;

import java.io.File;

import static dev.rdh.imag.Main.err;
import static dev.rdh.imag.Main.log;

public enum Binary {
		OXIPNG,
		ZOPFLI,
		ZOPFLIPNG,
		PNGOUT,
		OPTIVORBIS,
		PNGFIX;

		private String path;

	Binary() {
		String targetPath = name().toLowerCase();

		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			os = "win";
		} else if (os.contains("mac")) {
			os = "mac";
		} else {
			os = "nix";
		}

		ClassLoader classLoader = Binary.class.getClassLoader();
		targetPath = "imag/bin/" + os + "/" + targetPath;
		targetPath = targetPath.replace("/", File.separator);

		if(os.equals("win")) {
			targetPath += ".exe";
		}

		var result = classLoader.getResource(targetPath);

		if(result == null) { // If the resource is not found, try to use the system's installation
			err("Could not find binary " + name().toLowerCase() + " in classpath, trying system installation");
			this.path = targetPath;
			return;
		}

		String path = result.getPath();

		if(os.matches("nix|mac")) {
			ProcessBuilder pb = new ProcessBuilder("chmod", "+x", path);
			try {
				pb.start().waitFor();
			} catch(Exception e) {
				err("Could not make " + targetPath + " executable");
				e.printStackTrace();
			}
		}
		log("Found binary " + name().toLowerCase() + " at " + path);
		this.path = path;
	}

	@Override
	public String toString() {
		return path;
	}

	public static void load() {}
}
