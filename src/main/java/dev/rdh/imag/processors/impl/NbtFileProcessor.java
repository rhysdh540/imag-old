package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.impl.archives.GZipProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

public class NbtFileProcessor extends GZipProcessor {
	private NbtFileProcessor() {
		super();
	}

	public static NbtFileProcessor get() {
		return new NbtFileProcessor();
	}

	@Override
	public String name() {
		return "CustomNBT";
	}

	@Override
	public String fileType() {
		return "nbt";
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		File decompressedFile = tempFile(output + "-decompressed");

		try(GZIPInputStream in = new GZIPInputStream(new FileInputStream(file)); FileOutputStream out = new FileOutputStream(decompressedFile)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
		}
		this.command.add(0, binary.path());
		this.command.add(decompressedFile.getCanonicalPath());
	}
}
