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
	 * @param reencode whether or not to reencode the image.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processImage(File file, boolean reencode) {
		var processors = new ArrayList<>(Arrays.asList(
				OxiPngProcessor.get1(),
				OxiPngProcessor.get2(),
				ZopfliPngProcessor.get(),
				PngOutProcessor.get(),
				PngFixProcessor.get()
		));

		if(reencode)
			processors.add(0, ImageIOProcessor.get());

		for(var p : processors) {
			try {
				p.process(file);
			} catch(Exception e) {
				return e;
			}
		}

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
