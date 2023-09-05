package dev.rdh.imag.processors;

import dev.rdh.imag.Binary;

@SuppressWarnings("DuplicatedCode")
public class PngOutProcessor extends AbstractFileProcessor {

	private PngOutProcessor() {
		super("png", true, Binary.PNGOUT,"-q -y -r -k1 -s0");
	}

	public static PngOutProcessor get() {
		return new PngOutProcessor();
	}
}
