package dev.rdh.imag.processors.image;

import dev.rdh.imag.Main;
import dev.rdh.imag.processors.AbstractFileProcessor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static dev.rdh.imag.Main.err;

public class ZopfliPngProcessor extends AbstractFileProcessor {

	public static final ZopfliPngProcessor INSTANCE = new ZopfliPngProcessor();

	private ZopfliPngProcessor() {
		super("png", false, "zopflipng --iterations=15 -y --lossy_transparent");
	}

	@Override
	public void process(File file) throws Exception {
		final char[] filters = {'0', '1', '2', '3', '4', 'm', 'e', 'p', 'b'};

		CompletableFuture<?>[] asyncs = new CompletableFuture<?>[filters.length];
		HashMap<Long, Character> ids = new HashMap<>(filters.length,1.0f);
		File outputDir = new File(".workdir").getCanonicalFile();
		outputDir.mkdir();

		for(int i = 0; i < filters.length; i++) {
			this.command.add(1, "--filters=" + filters[i]);
			this.command.add(file.getCanonicalPath());
			this.command.add(filters[i] + ".png");

			ProcessBuilder builder = new ProcessBuilder(this.command);

			builder.directory(outputDir)
					.redirectError(ProcessBuilder.Redirect.DISCARD)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD);
			Process proc = builder.start();

			synchronized(ids) {
				ids.put(proc.pid(), filters[i]);
			}

			asyncs[i] = proc.onExit().thenAcceptAsync(p -> {
				char id;
				synchronized(ids) {
					id = ids.get(p.pid());
				}
				if(Main.debug) {
					synchronized (System.out) {
						System.out.print(id);
						System.out.flush();
					}
				}
			});
		}
		for(CompletableFuture<?> cf : asyncs) cf.get();
		if(Main.debug) {
			synchronized (System.out) {
				System.out.println();
			}
		}

		if(outputDir.listFiles() == null) {
			err("No output files found!");
			return;
		}

		File bestResult = Arrays.stream(outputDir.listFiles())
				.min(Comparator.comparingLong(File::length))
				.orElse(file);

		Path a = file.toPath();
		Path b = bestResult.toPath();
		Files.copy(b, a, StandardCopyOption.REPLACE_EXISTING);

		for(File f : outputDir.listFiles()) {
			f.delete();
		}
		outputDir.delete();
	}
}
