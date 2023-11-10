package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.FileProcessor;
import java.io.File;
import java.nio.file.Files;

public class OxiPngProcessor implements FileProcessor {

	private OxiPngProcessor() {}

	public static OxiPngProcessor newInstance() {
		return new OxiPngProcessor();
	}

	@Override
	public String name() {
		return "Oxipng";
	}

	@Override
	public String extension() {
		return "png";
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(extension())) return;

		byte[] data = Files.readAllBytes(file.toPath());
		byte[] compressed = compress(data, false);
		if(compressed.length < data.length) data = compressed;

		byte[] compressedAlpha = compress(data, true);
		if(compressedAlpha.length < data.length) data = compressedAlpha;

		if(data.length < file.length()) Files.write(file.toPath(), data);
	}

	private static native byte[] compress(byte[] data, boolean alpha);
}
