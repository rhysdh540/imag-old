package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.BinaryFileProcessor;
import dev.rdh.imag.util.Binary;

public class EctPngProcessor extends BinaryFileProcessor {
	private EctPngProcessor() {
		super(false, Binary.ECT, "-9 --allfilters-b -strip -keep -quiet --mt-deflate");
	}

	public static EctPngProcessor newInstance() {
		return new EctPngProcessor();
	}

	@Override
	public String name() {
		return "ECT";
	}

	@Override
	public String extension() {
		return "png";
	}
}
