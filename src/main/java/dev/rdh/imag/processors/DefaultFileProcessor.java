package dev.rdh.imag.processors;

import dev.rdh.imag.util.Binary;
import dev.rdh.imag.Main;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({ "ResultOfMethodCallIgnored", "DuplicatedCode" })
public abstract class DefaultFileProcessor implements FileProcessor {

	protected final List<String> command;
	protected final Binary binary;
	final boolean front;

	protected DefaultFileProcessor(boolean front, Binary binary, String... command) {
		this.front = front;
		this.binary = binary;

		String[] strings = String.join(" ", command).split(" ");

		this.command = new ArrayList<>(Arrays.asList(strings));
	}

	protected void addFilesToArgList(File file, String output) throws Exception {
		this.command.add(0, binary.path());
		if(front) command.add(1, file.getCanonicalPath());
		else command.add(file.getCanonicalPath());

		command.add(output);
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(extension())) return;

		if(binary.path() == null) { // If the binary is not found, skip processing
			return;
		}

		String name = String.valueOf(file.hashCode());

		File output = tempFile(name);

		addFilesToArgList(file, output.getName());

		ProcessBuilder pb = new ProcessBuilder(command)
				.directory(output.getParentFile())
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.redirectOutput(ProcessBuilder.Redirect.DISCARD);

		pb.start().waitFor();

		if(output.exists() && output.length() < file.length()) {
			Files.move(output.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	protected final File tempFile(String name) throws Exception {
		File result = File.createTempFile(name, '.' + extension(), Main.WORKDIR);
		result.deleteOnExit();
		result.delete();
		return result;
	}

	protected final File tempDir(String name) throws Exception {
		File result = Files.createTempDirectory(Main.WORKDIR.toPath(), name).toFile();
		result.deleteOnExit();
		return result;
	}
}
