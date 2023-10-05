package dev.rdh.imag.util;

import io.nayuki.png.PngImage;
import io.nayuki.png.chunk.Actl;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class PngUtils {
	private PngUtils() { }

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
		PngImage image;

		try {
			image = PngImage.read(f);
		} catch (IllegalArgumentException | IOException e) {
			return false;
		}

		Optional<Actl> actl = PngImage.getChunk(Actl.class, image.afterIdats, image.afterIhdr);
		return actl.isPresent();
	}
}
