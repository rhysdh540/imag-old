package dev.rdh.imag.util;

import dev.rdh.imag.processors.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

public class Processing {
	/**
	 * Run an PNG file through all of the PNG processors.
	 * @param file the image to process. Guaranteed to be a {@code .png} file.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processImage(File file, boolean first) {
		var processors = new ArrayList<>(Arrays.asList(
				ZopfliPngProcessor.get(),
				OxiPngProcessor.get1(),
				OxiPngProcessor.get2(),
				PngOutProcessor.get(),
				PngFixProcessor.get()
		));

		if(first)
			processors.add(0, ImageIOProcessor.get());

		//create a temporary file with the contents of the original file
		var temp = file.getParentFile().toPath().resolve("imag-" + file.getName()).toFile();
		try {
			Files.copy(file.toPath(), temp.toPath());
		} catch(Exception e) {
			return e;
		}

		for(var p : processors) {
			try {
				p.process(temp);
			} catch(Exception e) {
				return e;
			}
		}

		if(temp.length() < file.length()) {
			try {
				Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch(Exception e) {
				return e;
			}
		}

		temp.delete();
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

	private Processing(){}
}
