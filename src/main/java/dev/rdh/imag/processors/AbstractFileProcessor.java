package dev.rdh.imag.processors;

import dev.rdh.imag.Binary;
import dev.rdh.imag.Main;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"ResultOfMethodCallIgnored", "DuplicatedCode"})
public abstract class AbstractFileProcessor {

	protected final List<String> command;
	protected final String fileType;
	protected final Binary binary;
	final boolean front;

	protected AbstractFileProcessor(String fileType, boolean front, Binary binary, String... command) {
		this.fileType = fileType;
		this.front = front;
		this.binary = binary;

		var strings = String.join(" ", command).split(" ");

		this.command = new ArrayList<>(Arrays.asList(strings));
	}

	protected void addFilesToArgList(File file, String output) throws Exception {
		this.command.add(0, binary.toString());
		if(front)
			command.add(1, file.getCanonicalPath());
		else
			command.add(file.getCanonicalPath());

		command.add(output);
	}

	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;

		if(binary.toString() == null) { // If the binary is not found, skip processing
			return;
		}

		var name = String.valueOf(file.hashCode());

		var output = tempFile(name);

		addFilesToArgList(file, output.getName());

		var pb = new ProcessBuilder(command)
				.directory(output.getParentFile())
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.redirectOutput(ProcessBuilder.Redirect.DISCARD);

		pb.start().waitFor();

		if(output.exists() && output.length() < file.length()) {
			Files.move(output.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	File tempFile(String name) throws Exception {
		File result = File.createTempFile(name, '.' + fileType, Main.workdir);
		result.deleteOnExit();
		result.delete();
		return result;
	}

	File tempDir(String name) throws Exception {
		File result = Files.createTempDirectory(Main.workdir.toPath(), name).toFile();
		result.deleteOnExit();
		return result;
	}
}
