package dev.rdh.imag.util;

import io.nayuki.png.PngImage;
import io.nayuki.png.chunk.Actl;
import java.io.File;
import java.io.IOException;

public class PngUtils {
	private PngUtils() {}

	public static boolean isPNG(File f) {
		if(f == null || !f.exists()) return false;

		try {
			PngImage.read(f);
			return true;
		} catch (IllegalArgumentException | IOException e) {
			return false;
		}
	}

	public static boolean isAnimated(File f) {
		try {
			PngImage image = PngImage.read(f);
			return PngImage.getChunk(Actl.class, image.afterIdats, image.afterIhdr).isPresent();
		} catch (IllegalArgumentException | IOException e) {
			return false;
		}
	}
}
