package dev.rdh.imag.util;

import dev.rdh.imag.Main;
import dev.rdh.imag.processors.DefaultFileProcessor;
import dev.rdh.imag.processors.FileProcessor;
import dev.rdh.imag.processors.NbtFileProcessor;
import dev.rdh.imag.processors.OptiVorbisProcessor;
import dev.rdh.imag.processors.OxiPngProcessor;
import dev.rdh.imag.processors.PngFixProcessor;
import dev.rdh.imag.processors.PngOutProcessor;
import dev.rdh.imag.processors.Reencoder;
import dev.rdh.imag.processors.ZopfliPngProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Processing {
	private Processing() { }

	/**
	 * Run an PNG file through all of the PNG processors.
	 *
	 * @param file     the image to process. Guaranteed to be a {@code .png} file.
	 * @param reencode whether or not to reencode the image.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processImage(File file, boolean reencode) {
		List<FileProcessor> processors = new ArrayList<>(Arrays.asList(
				OxiPngProcessor.get1(),
				OxiPngProcessor.get2(),
				ZopfliPngProcessor.get(),
				PngOutProcessor.get(),
				PngFixProcessor.get()
		));

		if(reencode) processors.add(0, Reencoder.get());

		for(var p : processors) {
			try {
				long pre = file.length();
				p.process(file);
				long post = file.length();
				Main.LOGGER.info("${p.name()} (${file.getName()}): ${post-pre}");
			} catch (Exception e) {
				return e;
			}
		}

		return null;
	}

	/**
	 * Run an NBT file through the NBT processor.
	 *
	 * @param file the NBT file to process. Guaranteed to be a {@code .nbt} file.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processNbt(File file) {
		try {
			NbtFileProcessor.get().process(file);
		} catch (Exception e) {
			return e;
		}
		return null;
	}

	/**
	 * Run an OGG file through the OGG processor.
	 *
	 * @param file the audio file to process. Guaranteed to be a {@code .ogg} file.
	 * @return an exception if one occurred, otherwise {@code null}.
	 */
	public static Throwable processOgg(File file) {
		try {
			OptiVorbisProcessor.get().process(file);
		} catch (Exception e) {
			return e;
		}
		return null;
	}
}
