package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.DefaultFileProcessor;
import dev.rdh.imag.util.Binary;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import static dev.rdh.imag.util.StringUtils.err;

@SuppressWarnings({ "DuplicatedCode", "DataFlowIssue" })
public class PngOutProcessor extends DefaultFileProcessor {

	private PngOutProcessor() {
		super(true, Binary.PNGOUT, "-q -y -r -k1 -s0");
	}

	public static PngOutProcessor newInstance() {
		return new PngOutProcessor();
	}

	@Override
	public String name() {
		return "PNGOUT";
	}

	@Override
	public String fileType() {
		return "png";
	}

	@Override
	public void process(File file) throws Exception {
		final int[] blockSizes = { 0, 128, 192, 256, 512, 1024, 2048, 4096, 8192 };

		CompletableFuture<?>[] asyncs = new CompletableFuture<?>[blockSizes.length];

		File outputDir = tempDir(String.valueOf(file.hashCode()));

		for(int i = 0; i < blockSizes.length; i++) {
			ArrayList<String> command = new ArrayList<>(this.command);
			command.add(0, binary.path());
			command.add(1, "-b" + blockSizes[i]);
			command.add(file.getCanonicalPath());

			ProcessBuilder builder = new ProcessBuilder(command)
					.directory(outputDir)
					.redirectError(ProcessBuilder.Redirect.DISCARD)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD);

			asyncs[i] = builder.start().onExit();
		}

		CompletableFuture.allOf(asyncs).join();

		if(outputDir.listFiles() == null) {
			err("No output files found!");
			return;
		}

		File bestResult = Arrays.stream(outputDir.listFiles()).min(Comparator.comparingLong(File::length)).orElse(file);

		Files.copy(bestResult.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
}
