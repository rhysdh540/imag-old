package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.DefaultFileProcessor;
import dev.rdh.imag.util.Binary;
import java.io.File;

public class OxiPngProcessor extends DefaultFileProcessor {

	private OxiPngProcessor(String... command) {
		super(false, Binary.OXIPNG, "-o max -q", String.join(" ", command));
	}

	public static OxiPngProcessor new1Instance() {
		return new OxiPngProcessor();
	}

	public static OxiPngProcessor new2Instance() {
		return new OxiPngProcessor("-a");
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
	protected void addFilesToArgList(File file, String output) throws Exception {
		command.add(0, binary.path());
		command.add("--out=" + output);
		command.add(file.getCanonicalPath());
	}
}
