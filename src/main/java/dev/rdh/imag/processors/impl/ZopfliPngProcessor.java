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

@SuppressWarnings({ "DataFlowIssue" })
public class ZopfliPngProcessor extends DefaultFileProcessor {

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
	public String fileType() {
		return "png";
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType())) return;

		String binaryPath = binary.path();

		if(binaryPath == null) { // If the binary is not found, skip processing
			return;
		}

		this.command.add(0, binaryPath);
		var asyncs = new CompletableFuture<?>[filters.length];
		var outputDir = tempDir(String.valueOf(file.hashCode()));

		for(int i = 0; i < filters.length; i++) {
			var command = new ArrayList<>(this.command);
			command.add(1, "--filters=" + filters[i]);
			command.add(file.getCanonicalPath());
			command.add(filters[i] + ".png");

			var builder = new ProcessBuilder(command).directory(outputDir).redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD);

			asyncs[i] = builder.start().onExit();
		}

		CompletableFuture.allOf(asyncs).join();

		if(outputDir.listFiles() == null) {
			err("No output files found!");
			return;
		}

		var bestResult = Arrays.stream(outputDir.listFiles()).min(Comparator.comparingLong(File::length)).orElse(file);

		Files.copy(bestResult.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
}
