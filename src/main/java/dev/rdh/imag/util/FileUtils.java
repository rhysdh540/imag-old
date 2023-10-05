package dev.rdh.imag.util;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import manifold.util.ReflectUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.rdh.imag.Main.nbt;
import static dev.rdh.imag.Main.ogg;
import static dev.rdh.imag.Main.png;

public class FileUtils {
	private FileUtils(){}

	/**
	 * Get all valid files in a directory.
	 * <p>
	 * Depending on the program's settings, this will only return files ending in {@code .png}, {@code .nbt}, or {@code .ogg}.
	 * @param dir the directory to get files from.
	 * @return a Deque of all valid files in the directory.
	 */
	public static List<File> getFiles(@NotNull File dir) {
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

	/**
	 * Create a temporary directory for the program to work in.
	 * @return the temporary directory.
	 */
	public static File makeWorkDir() {
		try {
			File f = Files.createTempDirectory(".imag-workdir").toFile();
			f.deleteOnExit();
			return f;
		} catch (IOException e) {
			StringUtils.err("Could not create work directory!", e);
			System.exit(1);
		}
		return null;
	}

	/**
	 * Get the total size of a collection of files.
	 * @param files the files to get the size of.
	 * @return the total size of the files, in bytes.
	 */
	public static long size(@NotNull Collection<File> files) {
		long sum = 0L;
		for(File file : files) {
			sum += file.length();
		}
		return sum;
	}

	/**
	 * Get an input stream from a URL.
	 * @param url the URL to get the input stream from.
	 * @return the input stream.
	 */
	public static @Nullable InputStream onlineResource(@NotNull String url) {
		try {
			return new URL(url).openStream();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Get a resource from the classpath.
	 * @param path the path to the resource.
	 * @return the resource as an input stream.
	 */
	public static @Nullable InputStream localResource(String path) {
		return StringUtils.class.getClassLoader().getResourceAsStream(path);
	}

	/**
	 * Fix a path name to be correctly capitalized and have the correct path separator.
	 * @param path the path to fix.
	 * @return the fixed path.
	 */
	public static String sanitize(String path) {
		File file = new File(path.replace('\\', '/')
								 .replace('/', File.separatorChar));

		return recursiveSanitize(file);
	}

	private static String recursiveSanitize(File file) {
		if (file == null)
			return "";

		String currentName = file.getName();
		File parentFile = file.getParentFile();

		if (parentFile == null) {
			return currentName;
		}

		String parentPath = recursiveSanitize(parentFile);

		File[] matchingFiles = parentFile.listFiles((dir, name) -> name.equalsIgnoreCase(currentName));

		if (matchingFiles != null) {
			for (File matchingFile : matchingFiles) {
				if (matchingFile.getName().equalsIgnoreCase(currentName)) {
					return parentPath + File.separator + matchingFile.getName();
				}
			}
		}

		return parentPath + File.separator + currentName;
	}

	public static void echo(boolean on) {
		try {
			if(System.console() == null) return;
			ReflectUtil.method(System.console(), "echo", boolean.class).invoke(on);
		} catch (Throwable e) { // echo throws an ioexception
			if(e instanceof RuntimeException) {
				return; //we are probably on the native image
			}
			throw new IOError(e);
		}
	}
}
