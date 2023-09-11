package dev.rdh.imag.processors;

import dev.rdh.imag.Binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public class NbtFileProcessor extends AbstractFileProcessor {
	private NbtFileProcessor() {
		super("nbt", false, Binary.ZOPFLI, "--gzip --i1000 -c");
	}

	public static NbtFileProcessor get() {
		return new NbtFileProcessor();
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		File decompressedFile = tempFile(output + "-decompressed");

		try (var in = new GZIPInputStream(new FileInputStream(file)); var out = new FileOutputStream(decompressedFile)) {
			var buffer = new byte[4096];
			int bytesRead;
			while((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
		}
		this.command.add(0, binary.path());
		this.command.add(decompressedFile.getCanonicalPath());
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;

		var name = String.valueOf(file.hashCode());

		var output = tempFile(name);

		addFilesToArgList(file, name);

		var pb = new ProcessBuilder(command)
				.directory(output.getParentFile())
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.redirectOutput(output);

		pb.start().waitFor();

		if(output.exists() && output.length() < file.length()) {
			Files.move(output.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
