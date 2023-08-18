package dev.rdh.imag.processors.image;

import dev.rdh.imag.processors.AbstractFileProcessor;

import java.io.File;

public class PngFixProcessor extends AbstractFileProcessor {
	public static final PngFixProcessor INSTANCE = new PngFixProcessor();

	private PngFixProcessor() {
		super("png", true, "pngfix -o --strip=all -q");
	}

	@Override
	protected void addFilesToArgList(File file) throws Exception {
		command.add(file.getCanonicalPath());
		command.add("--out=output." + fileType);
	}
}
