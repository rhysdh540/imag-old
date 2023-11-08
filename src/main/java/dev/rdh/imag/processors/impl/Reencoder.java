package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.FileProcessor;
import dev.rdh.imag.util.PngUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.zip.Deflater;

import com.pngencoder.PngEncoder;

import javax.imageio.ImageIO;

public class Reencoder implements FileProcessor {

	private Reencoder() {}

	public static Reencoder newInstance() {
		return new Reencoder();
	}

	@Override
	public String name() {
		return "Reencoder";
	}

	@Override
	public String extension() {
		return "png";
	}

	@Override
	public void process(File file) throws Exception {
		if(!PngUtils.isPNG(file) || PngUtils.isAnimated(file)) return;

		BufferedImage bi = ImageIO.read(file);

		if(bi == null) {
			throw new IOException("Failed to read image");
		}

		new PngEncoder()
				.withBufferedImage(bi)
				//.withCompressionLevel(Deflater.NO_COMPRESSION)
				.withMultiThreadedCompressionEnabled(false)
				.toFile(file);
	}
}
