package dev.rdh.imag.processors;

import dev.rdh.imag.util.Binary;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

public class NbtFileProcessor extends DefaultFileProcessor {
	private NbtFileProcessor() {
		super("nbt", false, Binary.ZOPFLI, "--gzip --i1000 -c");
	}

	public static NbtFileProcessor get() {
		return new NbtFileProcessor();
	}

	@Override
	public String name() {
		return "NBT";
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		File decompressedFile = tempFile(output + "-decompressed");

		try(var in = new GZIPInputStream(new FileInputStream(file)); var out = new FileOutputStream(decompressedFile)) {
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
		if(!file.getCanonicalPath().endsWith(fileType)) return;

		var name = String.valueOf(file.hashCode());

		addFilesToArgList(file, name);

		var pb = new ProcessBuilder(command).directory(file.getParentFile()).redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(file);

		pb.start().waitFor();
	}
}
