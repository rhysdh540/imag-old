package dev.rdh.imag.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Processor {

	private final List<String> command;
	private final String fileType;

	Processor(String fileType, String command) {
		this.fileType = fileType;

		String[] strings = command.split(" ");

		this.command = new ArrayList<>(Arrays.asList(command));
	}

	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;
		command.add(file.getCanonicalPath());

		new ProcessBuilder(command).start().waitFor();
	}
}
