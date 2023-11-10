package dev.rdh.imag;

import dev.rdh.imag.processors.FileProcessor;
import dev.rdh.imag.processors.impl.*;
import dev.rdh.imag.util.EpicLogger;
import dev.rdh.imag.util.FileUtils;
import dev.rdh.imag.util.StringUtils.Pair;
import dev.rdh.imag.util.Versioning;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.rdh.imag.util.StringUtils.*;

public class Main {
	public static int passes = 1;
	public static int threads = 8;

	static final Map<Pair<String, String>, Function<String, Boolean>> args = new HashMap<>();

	public static final File WORKDIR = FileUtils.makeWorkDir();
	public static final File MAINDIR = new File(System.getProperty("user.home"), ".imag");

	public static final EpicLogger LOGGER = new EpicLogger("imag")
			#if DEV
			.file(new File(MAINDIR, "logs/debug.log"))
			#else
			.file(new File(MAINDIR, "logs/latest.log"))
			#endif
			.disableDebug()
			.disableTrace()
			.enableInfo();

	// statistics
	static double maxReduction = 0.0;
	static long maxReductionSize = 0;

	static {
		MAINDIR.mkdirs();

		#if !DEV
		//unpack
		#endif

		System.loadLibrary("imag");
	}

	/**
	 * The list of processors to run.
	 */
	public static final List<Supplier<FileProcessor>> processors = new ArrayList<>(List.of(
			Reencoder::newInstance,
			OxiPngProcessor::newInstance,
			ZopfliPngProcessor::newInstance,
			PngOutProcessor::newInstance,
			PngFixProcessor::newInstance,
			NbtFileProcessor::newInstance,
			OptiVorbisProcessor::newInstance
	));

