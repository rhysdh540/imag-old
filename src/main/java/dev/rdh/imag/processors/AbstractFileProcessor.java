package dev.rdh.imag.processors;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"ResultOfMethodCallIgnored", "DuplicatedCode"})
public abstract class AbstractFileProcessor {

	protected final List<String> command;
	protected final String fileType;
	final boolean front;

	public AbstractFileProcessor(String fileType, boolean front, String command) {
		this.fileType = fileType;
		this.front = front;

		var strings = command.split(" ");

		this.command = new ArrayList<>(Arrays.asList(strings));
	}

	protected void addFilesToArgList(File file) throws Exception {
		if(front)
			command.add(1, file.getCanonicalPath());
		else
			command.add(file.getCanonicalPath());

		command.add("output." + fileType);
	}

	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;

		var outputDir = new File(".workdir" + File.separator + file.hashCode()).getCanonicalFile();
		outputDir.mkdirs();

		addFilesToArgList(file);

		var pb = new ProcessBuilder(command)
				.directory(outputDir)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.redirectOutput(ProcessBuilder.Redirect.DISCARD);

		pb.start().waitFor();

		var output = new File(outputDir, "output." + fileType);
		if(output.exists() && output.length() < file.length()) {
			file.delete();
			Path path = file.toPath();
			Files.move(output.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
		}

		//noinspection DataFlowIssue
		for(File f : outputDir.listFiles()) {
			f.delete();
		}

		outputDir.delete();
	}
}
