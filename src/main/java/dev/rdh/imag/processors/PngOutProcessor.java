package dev.rdh.imag.processors;

import java.io.File;

@SuppressWarnings("DuplicatedCode")
public class PngOutProcessor extends AbstractFileProcessor {

	private PngOutProcessor() {
		super("png", true, "pngout -q -y -r -k1");
	}

	public static PngOutProcessor newInstance() {
		return new PngOutProcessor();
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
