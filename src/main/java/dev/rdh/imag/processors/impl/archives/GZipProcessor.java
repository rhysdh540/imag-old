package dev.rdh.imag.processors.impl.archives;

import dev.rdh.imag.processors.FileProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

public class GZipProcessor implements FileProcessor {

	public static GZipProcessor newInstance() {
		return new GZipProcessor();
	}

	@Override
	public String name() {
		return "CustomGZip";
	}

	@Override
	public String extension() {
		return "gz";
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getName().endsWith(extension())) return;
		try(GZIPInputStream in = new GZIPInputStream(new FileInputStream(file))) {
			byte[] data = new byte[(int) file.length()];
			in.read(data);

			byte[] compressed = compress(data);

			if(compressed.length >= data.length) return;

			try(FileOutputStream out = new FileOutputStream(file)) {
				out.write(compressed);
			}
		}
	}

	public static native byte[] compress(byte[] data);
}
