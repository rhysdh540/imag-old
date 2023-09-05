package dev.rdh.imag.processors;

import dev.rdh.imag.Binary;

public class OxiPngProcessor extends AbstractFileProcessor {

	private OxiPngProcessor(String command) {
		super("png", false, Binary.OXIPNG,"-q -a -o max -i 0 ", command);
	}

	public static OxiPngProcessor getFirst() {
		return new OxiPngProcessor("--nx");
	}

	public static OxiPngProcessor getSecond() {
		return new OxiPngProcessor("");
	}
}
