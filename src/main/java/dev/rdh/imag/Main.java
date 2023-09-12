package dev.rdh.imag;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static dev.rdh.imag.Processing.*;
import static dev.rdh.imag.Utils.*;

public class Main {

	// Statistics
	static double maxReduction = 0.0;
	static long maxReductionSize = 0;

	// Settings
	static boolean
		png = true,
		nbt = true,
		ogg = true;

	static boolean quiet = false;
	static boolean slow = false;
	static int passes = 3;

	public static final File WORKDIR = makeWorkDir();

	#if DEV
	@SuppressWarnings({"ParameterCanBeLocal", "ConstantValue"})
	#endif
	public static void main(String... args) {
		initArgs();
		#if DEV
		String a = "/users/rhys/coding/Unity Projects/twosteptravel.github.io/";
		args = new String[]{a, "-p=1"};
		#endif

		if(args.length < 1) {
			err("No input specified! Use --help or -h for usage.");
			return;
		}

		if(args[0].startsWith("--help") || args[0].startsWith("-h")) {
			log("""
					Usage: \033[4mimag <input> [options]\033[0m
					Options:
					  -p, --passes=<number>        The number of times to run the processors. Default is 3.
					  
					  --disable=<filetypes>        Disable processing of the specified filetypes (comma-separated).
					                               Valid filetypes are 'png', 'nbt', and 'ogg'.
					                               
					  -s, --slow                   Process files sequentially instead of asynchronously,
					                               which is less demanding on your computer but slower.
					                               
					  -h, --help                   Display this help message.
					  
					  -q, --quiet                  Suppress individual log messages per file
					                               and just output for each pass and the ending statistics.
					""");
			return;
		}

		String path = args[0];

		args = Arrays.copyOfRange(args, 1, args.length);

		for(String input : args) {
			String[] split = input.split("=", 2);

			String arg = split[0];
			String value = (split.length == 1) ? null : split[1];

			if(parseArg(arg, value))
				return;
		}
		Binary.load();

		File input = new File(path);

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
		log("Processing " + plural(filesToProcess.size(), "file") + " " + plural(passes, "time") + "...");

		long startTime = System.currentTimeMillis();
		long preSize = size(filesToProcess);

		if(!slow) asyncs = new CompletableFuture<?>[filesToProcess.size()];
		if(passes == 1) {
			pass(filesToProcess, -1);
		} else {
			for(int pass = 1; pass < passes + 1; pass++) {
				if(pass(filesToProcess, pass)) break;
			}
		}

		long postSize = size(filesToProcess);
		long endTime = System.currentTimeMillis();

		long totalSavings = preSize - postSize;
		double timeTaken = (endTime - startTime) / 1e3;
		double percentage = ((double) totalSavings / preSize) * 100;

		String s = "\n\033[1;4m" + "Done!" + "\033[0m\n" +
				"Took " + timeFromSecs(timeTaken) + "\n" +
				"Saved " + plural(totalSavings, "byte") + " (" + format(percentage) + "% of " + preSize + ") - up to " + format(maxReduction) + "%\n" +
				"Max reduction: " + plural(maxReductionSize, "byte");

		log(s);
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
		String name = file.getName().toLowerCase();
		long startTime = System.currentTimeMillis();

		Throwable t = switch(name.substring(name.lastIndexOf('.'))) {
			case ".png" -> processImage(file);
			case ".nbt" -> processNbt(file);
			case ".ogg" -> processOgg(file);
			default -> {
				err("Unknown file type: " + name.substring(name.lastIndexOf('.')));
				yield null;
			}
		};
		long endTime = System.currentTimeMillis();
		double timeTaken = (endTime - startTime) / 1e3;

		if(t != null) {
			err("Error processing ${file.getName()} !", t);
		}

		long postSize = file.length();
		double reduction = 100.0 - ((double) postSize / (double) preSize) * 100.0;

		maxReduction = Math.max(maxReduction, reduction);
		maxReductionSize = Math.max(maxReductionSize, preSize - postSize);

		if (quiet) return;
		StringBuilder sb = new StringBuilder("\nProcessed ${file.getName()} in ${timeFromSecs(timeTaken)}\n");

		if (reduction > 0.0) {
			sb.append("File size decreased: ").append(format(preSize)).append(" -> ").append(plural(postSize, "byte")).append('\n');
			sb.append("Savings of ").append(plural(preSize - postSize, "byte")).append(" (").append(format(reduction)).append("%)");
		} else {
			sb.append("File size not changed");
		}
		log(sb.toString());
	}

	private static CompletableFuture<Void> processAsync(File f) {
		return CompletableFuture.runAsync(() -> process(f));
	}

	private static CompletableFuture<?>[] asyncs;

	private static boolean pass(List<File> filesToProcess, int whatPassAmIOn) {
		boolean doLog = whatPassAmIOn != -1;

		if(doLog) log("\n\033[1;4mRunning pass " + whatPassAmIOn + "...\033[0m");

		long prePassSize = size(filesToProcess);

		for(int i = 0; i < filesToProcess.size(); i++) {
			final File f = filesToProcess.get(i);
			if(slow) {
				process(f);
			} else {
				asyncs[i] = processAsync(f);
			}
		}

		if(!slow) {
			try {
				CompletableFuture.allOf(asyncs).join();
			} catch (CompletionException e) {
				err("Processing failed!", e);
			}
			Arrays.fill(asyncs, null);
		}

		System.gc();

		long currentSavings = prePassSize - size(filesToProcess);

		if(doLog) log("\nPass " + whatPassAmIOn + " complete!\n" +
					  "Saved " + plural(currentSavings, "byte") + "!");

		if(currentSavings == 0) {
			log("Savings are 0, stopping early");
			return true;
		}
		return false;
	}

	static Map<Pair<String, String>, Function<String, Boolean>> args = new HashMap<>();

	private static void addArg(String longName, String shortName, Function<String, Boolean> action) {
		args.put(Pair.of(longName, shortName), action);
	}

	private static void initArgs() {
		addArg("--passes", "-p", value -> {
			try {
				int passes = Integer.parseInt(value);
				if(passes < 1) {
					err("Passes must be greater than 0!");
					return true;
				}
				Main.passes = passes;
			} catch(NumberFormatException e) {
				err("Invalid number of passes: ${value}");
				return true;
			}
			return false;
		});
		addArg("--disable", null, value -> {
			String[] values = value.split(",");
			for(String v : values) {
				switch(v) {
					case "png" -> png = false;
					case "nbt" -> nbt = false;
					case "ogg" -> ogg = false;
					default -> {
						err("Unknown filetype: ${v}");
						return true;
					}
				}
			}
			return false;
		});
		addArg("--slow", "-s", value -> slow = true);
		addArg("--quiet", "-q", value -> quiet = true);
	}

	private static boolean parseArg(String name, String value) {
		for(Pair<String, String> pair : args.keySet()) {
			if(name.equals(pair.first()) || name.equals(pair.second())) {
				return args.get(pair).apply(value);
			}
		}
		err("Unknown argument: ${name}");
		return true;
	}
}
