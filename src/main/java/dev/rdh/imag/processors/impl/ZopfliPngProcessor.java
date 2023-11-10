package dev.rdh.imag.processors.impl;

import dev.rdh.imag.Main;
import dev.rdh.imag.processors.BinaryFileProcessor;
import dev.rdh.imag.util.Binary;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings({ "DataFlowIssue" })
public class ZopfliPngProcessor extends BinaryFileProcessor {

	static final char[] filters = { '0', '1', '2', '3', '4', 'm', 'e', 'p', 'b' };

	private ZopfliPngProcessor() {
		super(false, Binary.ZOPFLIPNG, "--iterations=15 --keepchunks=acTL,fdAT,fcTL -y"); // keepchunks so apng works
	}

	public static ZopfliPngProcessor newInstance() {
		return new ZopfliPngProcessor();
	}

	@Override
	public String name() {
		return "ZopfliPNG";
	}

	@Override
	public String extension() {
		return "png";
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(extension())) return;

		String binaryPath = binary.path();

		if(binaryPath == null) { // If the binary is not found, skip processing
			return;
		}

		this.command.add(0, binaryPath);
		CompletableFuture<?>[] asyncs = new CompletableFuture<?>[filters.length];
		File outputDir = tempDir(String.valueOf(file.hashCode()));

		for(int i = 0; i < filters.length; i++) {
			ArrayList<String> command = new ArrayList<>(this.command);
			command.add(1, "--filters=" + filters[i]);
			command.add(file.getCanonicalPath());
			command.add(filters[i] + ".png");

			ProcessBuilder builder = new ProcessBuilder(command)
					.directory(outputDir)
					.redirectError(ProcessBuilder.Redirect.DISCARD)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD);

			asyncs[i] = builder.start().onExit();
		}

		CompletableFuture.allOf(asyncs).join();

		if(outputDir.listFiles() == null) {
			Main.LOGGER.error("No output files found for file " + file.getName() + "!");
			return;
		}

		File bestResult = Arrays.stream(outputDir.listFiles())
								.min(Comparator.comparingLong(File::length))
								.orElse(file);
		if(bestResult.length() < file.length()) {
			Files.copy(bestResult.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
