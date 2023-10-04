package dev.rdh.imag.util;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.Date;
import manifold.util.ILogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import static dev.rdh.imag.util.EpicLogger.Level.*;

/**
 * A logger that logs to the console, and optionally a file. This definitely isnt me justifying including manifold
 *
 * @noinspection unused, ResultOfMethodCallIgnored
 */
public class EpicLogger implements ILogger {

	public enum Level {
		DEBUG, TRACE, INFO, WARN, ERROR, FATAL
	}

	private final Object lock = new Object();

	private final String name;
	private boolean debug = false;
	private boolean trace = false;
	private boolean info = false;

	private PrintStream file;

	public EpicLogger(String name) {
		this.name = name;
	}

	public EpicLogger file(File logFile) {
		if(logFile.exists()) {
			if(logFile.isDirectory()) {
				logFile.delete();
			} else {
				compress(logFile);
			}
		}

		if(logFile.getParentFile() != null)
			logFile.getParentFile().mkdirs();

		try {
			this.file = new PrintStream(logFile);
			logFile.createNewFile();
		} catch (FileNotFoundException e) {
			fatal("Could not open log file: " + logFile.getAbsolutePath(), e);
		} catch (IOException e) {
			fatal("Could not create log file: " + logFile.getAbsolutePath(), e);
		}
		return this;
	}

	public EpicLogger file(String logFile) {
		return file(new File(logFile));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void debug(Object o) {
		debug(o, null);
	}

	@Override
	public void debug(Object o, Throwable throwable) {
		log(DEBUG, o, throwable);
	}

	@Override
	public void trace(Object o) {
		trace(o, null);
	}

	@Override
	public void trace(Object o, Throwable throwable) {
		log(TRACE, o, throwable);
	}

	@Override
	public void info(Object o) {
		info(o, null);
	}

	@Override
	public void info(Object o, Throwable throwable) {
		log(INFO, o, throwable);
	}

	@Override
	public void warn(Object o) {
		warn(o, null);
	}

	@Override
	public void warn(Object o, Throwable throwable) {
		log(WARN, o, throwable);
	}

	@Override
	public void error(Object o) {
		error(o, null);
	}

	@Override
	public void error(Object o, Throwable throwable) {
		log(ERROR, o, throwable);
	}

	@Override
	public void fatal(Object o) {
		fatal(o, null);
	}

	@Override
	public void fatal(Object o, Throwable throwable) {
		log(FATAL, o, throwable);
		System.exit(1);
	}

	public void log(Level level, Object o, Throwable throwable) {
		if (level == DEBUG && !debug
			|| level == TRACE && !trace
			|| level == INFO && !info
			|| file == null) return;

		raw(getPrefix(level) + o, throwable);
	}

	private void raw(Object o, Throwable throwable) {
		synchronized(lock) {
			if (file != null) {
				file.println(o);
				if (throwable != null) throwable.printStackTrace(file);
			}
		}
	}

	@Override
	public boolean isTraceEnabled() {
		return trace;
	}
	public EpicLogger setTraceEnabled(boolean trace) {
		this.trace = trace;
		return this;
	}
	public EpicLogger enableTrace() {
		return setTraceEnabled(true);
	}
	public EpicLogger disableTrace() {
		return setTraceEnabled(false);
	}

	@Override
	public boolean isInfoEnabled() {
		return info;
	}
	public EpicLogger setInfoEnabled(boolean info) {
		this.info = info;
		return this;
	}
	public EpicLogger enableInfo() {
		return setInfoEnabled(true);
	}
	public EpicLogger disableInfo() {
		return setInfoEnabled(false);
	}

	@Override
	public boolean isDebugEnabled() {
		return debug;
	}
	public EpicLogger setDebugEnabled(boolean debug) {
		this.debug = debug;
		return this;
	}
	public EpicLogger enableDebug() {
		return setDebugEnabled(true);
	}
	public EpicLogger disableDebug() {
		return setDebugEnabled(false);
	}


	private String getPrefix(Level level) {
		return "[${Thread.currentThread().getName()}/${level.name()}] (${name}): ";
	}

	public void close() {
		if(file != null) {
			file.close();
		}
	}

	private static void compress(File file) {
		String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		File newFile = new File(file.getParent() + File.separator + date + ".log");
		file.renameTo(newFile);
		file = newFile;

		var pb = new ProcessBuilder(Binary.ZOPFLI.path(), "--gzip", "--i1000", file.getAbsolutePath())
				.redirectError(Redirect.DISCARD)
				.redirectOutput(Redirect.DISCARD)
				.directory(file.getParentFile());

		try {
			pb.start().waitFor();
		} catch (Exception ignore) {}
		file.delete();
	}
}
