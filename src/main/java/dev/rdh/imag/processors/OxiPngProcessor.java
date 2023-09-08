package dev.rdh.imag.processors;

import dev.rdh.imag.Binary;

import java.io.File;

public class OxiPngProcessor extends AbstractFileProcessor {

	private OxiPngProcessor(String command) {
		super("png", false, Binary.OXIPNG,"-o max -q -a --zc 12", command);
	}

	@Override
	protected void addFilesToArgList(File file, String output) throws Exception {
		this.command.add(0, binary.toString());
		this.command.add("--out=" + output);
		this.command.add(file.getCanonicalPath());
	}

	public static OxiPngProcessor get1() {
		return new OxiPngProcessor("--nx");
	}

	public static OxiPngProcessor get2() {
		return new OxiPngProcessor("");
	}
}
