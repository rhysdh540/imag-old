package dev.rdh.imag;

import dev.rdh.imag.processors.*;

import jdk.jfr.Percentage;

import java.io.File;


import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class Main {

	// Statistics
	static @Percentage double maxReduction = 0.0;
	static long maxReductionSize = 0;

	// Settings
	static boolean
		png = true,
		nbt = true,
		ogg = true;

	static boolean quiet = false;

	public static final File WORKDIR = makeWorkDir();

	public static void main(String... args) {
		boolean debug = System.getenv("ideLaunch") != null;
		if(debug) {
			String a = "/users";
			args = new String[]{a, "-p=1"};
		}

		if(args.length < 1) {
			err("No input specified! Use --help or -h for usage.");
			return;
		}

		if(args[0].startsWith("--help") || args[0].startsWith("-h")) {
			log("""
					Usage: \033[4mimag <input> [options]\033[0m
					Options:
					  --disable=<filetypes>        Disable processing of the specified filetypes. Valid filetypes are png, nbt, and ogg.
					  -p, --passes=<passes>        The number of times to run the processors. Default is 3.
					  -h, --help                   Display this help message.
					  -q, --quiet                  Suppress individual log messages per file and just output for each pass and the ending statistics.
					""");
			return;
		}

		var path = args[0];
		var passes = 3;

		args = Arrays.copyOfRange(args, 1, args.length);

		for(var arg : args) {
			var value = arg.substring(arg.indexOf('=') + 1);

			if(arg.startsWith("--disable")) {
				String[] parts = arg.substring(arg.indexOf('=') + 1).split(",");
				for(var part : parts) {
					switch(part) {
						case "png" -> png = false;
						case "nbt" -> nbt = false;
						case "ogg" -> ogg = false;
						default -> err("Unknown file type: " + part);
					}
				}
			} else {
				if(arg.startsWith("--passes") || arg.startsWith("-p")) {
					try {
						passes = Integer.parseInt(value);
						if(passes < 1) {
							err("Number of passes must be at least 1!");
							return;
						}
					} catch (NumberFormatException e) {
						err("Invalid number of passes: '" + value + "'");
						return;
					}
				} else if(arg.equals("--quiet") || arg.equals("-q")) {
					quiet = true;
				} else {
					err("Unknown argument: " + arg);
				}
			}
		}
		Binary.load();

		var input = new File(path);

		if(!input.exists()) {
			err("Specified input does not exist!");
			return;
		}

		List<File> filesToProcess = input.isDirectory() ?
				getFiles(input)
				: List.of(input);

		if(filesToProcess.isEmpty()) {
			err("No files found!");
			return;
		}

		run(filesToProcess, passes);
	}

	/**
	 * Run the program.
	 * @param filesToProcess the files to process.
	 * @param passes the number of times to run the processors.
	 */
	public static void run(List<File> filesToProcess, int passes) {

		long preSize = size(filesToProcess);

		log("Processing " + filesToProcess.size() + " files " + plural(passes, "time") + "...");

		var startTime = System.currentTimeMillis();

		var asyncs = new CompletableFuture<?>[filesToProcess.size()];

		for(final int finalPasses = passes + 1; passes > 0; passes--) {
			log("\n\033[1;4mRunning pass " + (finalPasses - passes) + "...\033[0m");

			long prePassSize = size(filesToProcess);

			for(int i = 0; i < filesToProcess.size(); i++) {
				final File f = filesToProcess.get(i);
				asyncs[i] = CompletableFuture.runAsync(() -> process(f));
			}

			try {
				CompletableFuture.allOf(asyncs).join();
			} catch(CompletionException e) {
				err("Processing failed!", e);
			}

			long currentSize = size(filesToProcess);

			long currentSavings = prePassSize - currentSize;

			log("\nPass " + (finalPasses - passes) + " complete!\n" +
				"Saved " + plural(currentSavings, "byte") + "!");
			if(currentSavings == 0) {
				log("Savings are 0, stopping early");
				break;
			}
		}

		long postSize = size(filesToProcess);

		long totalSavings = preSize - postSize;

		String time = timeFromSecs(round((System.currentTimeMillis() - startTime) / 1e3));

		String s = "\n\033[1;4m" + "Done!" + "\033[0m\n" +
				"Took " + time + "\n" +
				"Saved " + plural(totalSavings, "byte") + " (" + round(((double) totalSavings / preSize) * 100) + "% of " + preSize + ") - up to " + round(maxReduction) + "%\n" +
				"Max reduction: " + plural(maxReductionSize, "byte");

		log(s);
		System.exit(0);
	}

	/**
	 * Process a file and run it (or any files inside of it) through the relevant processors.
	 * @param file the file to process. Guaranteed to not be a directory, to exist, and to end in {@code .png}, {@code .nbt}, or {@code .ogg}.
	 */
	public static void process(File file) {
		if(file.isDirectory()) {
			err("Directory found! This should not happen!");
			return;
		}

		long preSize = file.length();

		var name = file.getName().toLowerCase();

		Throwable t = switch(name.substring(name.lastIndexOf('.'))) {
			case ".png" -> processImage(file);
			case ".nbt" -> processNbt(file);
			case ".ogg" -> processOgg(file);
			default -> {
				err("Unknown file type: " + name.substring(name.lastIndexOf('.')));
				yield null;
			}
		};

		if(t != null) {
			err("Error processing " + file.getName() + "!", t);
		}

		long postSize = file.length();
		double reduction = 100.0 - ((double) postSize / (double) preSize) * 100.0;

		maxReduction = Math.max(maxReduction, reduction);
		maxReductionSize = Math.max(maxReductionSize, preSize - postSize);

		var sb = new StringBuilder("\nProcessed " + file.getName() + '\n');

		if(!quiet) {
			if (reduction > 0.0) {
				sb.append("File size decreased: ").append(format(preSize)).append(" -> ").append(plural(postSize, "byte")).append('\n');
				sb.append("Savings of ").append(plural(preSize - postSize, "byte")).append(" (").append(round(reduction)).append("%)");
			} else {
				sb.append("File size not changed");
			}
			log(sb.toString());
		}
	}

	/**
	 * Run an PNG file through all of the PNG processors.
	 * @param file the image to process. Guaranteed to be a {@code .png} file.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processImage(File file) {
		var processors = Arrays.asList(
				ZopfliPngProcessor.get(),
				OxiPngProcessor.getFirst(),
				OxiPngProcessor.getSecond(),
				PngOutProcessor.get(),
				PngFixProcessor.get()
		);

		for(var p : processors) {
			try {
				p.process(file);
			} catch(Exception e) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Run an NBT file through the NBT processor.
	 * @param file the NBT file to process. Guaranteed to be a {@code .nbt} file.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processNbt(File file) {
		try {
			NbtFileProcessor.get().process(file);
		} catch(Exception e) {
			return e;
		}
		return null;
	}

	/**
	 * Run an OGG file through the OGG processor.
	 * @param file the audio file to process. Guaranteed to be a {@code .ogg} file.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processOgg(File file) {
		try {
			OptiVorbisProcessor.get().process(file);
		} catch(Exception e) {
			return e;
		}
		return null;
	}

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
		log("\033[31;4m" + message + "\033[0m");
	}

	/**
	 * Format a number of a specific unit.
	 * @param num the number of bytes to format.
	 * @param unit the unit to use.
	 * @return the formatted number of the unit.
	 */
	private static String plural(double num, String unit) {
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
	private static String format(double d) {
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
	private static double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	/**
	 * Format a number of seconds into a human-readable time.
	 * @param secs the number of seconds to format.
	 * @return the formatted time.
	 */
	private static String timeFromSecs(double secs) {
		int hours = (int) (secs / 3600);
		int minutes = (int) ((secs % 3600) / 60);
		double seconds = secs % 60;

		return plural(hours, "hr") + " " + plural(minutes, "min") + " " + plural(seconds, "second");
	}

	/**
	 * Get all valid files in a directory.
	 * <p>
	 * Depending on the program's settings, this will only return files ending in {@code .png}, {@code .nbt}, or {@code .ogg}.
	 * @param dir the directory to get files from.
	 * @return a list of all valid files in the directory.
	 */
	private static List<File> getFiles(File dir) {
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

	private static File makeWorkDir() {
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

	private static long size(List<File> files) {
		long sum = 0L;
		for(File file : files) {
			sum += file.length();
		}
		return sum;
	}
}
