package dev.rdh.imag;

import java.io.File;
import java.io.IOException;
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

		String r = String.format("%,f", d);
		while(r.endsWith("0"))
			r = r.substring(0, r.length() - 1);
		return r;
	}

	/**
	 * Round a double to 2 decimal places.
	 * @param value the value to round.
	 * @return the rounded value.
	 */
	public static double round(double value) {
		return Math.round(value * 100.0) / 100.0;
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

		return plural(hours, "hr") + ' ' + plural(minutes, "min") + ' ' + plural(seconds, "second");
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

	public record Pair<F, S>(F first, S second) {
		public static <F, S> Pair<F, S> of(F first, S second) {
			return new Pair<>(first, second);
		}
	}
}
