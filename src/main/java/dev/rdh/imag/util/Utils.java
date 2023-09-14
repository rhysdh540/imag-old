package dev.rdh.imag.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static dev.rdh.imag.Main.*;

public class Utils {
	/**
	 * Log a message to the console.
	 * @param message the message to log.
	 */
	public static void log(String message) {
		synchronized(System.out) {
			System.out.println(message);
		}
	}

	/**
	 * Log an error message and stack trace to the console.
	 * @param m the message to log.
	 * @param t the exception to log.
	 */
	@SuppressWarnings("all")
	public static void err(String m, Throwable t) {
		err(m);
		t.printStackTrace();
	}

	/**
	 * Log an error message to the console.
	 * @param message the message to log.
	 */
	public static void err(String message) {
		log("\033[31;4m${message}\033[0m");
	}

	/**
	 * Format a number of a specific unit.
	 * @param num the number of bytes to format.
	 * @param unit the unit to use.
	 * @return the formatted number of the unit.
	 */
	public static String plural(double num, String unit) {
		String result = format(num) + ' ' + unit;
		if(num != 1)
			result += 's';
		return result;
	}

	/**
	 * Format a number with commas.
	 * @param d the number to format.
	 * @return the formatted long.
	 */
	public static String format(double d) {
		if(d == (long) d)
			return String.format("%,d", (long) d);

		String r = String.format("%,.2f", d);
		while(r.endsWith("0"))
			r = r.substring(0, r.length() - 1);
		return r;
	}

	/**
	 * Format a number of seconds into a human-readable time.
	 * @param secs the number of seconds to format.
	 * @return the formatted time.
	 */
	public static String timeFromSecs(double secs) {
		int hours = (int) (secs / 3600);
		int minutes = (int) ((secs % 3600) / 60);
		double seconds = secs % 60;

		StringBuilder sb = new StringBuilder();

		if(hours != 0)
			sb.append(plural(hours, "hr")).append(' ');

		if(minutes != 0)
			sb.append(plural(minutes, "min")).append(' ');

		sb.append(String.format("%,.2f", seconds)).append("s");

		return sb.toString();
	}

	/**
	 * Get all valid files in a directory.
	 * <p>
	 * Depending on the program's settings, this will only return files ending in {@code .png}, {@code .nbt}, or {@code .ogg}.
	 * @param dir the directory to get files from.
	 * @return a list of all valid files in the directory.
	 */
	public static List<File> getFiles(File dir) {
		var files = new ArrayList<File>();

		var extensions = new ArrayList<String>();
		if(png) extensions.add("png");
		if(nbt) extensions.add("nbt");
		if(ogg) extensions.add("ogg");

		@SuppressWarnings("all")  // regex + string concat = ???
		var filter = "(?i).*\\.(?:" + String.join("|", extensions) + ")";

		//noinspection DataFlowIssue
		for (var file : dir.listFiles()) {
			if (file.isDirectory())
				files.addAll(getFiles(file));
			else if (file.getName().matches(filter))
				files.add(file);
		}
		return files;
	}

	public static File makeWorkDir() {
		try {
			File f = Files.createTempDirectory(".imag-workdir").toFile();
			f.deleteOnExit();
			return f;
		} catch (IOException e) {
			err("Could not create work directory!", e);
			System.exit(1);
		}
		return null;
	}

	public static long size(List<File> files) {
		long sum = 0L;
		for(File file : files) {
			sum += file.length();
		}
		return sum;
	}

	public static InputStream getOnline(String url) {
		try {
			return new URL(url).openStream();
		} catch (IOException e) {
			return null;
		}
	}

	public static InputStream localResource(String path) {
		return Utils.class.getClassLoader().getResourceAsStream(path);
	}

	/**
	 * Fix a path name to be correctly capitalized and have the correct path separator.
	 * @param path the path to fix.
	 * @return the fixed path.
	 */
	public static String sanitize(String path) {
		path = path.replace('\\', '/');
		path = path.replace('/', File.separatorChar);
		File file = new File(path);

		return recursiveSanitize(file);
	}

	private static String recursiveSanitize(File file) {
		if (file == null)
			return "";

		String currentName = file.getName();
		File parentFile = file.getParentFile();

		if (parentFile == null) {
			return currentName;
		}

		String parentPath = recursiveSanitize(parentFile);

		File[] matchingFiles = parentFile.listFiles((dir, name) -> name.equalsIgnoreCase(currentName));

		if (matchingFiles != null) {
			for (File matchingFile : matchingFiles) {
				if (matchingFile.getName().equalsIgnoreCase(currentName)) {
					return parentPath + File.separator + matchingFile.getName();
				}
			}
		}

		return parentPath + File.separator + currentName;
	}

	public record Pair<F, S>(F first, S second) {
		public static <F, S> Pair<F, S> of(F first, S second) {
			return new Pair<>(first, second);
		}
	}
}