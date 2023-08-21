package dev.rdh.imag.processors;

import java.io.File;

public class PngFixProcessor extends AbstractFileProcessor {

	private PngFixProcessor() {
		super("png", true, "pngfix -o --strip=all -q");
	}

	public static PngFixProcessor newInstance() {
		return new PngFixProcessor();
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		command.add(file.getCanonicalPath());
		command.add("--out=" + output + "." + fileType);
	}
}
