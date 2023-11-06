package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.FileProcessor;
import dev.rdh.imag.util.PngUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import com.pngencoder.PngEncoder;

import javax.imageio.ImageIO;

public class Reencoder implements FileProcessor {

	public static Reencoder newInstance() {
		return new Reencoder();
	}

	@Override
	public String name() {
		return "Reencoder";
	}

	@Override
	public void process(File file) throws Exception {
		if(!PngUtils.isPNG(file) || PngUtils.isAnimated(file)) return;

		BufferedImage originalImage = ImageIO.read(file);

		if(originalImage == null) {
			throw new IOException("Failed to read image");
		}

		new PngEncoder()
				.withBufferedImage(originalImage)
				.withCompressionLevel(0)
				.withMultiThreadedCompressionEnabled(false)
				.toFile(file);
	}
}
