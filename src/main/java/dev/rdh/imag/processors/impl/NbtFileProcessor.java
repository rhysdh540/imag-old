package dev.rdh.imag.processors.impl;

import dev.rdh.imag.processors.impl.archives.GZipProcessor;

public class NbtFileProcessor extends GZipProcessor {
	private NbtFileProcessor() {
		super();
	}

	public static NbtFileProcessor newInstance() {
		return new NbtFileProcessor();
	}

	@Override
	public String name() {
		return "CustomNBT";
	}

	@Override
	public String extension() {
		return "nbt";
	}
}
