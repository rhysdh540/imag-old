package dev.rdh.imag;

import dev.rdh.imag.util.Binary;
import dev.rdh.imag.util.EpicLogger;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static dev.rdh.imag.util.FileUtils.*;
import static dev.rdh.imag.util.Processing.*;
import static dev.rdh.imag.util.StringUtils.*;

public class Main {

	public static final File WORKDIR = makeWorkDir();
	public static final File MAINDIR = new File(System.getProperty("user.home") + File.separator + ".imag");

	public static final EpicLogger LOGGER = new EpicLogger("imag")
			.disableTrace()
			.disableDebug()
			.enableInfo();

	private static final Map<Pair<String, String>, BiFunction<Options, String, Boolean>> args = new HashMap<>();

	static final Options options = new Options();
	static double maxReduction = 0.0;
	static long maxReductionSize = 0;
	private static CompletableFuture<?>[] asyncs;

	#if DEV
	@SuppressWarnings({ "ParameterCanBeLocal", "ConstantValue", "unused", "RedundantSuppression" })
	#endif public static void main(String... args) {
		preMainSetup();
		#if DEV
		String a = "/Users/rhys/documents/.png";
		args = new String[]{ a };
		#endif

		if(args.length < 1) {
			err("No input specified! Use --help or -h for usage.");
			return;
		}

		if(args[0].startsWith("--version")) {
			log("imag version " + Versioning.getLocalVersion());
			return;
		}

		if(args[0].startsWith("--update")) {
			Versioning.downloadNewVersionIfNecessary();
			return;
		}

		if(args[0].startsWith("--help") || args[0].startsWith("-h")) {
			log("""
					imag: a tool to reduce the size of png, nbt, and ogg files.
									
					Usage: \033[4mimag [options] <input>\033[0m
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
					  
					  -l, --no-log           Disable logging to the log file.
					  
					  -f, --force            Continue to process even if savings are 0.
					                         
					  --version              Display the version of imag you are using.
						
					  --update               Update imag to the latest version.
					""");
			return;
		}

		String path = args[args.length - 1];

		args = Arrays.copyOf(args, args.length - 1);

		for(String input : args) {
			String[] split = input.split("=", 2);

			String arg = split[0];
			String value = (split.length == 1) ? null : split[1];

			if(parseArg(options, arg, value)) return;
		}

		LOGGER.file(new File(MAINDIR, "logs" + File.separator + "latest.log"));

		Binary.load();

		File input = new File(sanitize(path));

		if(!input.exists()) {
			err("Specified input does not exist!");
			return;
		}

		List<File> filesToProcess = input.isDirectory() ? getFiles(input, options) : List.of(input);

		if(filesToProcess.isEmpty()) {
			err("No files found!");
			return;
		}

