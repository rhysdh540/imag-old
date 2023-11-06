package dev.rdh.imag.processors.impl.archives;

import dev.rdh.imag.processors.DefaultFileProcessor;
import java.io.File;

public class ZipProcessor extends DefaultFileProcessor {

	protected ZipProcessor() {
		super(false, null, "");
	}

	public static ZipProcessor newInstance() {
		return new ZipProcessor();
	}

	@Override
	public String name() {
		return "CustomZip";
	}

	@Override
	public String fileType() {
		return "zip";
	}

	@Override
	public void process(File file) throws Exception {

	}
}
