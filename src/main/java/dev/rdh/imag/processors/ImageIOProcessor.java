package dev.rdh.imag.processors;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageIOProcessor extends AbstractFileProcessor {
	protected ImageIOProcessor() {
		super("png", false, null, null, null);
		this.command.clear(); // we're not using this
	}

	public static ImageIOProcessor get() {
		return new ImageIOProcessor();
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;

		// Read the original PNG image
		BufferedImage originalImage = ImageIO.read(file);

		if (originalImage == null) {
			throw new IOException("Failed to read image");
		}

		BufferedImage reencodedImage = new BufferedImage(
				originalImage.getWidth(), originalImage.getHeight(),
				BufferedImage.TYPE_INT_ARGB);

		reencodedImage.createGraphics().drawImage(originalImage, 0, 0, null);

		ImageIO.write(reencodedImage, "png", file);
	}
}
