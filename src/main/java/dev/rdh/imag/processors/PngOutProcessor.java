package dev.rdh.imag.processors;

@SuppressWarnings("DuplicatedCode")
public class PngOutProcessor extends AbstractFileProcessor {

	private PngOutProcessor() {
		super("png", true, "pngout -q -y -r -k1 -s0");
	}

	public static PngOutProcessor newInstance() {
		return new PngOutProcessor();
	}
}
