package dev.rdh.imag.processors.image;

import dev.rdh.imag.processors.AbstractFileProcessor;

import java.io.File;

@SuppressWarnings("DuplicatedCode")
public class PngOutProcessor extends AbstractFileProcessor {

	public static final PngOutProcessor INSTANCE = new PngOutProcessor();

	private PngOutProcessor() {
		super("png", true, "pngout -q -y -r -k1");
	}

	@Override
	protected void addFilesToArgList(File file) throws Exception {
		int level = 0;
//		if(file.length() > 10000)
//			level = 1; // use faster setting for large files
		this.command.add("-s" + level);
		super.addFilesToArgList(file);
	}
}
