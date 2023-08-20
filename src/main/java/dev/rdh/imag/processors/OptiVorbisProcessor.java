package dev.rdh.imag.processors;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class OptiVorbisProcessor extends AbstractFileProcessor {

	public OptiVorbisProcessor() {
		super("ogg", false, "optivorbis -q -r ogg2ogg");
	}

	public static OptiVorbisProcessor newInstance() {
		return new OptiVorbisProcessor();
	}

	@Override
	public void process(File file) throws Exception {
		var tempFile = File.createTempFile(String.valueOf(file.hashCode()), fileType);
		Files.copy(file.toPath(), tempFile.toPath());

		super.process(file);

		if(file.length() == 0) {
			// Something went wrong, restore the original file
			Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