	/**
	 * The main method. Parses arguments then calls {@link #run(Queue)} a number of times.
	 * @param args the arguments to parse.
	 */
	#if DEV @SuppressWarnings("all") #endif
	public static void main(String[] args) {
		#if DEV
		String a = "/Users/rhys/Downloads/conductor_whistle.ogg";
		args = new String[]{ a };
		#endif
		initArgs();
		if(args.length < 1) {
			err("No input specified! Use --help or -h for usage.");
			return;
		}

		switch(args[0]) {
			case "-h", "--help" -> {
				log("""
					Usage: imag [options] <path>
									
					Options:
					-p, --passes=<number>    Number of passes to run (default: 1)
					-t, --threads=<number>   Number of images to process concurrently (default: 8)
					--disable=<processor>    Disable a processor
					-h, --help               Display this help message
					-v, --version            Display the version of imag
					-u, --update             Update imag to the latest version
					""");
				return;
			}
			case "-v", "--version" -> {
				log("imag v" + Versioning.getLocalVersion());
				return;
			}
			case "-u", "--update" -> {
				Versioning.downloadNewVersionIfNecessary();
				return;
			}
		}

		File path = new File(args[args.length - 1]);
		args = Arrays.copyOf(args, args.length - 1);

		for(String arg : args) {
			if(parseArg(arg)) return;
		}

		List<File> files;
		if(path.isFile()) {
			files = List.of(path);
		} else {
			files = FileUtils.getFiles(path, true, true, true, false); //todo
			files.sort(Comparator.comparing(File::getName));
		}

		LOGGER.info("imag v" + Versioning.getLocalVersion());
		LOGGER.info("Found " + plural(files.size(), "file"));
		LOGGER.info("Initial size: " + plural(FileUtils.size(files), "byte"));
		LOGGER.info(plural(passes, "pass") + ", " + plural(threads, "thread"));

		long preSize = FileUtils.size(files);
		long startTime = System.currentTimeMillis();

		for(int i = 0; i < passes; i++) {
			log("\033[1;4mStarting pass " + (i + 1) + "/" + passes + "\033[0m");
			long passPre = FileUtils.size(files);
			run(new ArrayDeque<>(files));
			if(i == 0) {
				processors.removeIf(Reencoder.class::isInstance);
			}

			long passPost = FileUtils.size(files);
			String info = "Pass " + (i + 1) + "/" + passes + ": saved " + plural(passPre - passPost, "byte") + " (" + format((passPre - passPost) / (double) passPre * 100) + "%)";
			log(info);
			LOGGER.info(info);
		}

		long endTime = System.currentTimeMillis();
		long postSize = FileUtils.size(files);

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
	 * The thread group for all the worker threads.
	 */
	static final ThreadGroup group = new ThreadGroup("imag workers");

	/**
	 * Run the processors on the files in the queue.
	 * @param queue the queue of files to process.
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void run(Queue<File> queue) {
		for(int i = 0; i < threads; i++) {
			final int index = i;
			new Thread(group, () -> {
				File tempDir = new File(WORKDIR, "worker-" + index);
				tempDir.mkdirs();

				while(true) {
					// get the file
					File file;
					synchronized(queue) {
						file = queue.poll();
					}
					if(file == null) break;

					// get starting file info
					String name = file.getName();
					long pre = file.length();
					long start = System.currentTimeMillis();

					// copy the file to the temp directory
					File tempFile = new File(tempDir, name);

					try {
						Files.copy(file.toPath(), tempFile.toPath());
					} catch(Exception e) {
						err("Failed to copy file: " + file.getAbsolutePath(), e);
						tempFile.delete();
						continue;
					}

					// process the file
					for(Supplier<FileProcessor> s : processors) {
						FileProcessor p = s.get();
						if(!name.endsWith(p.extension())) continue;
						try {
							long processorPre = tempFile.length();
							p.process(tempFile);
							long processorPost = tempFile.length();
							LOGGER.info(p.name() + " on '" + tempFile.getName() + "': " + processorPre + " -> " + processorPost + " [" + sign(processorPost - processorPre) + "] (total: " + sign(processorPost - pre) + ")");
						} catch(Exception e) {
							err("Failed to process file: " + tempFile.getAbsolutePath(), e);
						}
					}

					// copy the temp file back to the original file if it's smaller
					if(tempFile.length() < file.length()) {
						try {
							Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
						} catch(Exception e) {
							err("Failed to copy file: " + tempFile.getAbsolutePath(), e);
						}
					}
					tempFile.delete();

					// get ending file info
					long end = System.currentTimeMillis();
					long post = file.length();
					double reduction = 100.0 - ((double) post / (double) pre) * 100.0;

					maxReduction = Math.max(maxReduction, reduction);
					maxReductionSize = Math.max(maxReductionSize, pre - post);

					double timeTaken = (end - start) / 1000.0;

					LOGGER.info("Processed " + file.getName() + " in " + timeFromSecs(timeTaken) +
								"\nSize: " + format(pre) + " -> " + format(post) + " (" + format(reduction) + "%)");
					if(reduction < 0.0) {
						LOGGER.warn("File size increased while processing " + file.getName() + "!");
					}

					String message = "\nProcessed " + name + " in " + timeFromSecs(timeTaken) + "\n";

					if(reduction > 0.0) {
						message += "File size decreased: " + format(pre) + " -> " + plural(post, "byte") + '\n'
								   + "Savings of " + plural(pre - post, "byte") + " (" + format(reduction) + "%)";
					} else if(reduction < 0.0) {
						message += "File size increased! This should not happen!";
					} else {
						message += "File size not changed";
					}

					log(message);
				}
			}, "imag worker #${index}")
					.start();
		}

		int i = 0;
		//wait for all the threads to finish
		while(group.activeCount() > 0) {
			try {
				//noinspection BusyWait
				Thread.sleep(100);
				i++;
			} catch(InterruptedException e) {
				err("Main thread interrupted while waiting for children to finish", e);
			}
			if(i == 600) {
				// gc every minute
				System.gc();
				i = 0;
			}
		}
	}

	public static void initArgs() {
		args.put(Pair.of("-p", "--passes"), arg -> {
			try {
				passes = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
			} catch(NumberFormatException e) {
				err("Invalid number of passes: " + arg);
				return true;
			}
			return false;
		});
		args.put(Pair.of("-t", "--threads"), arg -> {
			try {
				threads = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
			} catch(NumberFormatException e) {
				err("Invalid number of threads: " + arg);
				return true;
			}
			return false;
		});
		args.put(Pair.of("--disable", "--disable"), arg -> {
			String[] names = arg.substring(arg.indexOf("=") + 1).split(",");
			outer: for(String name : names) {
				for(Supplier<FileProcessor> s : processors) {
					FileProcessor p = s.get();
					if(p.name().equalsIgnoreCase(name)) {
						processors.remove(s);
						continue outer;
					}
				}
				err("Invalid processor: " + name);
				return true;
			}
			return false;
		});
	}

	public static boolean parseArg(String arg) {
		for(Pair<String, String> key : args.keySet()) {
			if(arg.startsWith(key.first()) || arg.startsWith(key.second())) {
				return args.get(key).apply(arg);
			}
		}
		err("Invalid argument: " + arg);
		return true;
	}
}