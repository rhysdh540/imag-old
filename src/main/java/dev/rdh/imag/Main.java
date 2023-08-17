package dev.rdh.imag;

import dev.rdh.imag.processors.AbstractFileProcessor;
import dev.rdh.imag.processors.image.PngFixProcessor;
import dev.rdh.imag.processors.image.OxiPngProcessor;
import dev.rdh.imag.processors.image.PngOutProcessor;
import dev.rdh.imag.processors.image.ZopfliPngProcessor;
import jdk.jfr.Percentage;

import java.io.File;
import java.util.*;

public class Main {
	public static boolean debug;

	static @Percentage double maxReduction = 0.0;

	public static void main(String[] args) {
		maina("/Users/rhys/Desktop/base.png", "3", "--debug");
	}

	public static void maina(String... args) {
		List<String> argList = new ArrayList<>(Arrays.asList(args));

		if(argList.contains("--debug")) {
			debug = true;
			argList.remove("--debug");
		}

		if(argList.contains("-d")) {
			debug = true;
			argList.remove("-d");
		}

		if(argList.size() < 2) {
			err("Not enough arguments!");
			return;
		}

		int passes;
		try {
			passes = Integer.parseInt(argList.get(1));
		} catch (NumberFormatException e) {
			err("Invalid number of passes!");
			return;
		}

		File input = new File(argList.get(0));

		if(!input.exists()) {
			err("Specified input does not exist!");
		}

		List<File> filesToProcess = input.isDirectory() ?
				getFiles(input)
				: List.of(input);

		log("Processing " + filesToProcess.size() + " files " + passes + " times...");

		final int finalPasses = passes + 1;

		for(; passes > 0; passes--) {
			log("Running pass " + (finalPasses - passes) + "...");

			for(File file : filesToProcess) {
				int code = process(file);

				if(code != 0) {
					err("Error processing " + file.getName() + "!");
					return;
				}
			}
		}
	}

	public static void log(String message) {
		System.out.println(message);
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
	public static int process(File file) {
		debug("Processing " + file.getName() + "...");
		if(file.isDirectory()) {
			err("Directory found! This should not happen!");
			return 1;
		}

		long preSize = file.length();

		int exitCode =  switch (file.getName().toLowerCase().substring(file.getName().lastIndexOf('.'))) {
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
		}

		long postSize = file.length();
		double reduction = 100.0 - ((double) postSize / (double) preSize) * 100.0;
		maxReduction = Math.max(maxReduction, reduction);

		log("Processed " + file.getName());
		log("Savings of " + (preSize - postSize) + " bytes (" + reduction + "%)");
		return exitCode;
	}

	/**
	 * Run an image through all of the image processors.
	 * @param file the image to process. Guaranteed to be a {@code .png} file.
	 */
	public static int processImage(File file) {
		List<AbstractFileProcessor> processors = Arrays.asList(
				ZopfliPngProcessor.INSTANCE,
				OxiPngProcessor.FIRST,
				OxiPngProcessor.SECOND,
				PngOutProcessor.INSTANCE,
				PngFixProcessor.INSTANCE
		);
		for (AbstractFileProcessor p : processors) {
			try {
				p.process(file);
			} catch (Exception e) {
				return 1;
			}
		}
		return 0;
	}

	public static int processNbt(File file) {
		return 0;
	}

	public static int processOgg(File file) {
		return 0;
	}
}
