package dev.rdh.imag.processors;

import dev.rdh.imag.util.Binary;

import java.io.File;

public class OxiPngProcessor extends AbstractFileProcessor {

	private OxiPngProcessor(String command) {
		super("png", false, Binary.OXIPNG,"-o max -q", command);
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		this.command.add(0, binary.path());
		this.command.add("--out=" + output);
		this.command.add(file.getCanonicalPath());
	}

	public static OxiPngProcessor get1() {
		return new OxiPngProcessor("");
	}

	public static OxiPngProcessor get2() {
		return new OxiPngProcessor("-a");
	}
}
