package dev.rdh.imag;

import java.io.File;

import static dev.rdh.imag.Main.err;

public enum BinaryLoader {
		OXIPNG,
		ZOPFLI,
		ZOPFLIPNG,
		PNGOUT,
		OPTIVORBIS,
		PNGFIX;



	public static void load() {

	}

	private static String asResource(String binary) {
		String os;
		String a = System.getProperty("os.name").toLowerCase();
		if (a.contains("win")) {
			os = "win";
		} else if (a.contains("mac")) {
			os = "mac";
		} else {
			os = "nix";
		}

		ClassLoader classLoader = BinaryLoader.class.getClassLoader();
		binary = "imag/bin/" + os + "/" + binary;
		binary = binary.replace("/", File.separator);
		if(os.equals("win")) {
			binary += ".exe";
		}

		var result = classLoader.getResource(binary);

		if(result == null) { // If the resource is not found, try to use the system's installation
			err("Could not find binary " + binary + " in classpath, trying system installation");
			return binary;
		}

		String path = result.getPath();

		if(os.matches("nix|mac")) {
			ProcessBuilder pb = new ProcessBuilder("chmod", "+x", path);
			try {
				pb.start().waitFor();
			} catch(Exception e) {
				err("Could not make " + binary + " executable");
				e.printStackTrace();
			}
		}
		return path;
	}
}
