package dev.rdh.imag.processors;

public class OxiPngProcessor extends AbstractFileProcessor {
	public static final AbstractFileProcessor FIRST = new OxiPngProcessor("-o max -a --nx -i 0");
	public static final AbstractFileProcessor SECOND = new OxiPngProcessor("-o max -a -i 0");

	private OxiPngProcessor(String command) {
		super("png", "oxipng -q " + command);
	}
}
