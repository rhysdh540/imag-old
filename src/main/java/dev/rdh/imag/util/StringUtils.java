package dev.rdh.imag.util;

import org.jetbrains.annotations.NotNull;

public class StringUtils {
	private StringUtils() { }

	/**
	 * Log a message to the console.
	 *
	 * @param message the message to log.
	 */
	public static void log(String message) {
		synchronized(System.out) {
			System.out.println(message);
		}
	}

	// Overloaded log methods
	public static void log(int message) { log(String.valueOf(message)); }

	public static void log(long message) { log(String.valueOf(message)); }

	public static void log(double message) { log(String.valueOf(message)); }

	public static void log(float message) { log(String.valueOf(message)); }

	public static void log(boolean message) { log(String.valueOf(message)); }

	public static void log(char message) { log(String.valueOf(message)); }

	public static void log(byte message) { log(String.valueOf(message)); }

	public static void log(short message) { log(String.valueOf(message)); }

	public static void log(Object message) { log(String.valueOf(message)); }

	/**
	 * Log an error message and stack trace to the console.
	 *
	 * @param m the message to log.
	 * @param t the exception to log.
	 */
	@SuppressWarnings("all")
	public static void err(String m, @NotNull Throwable t) {
		err(m);
		t.printStackTrace();
	}

	/**
	 * Log an error message to the console.
	 *
	 * @param message the message to log.
	 */
	public static void err(String message) {
		log("\033[31;4m${message}\033[0m");
	}

	public static void err(Throwable t) {
		err(t.getMessage(), t);
	}

	/**
	 * Format a number of a specific unit.
	 *
	 * @param num  the number of bytes to format.
	 * @param unit the unit to use.
	 * @return the formatted number of the unit.
	 */
	public static String plural(double num, String unit) {
		return format(num) + ' ' + unit + (num != 1 ? "s" : "");
	}

	/**
	 * Format a number with commas.
	 *
	 * @param d the number to format.
	 * @return the formatted long.
	 */
	public static String format(double d) {
		if(d == (long) d) return String.format("%,d", (long) d);

		String r = String.format("%,.2f", d);
		while(r.endsWith("0")) r = r.substring(0, r.length() - 1);
		return r;
	}

	/**
	 * Format a number of seconds into a human-readable time.
	 *
	 * @param secs the number of seconds to format.
	 * @return the formatted time.
	 */
	public static String timeFromSecs(double secs) {
		int hours = (int) (secs / 3600);
		int minutes = (int) ((secs % 3600) / 60);
		double seconds = secs % 60;

		StringBuilder sb = new StringBuilder();

		if(hours != 0) sb.append(plural(hours, "hr")).append(' ');

		if(minutes != 0) sb.append(plural(minutes, "min")).append(' ');

		sb.append(format(seconds)).append("s");

		return sb.toString();
	}

	/**
	 * A simple pair class.
	 *
	 * @param <F>    the type of the first value.
	 * @param <S>    the type of the second value.
	 * @param first  the first value.
	 * @param second the second value.
	 */
	public record Pair<F, S>(F first, S second) {
		public static <F, S> Pair<F, S> of(F first, S second) {
			return new Pair<>(first, second);
		}
	}
}
