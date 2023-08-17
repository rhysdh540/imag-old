package dev.rdh.imag.processors.image;

import dev.rdh.imag.processors.AbstractFileProcessor;

public class OxiPngProcessor extends AbstractFileProcessor {
	public static final AbstractFileProcessor FIRST = new OxiPngProcessor("--nx");
	public static final AbstractFileProcessor SECOND = new OxiPngProcessor("");

	private OxiPngProcessor(String command) {
		super("png", false, "oxipng -q -a -o max -i 0 " + command);
	}
}
