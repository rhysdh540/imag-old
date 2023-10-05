package dev.rdh.imag.processors;

import dev.rdh.imag.util.PngUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageIOProcessor extends AbstractFileProcessor {
	protected ImageIOProcessor() {
		super("png", false, null, "");
		this.command.clear(); // we're not using this
	}

	public static ImageIOProcessor get() {
		return new ImageIOProcessor();
	}

	@Override
	public String name() {
		return "ImageIO";
	}

	@Override
	public void process(File file) throws Exception {
		if(!PngUtils.isPNG(file) || PngUtils.isAnimated(file)) return;

		BufferedImage originalImage = ImageIO.read(file);

		if (originalImage == null) {
			throw new IOException("Failed to read image");
		}

		BufferedImage reencodedImage = new BufferedImage(
				originalImage.getWidth(), originalImage.getHeight(),
				BufferedImage.TYPE_INT_ARGB); // using originalImage.getType() is very buggy and breaks a lot, argb is a safe bet

		reencodedImage.createGraphics().drawImage(originalImage, 0, 0, null);

		ImageIO.write(reencodedImage, "png", file);
	}
}
