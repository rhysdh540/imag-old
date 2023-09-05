package dev.rdh.imag.processors;

import dev.rdh.imag.Binary;

import java.io.File;

public class PngFixProcessor extends AbstractFileProcessor {

	private PngFixProcessor() {
		super("png", true, Binary.PNGFIX,"-o --strip=all -q");
	}

	public static PngFixProcessor get() {
		return new PngFixProcessor();
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		command.add(0, binary.toString());
		command.add(file.getCanonicalPath());
		command.add("--out=" + output + "." + fileType);
	}
}
