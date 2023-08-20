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
	public static boolean debug;

	static @Percentage double maxReduction = 0.0;
	static @Percentage double avgReduction = 0.0;

	public static void main(String[] args) {
		run("/Users/rhys/coding/mc/Railway/common/src/main/resources/assets/railways/ponder", 3, 32, true);
	}

	public static void run(String path, int passes, int maxThreads, boolean debug) {
		Main.debug = debug;

		File input = new File(path);

		if(!input.exists()) {
			err("Specified input does not exist!");
		}

		List<File> filesToProcess = input.isDirectory() ?
				getFiles(input)
				: List.of(input);

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

		avgReduction /= (double) filesToProcess.size() * (double) finalPasses;
		log("\n\nDone!");
		log("Max reduction: " + maxReduction + "%");
		log("Average reduction: " + avgReduction + "%");
		System.exit(0);
	}

	public static void log(String message) {
		synchronized (System.out) {
			System.out.println(message);
		}
	}

	public static void err(String message) {
		log("\033[31;4m" + message + "\033[0m");
	}

	public static void debug(String message) {
		if(debug) {
			log(message);
		}
	}

	private static List<File> getFiles(File dir) {
		List<File> files = new ArrayList<>();

		for(File file : Objects.requireNonNull(dir.listFiles())) {
			if(file.isDirectory())
				files.addAll(getFiles(file));
			else if(file.getName().matches("(?i).*\\.(png|nbt|ogg)"))
				files.add(file);
		}
		return files;
	}

	/**
	 * Process a file and run it (or any files inside of it) through the relevant processors.
	 * @param file the file to process. Guaranteed to not be a directory, to exist, and to end in {@code .png}, {@code .nbt}, or {@code .ogg}.
	 */
	public static void process(File file) {
		debug("Processing " + file.getName() + "...");
		if(file.isDirectory()) {
			err("Directory found! This should not happen!");
			return;
		}

		long preSize = file.length();

		String name = file.getName().toLowerCase();

		int exitCode =  switch (name.substring(name.lastIndexOf('.'))) {
			case ".png" -> processImage(file);
			case ".nbt" -> processNbt(file);
			case ".ogg" -> processOgg(file);
			default -> {
				err("Unknown file type!");
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
		avgReduction += reduction;

		StringBuilder sb = new StringBuilder("\nProcessed " + file.getName() + '\n');
		if(reduction > 0.0) {
			sb.append("File size decreased: ").append(preSize).append(" -> ").append(postSize).append('\n');
			sb.append("Savings of ").append(preSize - postSize).append(" bytes (").append(reduction).append("%)");
		} else {
			sb.append("File size not changed");
		}
		log(sb.toString());
	}

	/**
	 * Run an image through all of the image processors.
	 * @param file the image to process. Guaranteed to be a {@code .png} file.
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

	public static int processNbt(File file) {
		try {
			NbtFileProcessor.newInstance().process(file);
		} catch (Exception e) {
			return 1;
		}
		return 0;
	}

	public static int processOgg(File file) {
		try {
			OptiVorbisProcessor.newInstance().process(file);
		} catch (Exception e) {
			return 1;
		}
		return 0;
	}
}
