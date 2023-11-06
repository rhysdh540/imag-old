package dev.rdh.imag.processors.impl.archives;

import dev.rdh.imag.processors.DefaultFileProcessor;
import dev.rdh.imag.util.Binary;
import java.io.File;

public class GZipProcessor extends DefaultFileProcessor {
	protected GZipProcessor() {
		super(false, Binary.ZOPFLI, "--gzip --i1000 -c");
	}

	public static GZipProcessor newInstance() {
		return new GZipProcessor();
	}

	@Override
	public String name() {
		return "CustomGZip";
	}

	@Override
	public String fileType() {
		return "gz";
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType())) return;

		String name = String.valueOf(file.hashCode());

		addFilesToArgList(file, name);

		ProcessBuilder pb = new ProcessBuilder(command)
				.directory(file.getParentFile())
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.redirectOutput(file);

		pb.start().waitFor();
	}
}
