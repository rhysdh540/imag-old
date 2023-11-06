package dev.rdh.imag;

public final class Options {
	public boolean png = true;
	public boolean nbt = true;
	public boolean ogg = true;
	public boolean archives = true;

	public boolean quiet = false;
	public boolean log = true;

	public boolean force = false;
	public boolean encode = true;
	public int threads = 8;
	public int passes = 3;

	public Options() {}
}
