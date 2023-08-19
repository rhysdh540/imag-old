package dev.rdh.imag.processors.image;

import dev.rdh.imag.processors.AbstractFileProcessor;

public class OxiPngProcessor extends AbstractFileProcessor {

	private OxiPngProcessor(String command) {
		super("png", false, "oxipng -q -a -o max -i 0 " + command);
	}

	public static OxiPngProcessor newFirstInstance() {
		return new OxiPngProcessor("--nx");
	}

	public static OxiPngProcessor newSecondInstance() {
		return new OxiPngProcessor("");
	}
}
