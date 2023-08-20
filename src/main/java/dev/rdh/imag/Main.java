package dev.rdh.imag;

import dev.rdh.imag.processors.*;

import jdk.jfr.Percentage;

import java.io.File;

import java.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	// Statistics
	static @Percentage double maxReduction = 0.0;
	static @Percentage double avgReduction = 0.0;
	static long totalBytes = 0;
	static long totalSavings = 0;


	// Settings
	static boolean
		png,
		nbt,
		ogg;

	public static void main(String[] args) {
		png = false;
		nbt = true;
		ogg = true;

		run("/Users/rhys/coding/mc/Railway/common/src/main/resources/assets/railways/", 3, 32);
	}

	/**
	 * Run the program.
	 * @param path the path to the file or directory to process.
	 * @param passes the number of times to run the processors.
	 * @param maxThreads the maximum number of threads to use.
	 */
	public static void run(String path, int passes, int maxThreads) {

		File input = new File(path);

		if(!input.exists()) {
			err("Specified input does not exist!");
		}

		List<File> filesToProcess = input.isDirectory() ?
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

		ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
		CompletableFuture<?>[] asyncs = new CompletableFuture<?>[filesToProcess.size()];

		for(; passes > 0; passes--) {
			log("\nRunning pass " + (finalPasses - passes) + "...");

			for(int i = 0; i < filesToProcess.size(); i++) {
				final File f = filesToProcess.get(i);
				asyncs[i] = CompletableFuture.runAsync(() -> process(f), executor);
			}

			try {
				CompletableFuture.allOf(asyncs).join();
			} catch (CompletionException e) {
				err(e.getMessage());
			}
		}

		try {
			new File(".workdir").getCanonicalFile().delete();
		} catch (Exception ignored) {}

		long postSize = filesToProcess.stream()
				.mapToLong(File::length)
				.sum();

		avgReduction = 100.0 - ((double) postSize / (double) preSize) * 100.0;

		StringBuilder sb = new StringBuilder("\n");

		sb.append("\033[1;4m").append("Done!").append("\033[0m\n");
		sb.append("Saved ").append(totalSavings).append(" bytes (").append(round(avgReduction)).append("% of ").append(totalBytes).append(")\n");
		sb.append("Max reduction: ").append(round(maxReduction)).append("%");

		log(sb.toString());

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

		String name = file.getName().toLowerCase();

		int exitCode = switch (name.substring(name.lastIndexOf('.'))) {
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

		totalBytes += preSize;
		totalSavings += preSize - postSize;

		StringBuilder sb = new StringBuilder("\nProcessed " + file.getName() + '\n');

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
		List<AbstractFileProcessor> processors = Arrays.asList(
				ZopfliPngProcessor.newInstance(),
				OxiPngProcessor.newFirstInstance(),
				OxiPngProcessor.newSecondInstance(),
				PngOutProcessor.newInstance(),
				PngFixProcessor.newInstance()
		);

		for (var p : processors) {
			try {
				p.process(file);
			} catch (Exception e) {
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
		} catch (Exception e) {
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
		} catch (Exception e) {
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
		synchronized (System.out) {
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
		List<File> files = new ArrayList<>();

		List<String> extensions = new ArrayList<>();
		if(png) extensions.add("png");
		if(nbt) extensions.add("nbt");
		if(ogg) extensions.add("ogg");

		String filter = "(?i).*\\.(?:" + String.join("|", extensions) + ")";

		log("Filter: " + filter);

		for(File file : Objects.requireNonNull(dir.listFiles())) {
			if(file.isDirectory())
				files.addAll(getFiles(file));
			else if(file.getName().matches(filter))
				files.add(file);
		}
		return files;
	}
}
