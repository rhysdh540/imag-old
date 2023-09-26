package dev.rdh.imag;

import dev.rdh.imag.util.Binary;
import dev.rdh.imag.util.Utils;
import dev.rdh.imag.util.Versioning;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static dev.rdh.imag.util.Processing.*;
import static dev.rdh.imag.util.Utils.*;

public class Main {

	// Statistics
	static double maxReduction = 0.0;
	static long maxReductionSize = 0;

	// Settings
	public static boolean
		png = true,
		nbt = true,
		ogg = true;

	static boolean quiet = false;
	static boolean slow = false;
	static boolean encode = true;
	static boolean quitEarly = true;
	static int threads = 8;
	static int passes = 3;

	public static final File WORKDIR = makeWorkDir();
	public static final File MAINDIR = new File(System.getProperty("user.home") + File.separator + ".imag");

	#if DEV
	@SuppressWarnings({"ParameterCanBeLocal", "ConstantValue", "unused", "RedundantSuppression"})
	#endif
	public static void main(String... args) {
		System.setProperty("apple.awt.UIElement", "true"); // Don't show the dock icon on macOS
		Utils.echo(false);
		log("\033[?25l");
		initArgs();
		#if DEV
		String a = "/users/rhys/downloads/actual downloads/ntl.png";
		args = new String[]{a};
		#endif

		if(args.length < 1) {
			err("No input specified! Use --help or -h for usage.");
			log("\033[?25h");
			return;
		}

		if(args[0].startsWith("--version")) {
			log("imag version " + Versioning.getLocalVersion());
			log("\033[?25h");
			return;
		}

		if(args[0].startsWith("--update")) {
			Versioning.downloadNewVersionIfNecessary();
			log("\033[?25h");
			return;
		}

		if(args[0].startsWith("--help") || args[0].startsWith("-h")) {
			log("""
					imag: a tool to reduce the size of png, nbt, and ogg files.
     				
					Usage: \033[4mimag <input> [options]\033[0m
					Options:
					  -p, --passes=<number>  The number of times to run the processors. Default: 3.
	
					  --disable=<filetypes>  Disable processing of the specified filetypes (comma-separated).
					                         Valid filetypes are 'png', 'nbt', and 'ogg'.
					                         
					  -t, --max-threads      The number of threads to use. Default: 8.
					                         
					  -s, --slow             Process files sequentially instead of asynchronously,
					                         which is less demanding on your computer but slower.
					                         
					  -h, --help             Display this help message.
					                         
					  -q, --quiet            Suppress individual log messages per file
					                         and just output for each pass and the ending statistics.
					                         
					  -n, --no-encode        Disable the reencoding of PNGs before processing them.
					  
					  -f, --force            Continue to process even if savings are 0.
					                         
					  --version              Display the version of imag you are using.
	
					  --update               Update imag to the latest version.
					""");
			log("\033[?25h");
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

		File input = new File(sanitize(path));

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

		run(filesToProcess);
		log("\033[?25h");
	}

	/**
	 * Run the program.
	 * @param files the files to process.
	 */
	public static void run(List<File> files) {
		log("Processing " + plural(files.size(), "file") + " " + plural(passes, "time") + "...");

		long startTime = System.currentTimeMillis();
		long preSize = size(files);

		asyncs = new CompletableFuture<?>[threads];
		if(passes == 1) {
			pass(files, -1);
		} else for(int pass = 1; pass < passes + 1; pass++) {
			if(pass(files, pass)) break;
		}

		long postSize = size(files);
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

	private static Deque<File> siblingFiles = new ArrayDeque<>();

	/**
	 * Process a file and run it (or any files inside of it) through the relevant processors.
	 * @param file the file to process. Guaranteed to not be a directory, to exist, and to end in {@code .png}, {@code .nbt}, or {@code .ogg}.
	 */
	public static void process(File file, boolean reencodeIfImage) {
		if(file == null) return;
		if(file.isDirectory()) {
			err("Directory found! This should not happen!");
			return;
		}

		Throwable t = null;

		long preSize = file.length();
		String name = file.getName().toLowerCase();
		long startTime = System.currentTimeMillis();

		var temp = file.getParentFile().toPath().resolve(".imag-" + file.getName()).toFile();

		try {
			Files.copy(file.toPath(), temp.toPath());
		} catch(Exception e) {
			t = e;
			if(!temp.delete()) {
				t = new Exception("Could not delete temp file!", e);
			}
		}

		Throwable t2 = switch (name.substring(name.lastIndexOf('.'))) {
			case ".png" -> processImage(temp, reencodeIfImage);
			case ".nbt" -> processNbt(temp);
			case ".ogg" -> processOgg(temp);
			default -> {
				err("Unknown file type: " + name.substring(name.lastIndexOf('.')));
				yield null;
			}
		};

		if(t2 != null) {
			t = new Exception("Error processing ${file.getName()} !", t2);
		}

		long endTime = System.currentTimeMillis();
		double timeTaken = (endTime - startTime) / 1e3;

		if(temp.length() < file.length()) {
			try {
				Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch(Exception e) {
				t = new Exception("Could not move temp file!", e);
			}
		}

		if(t != null) {
			err(t);
		}

		long postSize = file.length();
		double reduction = 100.0 - ((double) postSize / (double) preSize) * 100.0;

		maxReduction = Math.max(maxReduction, reduction);
		maxReductionSize = Math.max(maxReductionSize, preSize - postSize);

		if (quiet) return;
		StringBuilder sb = new StringBuilder("\nProcessed ${file.getName()} in ${timeFromSecs(timeTaken)}\n");

		if (reduction > 0.0) {
			sb.append("File size decreased: ").append(format(preSize)).append(" -> ").append(plural(postSize, "byte")).append('\n')
			  .append("Savings of ").append(plural(preSize - postSize, "byte")).append(" (").append(format(reduction)).append("%)");
		} else if (reduction < 0.0) {
			sb.append("File size increased! This should not happen!");
		} else {
			sb.append("File size not changed");
		}

		log(sb);
	}

	private static CompletableFuture<?>[] asyncs;

	private static boolean pass(List<File> files, int whatPassAmIOn) {
		boolean doLog = whatPassAmIOn != -1;

		Deque<File> filesToProcess = new ArrayDeque<>(files);

		if(doLog) log("\n\033[1;4mRunning pass ${whatPassAmIOn}...\033[0m");

		long prePassSize = size(files);

		boolean reencodeImage = (whatPassAmIOn == 1 || doLog) && encode;

		if(slow) {
			while(!filesToProcess.isEmpty()) {
				process(filesToProcess.poll(), reencodeImage);
			}
		} else {
			for (int i = 0; i < asyncs.length; i++) {
				asyncs[i] = CompletableFuture.runAsync(() -> {
					while (true) {
						File file;
						synchronized (filesToProcess) {
							file = filesToProcess.poll();
						}
						if (file == null) break;
						process(file, reencodeImage);
					}
				});
			}

			try {
				CompletableFuture.allOf(asyncs).join();
			} catch (CompletionException e) {
				err("Processing failed!", e);
			}
		}

		System.gc();

		long currentSavings = prePassSize - size(files);

		if(doLog) log("\nPass ${whatPassAmIOn} complete!\nSaved " + plural(currentSavings, "byte") + "!");

		if(currentSavings == 0 && quitEarly) {
			log("Savings are 0, stopping early");
			return true;
		}
		return false;
	}

	private static final Map<Pair<String, String>, Function<String, Boolean>> args = new HashMap<>();

	private static void addArg(String longName, String shortName, Function<String, Boolean> action) {
		args.put(Pair.of(longName, shortName), action);
	}

	private static void addArg(String longName, String shortName, Runnable action) {
		addArg(longName, shortName, value -> {
			action.run();
			return false;
		});
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

		addArg("--max-threads", "-t", value -> {
			try {
				int threads = Integer.parseInt(value);
				if(threads < 1) {
					err("Threads must be greater than 0!");
					return true;
				}
				Main.threads = threads;
			} catch(NumberFormatException e) {
				err("Invalid number of threads: ${value}");
				return true;
			}
			return false;
		});

		addArg("--slow", "-s", () -> slow = true);
		addArg("--quiet", "-q", () -> quiet = true);
		addArg("--no-encode", "-n", () -> encode = false);
		addArg("--force", "-f", () -> quitEarly = false);
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