		run(filesToProcess);
	}

	/**
	 * Run the program.
	 *
	 * @param files the files to process.
	 */
	public static void run(List<File> files) {
		log("Processing " + plural(files.size(), "file") + " " + plural(options.passes, "time") + "...");

		LOGGER.info("imag started on ${files.size()} files\n" +
					"\tVersion ${Versioning.getLocalVersion()}\n" +
					"\t${options.passes} passes, ${options.threads} threads\n" +
					"\t" + (options.png ? "PNG processing enabled\n" : "PNG processing disabled\n") +
					"\t" + (options.nbt ? "NBT processing enabled\n" : "NBT processing disabled\n") +
					"\t" + (options.ogg ? "OGG processing enabled\n" : "OGG processing disabled\n") +
					"\t" + (options.archives ? "Archive processing enabled\n" : "Archive processing disabled\n") +
					"\t" + (options.encode ? "Encoding enabled\n" : "Encoding disabled\n") +
					"\t" + (options.quiet ? "Quiet mode enabled\n" : "Quiet mode disabled\n") +
					"\t" + (options.threads == 1 ? "Slow mode enabled\n" : "Slow mode disabled\n") +
					"\t" + (options.force ? "Forced processing enabled\n" : "Forced processing disabled\n"));

		long startTime = System.currentTimeMillis();
		long preSize = size(files);

		asyncs = new CompletableFuture<?>[options.threads];
		if(options.passes == 1) {
			pass(files, -1);
		} else for(int pass = 1; pass < options.passes + 1; pass++) {
			if(pass(files, pass)) break;
		}

		long postSize = size(files);
		long endTime = System.currentTimeMillis();

		long totalSavings = preSize - postSize;
		double timeTaken = (endTime - startTime) / 1e3;
		double percentage = ((double) totalSavings / preSize) * 100;

		String s = "\n\033[1;4m" + "Done!" + "\033[0m\n" + "Took " + timeFromSecs(timeTaken) + "\n" + "Saved " + plural(totalSavings, "byte") + " (" + format(percentage) + "% of " + preSize + ") - up to " + format(maxReduction) + "%\n" + "Max reduction: " + plural(maxReductionSize, "byte");
		log(s);
		LOGGER.info("Exiting...");
	}

	/**
	 * Process a file and run it (or any files inside of it) through the relevant processors.
	 *
	 * @param file the file to process. Guaranteed to not be a directory, to exist, and to end in {@code .png}, {@code .nbt}, or {@code .ogg}.
	 */
	public static void process(File file, boolean reencodeIfImage) {
		if(file == null) return;
		if(file.isDirectory()) {
			err("Directory found! This should not happen!");
			return;
		}

		LOGGER.info("Processing ${file.getName()}");

		long preSize = file.length();
		String name = file.getName().toLowerCase();
		long startTime = System.currentTimeMillis();

		var temp = WORKDIR.toPath().resolve(".imag_temp-" + file.hashCode() + "-" + file.getName()).toFile();

		try {
			Files.copy(file.toPath(), temp.toPath());
		} catch (Exception e) {
			LOGGER.warn("Could not copy file ${file.getName()} to temp file", e);
		}

		Throwable t = switch(name.substring(name.lastIndexOf('.') + 1)) {
			case "png" -> processImage(temp, reencodeIfImage);
			case "nbt" -> processNbt(temp);
			case "ogg" -> processOgg(temp);
			default -> {
				LOGGER.error("Unknown file type: " + name.substring(name.lastIndexOf('.')));
				yield null;
			}
		};

		if(t != null) {
			LOGGER.error("Could not process file ${file.getName()}", t);
		}

		long endTime = System.currentTimeMillis();
		double timeTaken = (endTime - startTime) / 1e3;

		if(temp.length() < file.length()) {
			try {
				Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				LOGGER.warn("Could not move temp file ${temp.getName()} to ${file.getName()}", e);
			}
		}

		long postSize = file.length();
		double reduction = 100.0 - ((double) postSize / (double) preSize) * 100.0;

		maxReduction = Math.max(maxReduction, reduction);
		maxReductionSize = Math.max(maxReductionSize, preSize - postSize);

		LOGGER.info("Processed ${file.getName()} in ${timeFromSecs(timeTaken)}\nSize: ${format(preSize)} -> ${format(postSize)} (${format(reduction)}%)");
		if(reduction < 0.0) {
			LOGGER.warn("File size increased while processing ${file.getName()}!");
		}

		if(options.quiet) return;
		StringBuilder sb = new StringBuilder("\nProcessed ${file.getName()} in ${timeFromSecs(timeTaken)}\n");

		if(reduction > 0.0) {
			sb.append("File size decreased: ").append(format(preSize)).append(" -> ").append(plural(postSize, "byte")).append('\n').append("Savings of ").append(plural(preSize - postSize, "byte")).append(" (").append(format(reduction)).append("%)");
		} else if(reduction < 0.0) {
			sb.append("File size increased! This should not happen!");
		} else {
			sb.append("File size not changed");
		}

		log(sb);
	}

	private static boolean pass(List<File> files, int whatPassAmIOn) {
		boolean doLog = whatPassAmIOn != -1;
		whatPassAmIOn = doLog ? whatPassAmIOn : 1;

		Deque<File> filesToProcess = new ArrayDeque<>(files);

		if(doLog) log("\n\033[1;4mRunning pass ${whatPassAmIOn}...\033[0m");

		long prePassSize = size(files);

		boolean reencodeImage = (whatPassAmIOn == 1) && options.encode;

		//noinspection TextBlockMigration
		LOGGER.info("Pass ${whatPassAmIOn}\n"
					+ "\tOriginal size: ${format(prePassSize)}\n"
					+ "\tReencode images: ${reencodeImage}\n");

		if(options.threads == 1) {
			while(!filesToProcess.isEmpty()) {
				process(filesToProcess.poll(), reencodeImage);
			}
		} else {
			for(int i = 0; i < asyncs.length; i++) {
				int fi = i;
				asyncs[i] = CompletableFuture.runAsync(() -> {
					while(true) {
						File file;
						synchronized(filesToProcess) {
							file = filesToProcess.poll();
						}
						if(file == null) break;
						Thread.currentThread().setName("imag-worker-" + fi);
						process(file, reencodeImage);
					}
				});
			}

			try {
				CompletableFuture.allOf(asyncs).join();
			} catch (CompletionException e) {
				err("Processing failed! Check log for more info.");
				LOGGER.fatal("Processing failed!", e);
			}
		}

		System.gc();

		long currentSavings = prePassSize - size(files);

		if(doLog) log("\nPass ${whatPassAmIOn} complete!\nSaved " + plural(currentSavings, "byte") + "!");

		if(currentSavings == 0 && !options.force) {
			log("Savings are 0, stopping early");
			return true;
		}

		return false;
	}

	private static void addArg(String longName, String shortName, BiFunction<Options, String, Boolean> action) {
		args.put(Pair.of(longName, shortName), action);
	}

	private static void addArg(String longName, String shortName, Consumer<Options> action) {
		addArg(longName, shortName, (options, value) -> {
			action.accept(options);
			return false;
		});
	}

	private static void initArgs() {
		addArg("--passes", "-p", (options, value) -> {
			try {
				int passes = Integer.parseInt(value);
				if(passes < 1) {
					err("Passes must be greater than 0!");
					return true;
				}
				options.passes = passes;
			} catch (NumberFormatException e) {
				err("Invalid number of passes: ${value}");
				return true;
			}
			return false;
		});

		addArg("--disable", null, (options, value) -> {
			String[] values = value.split(",");
			for(String v : values) {
				switch(v) {
					case "png" -> options.png = false;
					case "nbt" -> options.nbt = false;
					case "ogg" -> options.ogg = false;
					default -> {
						err("Unknown filetype: ${v}");
						return true;
					}
				}
			}
			return false;
		});

		addArg("--max-threads", "-t", (options, value) -> {
			try {
				int threads = Integer.parseInt(value);
				if(threads < 1) {
					err("Threads must be greater than 0!");
					return true;
				}
				options.threads = threads;
			} catch (NumberFormatException e) {
				err("Invalid number of threads: ${value}");
				return true;
			}
			return false;
		});

		addArg("--slow", "-s", o -> o.passes = 1);
		addArg("--quiet", "-q", o -> o.quiet = true);
		addArg("--no-encode", "-n", o -> o.encode = false);
		addArg("--force", "-f", o -> o.force = true);
		addArg("--no-log", "-l", o -> o.log = false);
	}

	private static boolean parseArg(Options opts, String name, String value) {
		for(Pair<String, String> pair : args.keySet()) {
			if(name.equals(pair.first()) || name.equals(pair.second())) {
				return args.get(pair).apply(opts, value);
			}
		}
		err("Unknown argument: ${name}");
		return true;
	}

	private static void preMainSetup() {
		System.setProperty("apple.awt.UIElement", "true"); // Don't show the dock icon on macOS
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log("\033[?25h");
			echo(true);
			LOGGER.close();
		}));
		echo(false);
		log("\033[?25l");
		initArgs();
	}
}
