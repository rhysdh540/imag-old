package dev.rdh.imag.processors;

import java.io.File;
import java.util.List;

public class Util {
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

		return plural(hours, "hr") + " " + plural(minutes, "min") + " " + plural(seconds, "second");
	}

	/**
	 * Get the size of a list of files.
	 * @param files the files to get the size of.
	 * @return the size of the files in bytes.
	 */
	public static long size(List<File> files) {
		long sum = 0L;
		for(File file : files) {
			sum += file.length();
		}
		return sum;
	}

	/**
	 * Delete a file or directory recursively.
	 * @param dir the file or directory to delete.
	 */
	public static void deleteRecursively(File dir) {
		if (!dir.isDirectory()) {
			dir.delete();
			return;
		}
		File[] files = dir.listFiles();
		if(files == null) return;

		for(File file : files) {
			deleteRecursively(file);
		}
	}
}
