package dev.rdh.imag.processors;

import dev.rdh.imag.util.Binary;
import java.io.File;

public class OxiPngProcessor extends DefaultFileProcessor {

	private OxiPngProcessor(String command) {
		super("png", false, Binary.OXIPNG, "-o max -q", command);
	}

	public static OxiPngProcessor get1() {
		return new OxiPngProcessor("");
	}

	public static OxiPngProcessor get2() {
		return new OxiPngProcessor("-a");
	}

	@Override
	public String name() {
		return "Oxipng";
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		this.command.add(0, binary.path());
		this.command.add("--out=" + output);
		this.command.add(file.getCanonicalPath());
	}
}
