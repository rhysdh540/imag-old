package dev.rdh.imag.processors.image;

import dev.rdh.imag.processors.AbstractFileProcessor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PngFixProcessor extends AbstractFileProcessor {
	public static final PngFixProcessor INSTANCE = new PngFixProcessor();

	private PngFixProcessor() {
		super("png", true, "pngfix -o --strip=all -q");
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;
		command.add(file.getCanonicalPath());

		File outputDir = new File(".workdir").getCanonicalFile();
		outputDir.mkdir();

		command.add("--out=output." + fileType);

		ProcessBuilder pb = new ProcessBuilder(command)
				.directory(outputDir)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.redirectOutput(ProcessBuilder.Redirect.DISCARD);

		Process proc = pb.start();
		proc.waitFor();

		File output = new File(outputDir, "output." + fileType);
		if(output.exists() && output.length() < file.length()) {
			file.delete();
			Path path = file.toPath();
			Files.move(output.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
		}

		outputDir.delete();
	}
}
