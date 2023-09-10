package dev.rdh.imag;

import dev.rdh.imag.processors.*;

import java.io.File;
import java.util.Arrays;

public class Processing {
	/**
	 * Run an PNG file through all of the PNG processors.
	 * @param file the image to process. Guaranteed to be a {@code .png} file.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processImage(File file) {
		var processors = Arrays.asList(
				ZopfliPngProcessor.get(),
				OxiPngProcessor.get1(),
				OxiPngProcessor.get2(),
				PngOutProcessor.get(),
				PngFixProcessor.get()
		);

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
}
