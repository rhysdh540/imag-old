package dev.rdh.imag.processors.audio;

import dev.rdh.imag.processors.AbstractFileProcessor;

public class OptiVorbisProcessor extends AbstractFileProcessor {

	public OptiVorbisProcessor() {
		super("ogg", false, "optivorbis -q -r ogg2ogg");
	}

	public static OptiVorbisProcessor newInstance() {
		return new OptiVorbisProcessor();
	}
}
