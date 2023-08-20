package dev.rdh.imag.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public class NbtFileProcessor extends AbstractFileProcessor {
	private NbtFileProcessor() {
		super("nbt", false, "zopfli --gzip --i1000 -c");
	}

	public static NbtFileProcessor newInstance() {
		return new NbtFileProcessor();
	}

	@Override
	protected void addFilesToArgList(File file) throws Exception {
		File decompressedFile = new File(".workdir" + File.separator + file.getName() + ".decompressed");

		try (GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(file));
			 FileOutputStream decompressedOutputStream = new FileOutputStream(decompressedFile)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
				decompressedOutputStream.write(buffer, 0, bytesRead);
			}
		}

		this.command.add(decompressedFile.getCanonicalPath());
	}

	@Override
	public void process(File file) throws Exception {
		if(!file.getCanonicalPath().endsWith(fileType))
			return;

		File outputDir = new File(".workdir" + File.separator + file.hashCode()).getCanonicalFile();
		outputDir.mkdirs();

		addFilesToArgList(file);

		File output = new File(outputDir, "output." + fileType);

		ProcessBuilder pb = new ProcessBuilder(command)
				.directory(outputDir)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.redirectOutput(output);

		Process proc = pb.start();
		proc.waitFor();

		if(output.exists() && output.length() < file.length()) {
			file.delete();
			Path path = file.toPath();
			Files.move(output.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
		}

		//noinspection DataFlowIssue
		for(File f : outputDir.listFiles()) {
			f.delete();
		}

		outputDir.delete();
	}
}
