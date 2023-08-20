package dev.rdh.imag;

import dev.rdh.imag.processors.*;

import jdk.jfr.Percentage;

import java.io.File;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	// Statistics
	static @Percentage double maxReduction = 0.0;
	static long maxReductionSize = 0;

	// Settings
	static boolean
		png = true,
		nbt = true,
		ogg = true;

	static {
		main("/Users/rhys/coding/mc/Railway/common/src/main/resources/assets/railways/textures/block/bogeys/narrow", "-p=1");
		System.exit(0);
	}

	public static void main(String... args) {

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
					  -t, --maxthreads=<threads>   The maximum number of threads to use. Default is half of the number of available processors.
					  -h, --help                   Display this help message.
					""");
			return;
		}

		var path = args[0];
		var passes = 3;
		var maxThreads = Runtime.getRuntime().availableProcessors() / 2;

		args = Arrays.copyOfRange(args, 1, args.length);

		for(var arg : args) {
			var value = arg.substring(arg.indexOf('='));
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
					passes = Integer.parseInt(value);
				} else if(arg.startsWith("--maxthreads") || arg.startsWith("-t")) {
					maxThreads = Integer.parseInt(value);
				} else {
					err("Unknown argument: " + arg);
				}
			}
		}

		run(path, passes, maxThreads);
	}

	/**
	 * Run the program.
	 * @param path the path to the file or directory to process.
	 * @param passes the number of times to run the processors.
	 * @param maxThreads the maximum number of threads to use.
	 */
	public static void run(String path, int passes, int maxThreads) {

		var input = new File(path);

		if(!input.exists()) {
			err("Specified input does not exist!");
		}

		var filesToProcess = input.isDirectory() ?
				getFiles(input)
				: List.of(input);

		if(filesToProcess.isEmpty()) {
			err("No files found!");
			return;
		}

		long preSize = filesToProcess.stream()
				.mapToLong(File::length)
				.sum();

		log("Processing " + filesToProcess.size() + " files " + passes + " times...");

		final int finalPasses = passes + 1;

		var executor = Executors.newFixedThreadPool(maxThreads);
		var asyncs = new CompletableFuture<?>[filesToProcess.size()];

		for(; passes > 0; passes--) {
			log("\nRunning pass " + (finalPasses - passes) + "...");

			for(int i = 0; i < filesToProcess.size(); i++) {
				final File f = filesToProcess.get(i);
				asyncs[i] = CompletableFuture.runAsync(() -> process(f), executor);
			}

			try {
				CompletableFuture.allOf(asyncs).join();
			} catch(CompletionException e) {
				err(e.getMessage());
			}
		}

		try {
			//noinspection ResultOfMethodCallIgnored
			new File(".workdir").getCanonicalFile().delete();
		} catch(Exception ignored) {}

		long postSize = filesToProcess.stream()
				.mapToLong(File::length)
				.sum();

		long totalSavings = preSize - postSize;

		String s = "\n\033[1;4m" + "Done!" + "\033[0m\n" +
				"Saved " + totalSavings + " bytes (" + round(((double) totalSavings / preSize) * 100) + "% of " + preSize + ") - up to " + round(maxReduction) + "%\n" +
				"Max reduction: " + maxReductionSize + " bytes";

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

		int exitCode = switch(name.substring(name.lastIndexOf('.'))) {
			case ".png" -> processImage(file);
			case ".nbt" -> processNbt(file);
			case ".ogg" -> processOgg(file);
			default -> {
				err("Unknown file type: " + name.substring(name.lastIndexOf('.')));
				yield 1;
			}
		};

		if(exitCode != 0) {
			err("Error processing " + file.getName() + "!");
			return;
		}

		long postSize = file.length();
		double reduction = 100.0 - ((double) postSize / (double) preSize) * 100.0;

		maxReduction = Math.max(maxReduction, reduction);
		maxReductionSize = Math.max(maxReductionSize, preSize - postSize);

		var sb = new StringBuilder("\nProcessed " + file.getName() + '\n');

		if(reduction > 0.0) {
			sb.append("File size decreased: ").append(preSize).append(" -> ").append(postSize).append('\n');
			sb.append("Savings of ").append(preSize - postSize).append(" bytes (").append(round(reduction)).append("%)");
		} else {
			sb.append("File size not changed");
		}
		log(sb.toString());
	}

	/**
	 * Run an image through all of the image processors.
	 * @param file the image to process. Guaranteed to be a {@code .png} file.
	 * @return the exit code of the process.
	 */
	public static int processImage(File file) {
		var processors = Arrays.asList(
				ZopfliPngProcessor.newInstance(),
				OxiPngProcessor.newFirstInstance(),
				OxiPngProcessor.newSecondInstance(),
				PngOutProcessor.newInstance(),
				PngFixProcessor.newInstance()
		);

		for(var p : processors) {
			try {
				p.process(file);
			} catch(Exception e) {
				return 1;
			}
		}
		return 0;
	}

	/**
	 * Run an NBT file through the NBT processor.
	 * @param file the NBT file to process. Guaranteed to be a {@code .nbt} file.
	 * @return the exit code of the process.
	 */
	public static int processNbt(File file) {
		try {
			NbtFileProcessor.newInstance().process(file);
		} catch(Exception e) {
			return 1;
		}
		return 0;
	}

	/**
	 * Run an OGG file through the OGG processor.
	 * @param file the audio file to process. Guaranteed to be a {@code .ogg} file.
	 * @return the exit code of the process.
	 */
	public static int processOgg(File file) {
		try {
			OptiVorbisProcessor.newInstance().process(file);
		} catch(Exception e) {
			return 1;
		}
		return 0;
	}

	/**
	 * Round a double to 2 decimal places.
	 * @param value the value to round.
	 * @return the rounded value.
	 */
	private static String round(double value) {
		return String.format("%.2f", value);
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
	 * Log an error message to the console.
	 * @param message the message to log.
	 */
	public static void err(String message) {
		log("\033[31;4m" + message + "\033[0m");
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
}
