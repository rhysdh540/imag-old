package dev.rdh.imag.processors;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractFileProcessor {

	protected final List<String> command;
	protected final String fileType;
	final boolean front;

	public AbstractFileProcessor(String fileType, boolean front, String command) {
		this.fileType = fileType;
		this.front = front;

		String[] strings = command.split(" ");
		List<String> stringsButItsAListNow = Arrays.asList(strings);

		this.command = new ArrayList<>(stringsButItsAListNow);
	}

	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;
		if(front)
			command.add(1, file.getCanonicalPath());
		else
			command.add(file.getCanonicalPath());

		File outputDir = new File(".workdir").getCanonicalFile();
		outputDir.mkdir();

		command.add("output." + fileType);

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
