package dev.rdh.imag.processors;

import java.io.File;

public interface FileProcessor {
	/**
	 * Process the given file
	 * @param file The file to process
	 * @throws Exception If an error occurs
	 */
	void process(File file) throws Exception;

	/**
	 * The name of the processor
	 * @return The name of the processor
	 */
	String name();

	/**
	 * The extension of the files that this processor can process
	 * @return The extension of the files that this processor can process
	 */
	String extension();
}
