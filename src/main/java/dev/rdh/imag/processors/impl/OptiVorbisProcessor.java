package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.DefaultFileProcessor;
import dev.rdh.imag.util.Binary;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class OptiVorbisProcessor extends DefaultFileProcessor {

	public OptiVorbisProcessor() {
		super(false, Binary.OPTIVORBIS, "-q -r ogg2ogg");
	}

	public static OptiVorbisProcessor get() {
		return new OptiVorbisProcessor();
	}

	@Override
	public String name() {
		return "OptiVorbis";
	}

	@Override
	public String fileType() {
		return "ogg";
	}

	@Override
	public void process(File file) throws Exception {
		File tempFile = tempFile(file.hashCode() + "-original");
		Files.copy(file.toPath(), tempFile.toPath());

		super.process(file);

		if(file.length() == 0) {
			// Something went wrong, restore the original file
			Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
