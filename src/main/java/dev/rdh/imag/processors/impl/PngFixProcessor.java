package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.BinaryFileProcessor;
import dev.rdh.imag.util.Binary;
import java.io.File;

public class PngFixProcessor extends BinaryFileProcessor {

	private PngFixProcessor() {
		super(true, Binary.PNGFIX, "-o --strip=all -q");
	}

	public static PngFixProcessor newInstance() {
		return new PngFixProcessor();
	}

	@Override
	public String name() {
		return "pngfix";
	}

	@Override
	public String extension() {
		return "png";
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		command.add(0, binary.path());
		command.add(file.getCanonicalPath());
		command.add("--out=" + output);
	}
}
