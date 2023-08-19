package dev.rdh.imag.processors.image;

import dev.rdh.imag.processors.AbstractFileProcessor;

import java.io.File;

public class PngFixProcessor extends AbstractFileProcessor {

	private PngFixProcessor() {
		super("png", true, "pngfix -o --strip=all -q");
	}

	public static PngFixProcessor newInstance() {
		return new PngFixProcessor();
	}

	@Override
	protected void addFilesToArgList(File file) throws Exception {
		command.add(file.getCanonicalPath());
		command.add("--out=output." + fileType);
	}
}
