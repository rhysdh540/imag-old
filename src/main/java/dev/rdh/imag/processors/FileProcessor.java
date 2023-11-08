package dev.rdh.imag.processors;

import java.io.File;

public interface FileProcessor {
	void process(File file) throws Exception;

	String name();

	String extension();
}
