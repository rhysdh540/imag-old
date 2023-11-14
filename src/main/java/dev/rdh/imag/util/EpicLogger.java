package dev.rdh.imag.util;

import dev.rdh.imag.processors.impl.archives.GZipProcessor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import manifold.util.ILogger;

import static dev.rdh.imag.util.EpicLogger.Level.*;
import static manifold.util.ManExceptionUtil.unchecked;

/**
 * A logger that logs to a file. This definitely isnt me justifying including manifold
 */
@SuppressWarnings({ "unused", "ResultOfMethodCallIgnored", "UnusedReturnValue" })
public class EpicLogger implements ILogger, AutoCloseable {

	private final Object lock = new Object();
	private final String name;
	private boolean debug = false;
	private boolean trace = false;
	private boolean info = false;
	private File file;
	private PrintStream out;

	public EpicLogger(String name) {
		this.name = name;
	}

	public CompressOrDelete file(File logFile) {
		this.file = logFile;
		if(logFile.exists() && logFile.isDirectory()) {
			logFile.delete();
		}

		if(logFile.getParentFile() != null) {
			logFile.getParentFile().mkdirs();
			deleteOldFiles(logFile.getParentFile());
		}

		return new CompressOrDelete(this);
	}

	public void discard() {
		if(file != null) {
			file.delete();
		}
		close();
	}

	public CompressOrDelete file(String logFile) {
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
		if(level == DEBUG && !debug || level == TRACE && !trace || level == INFO && !info || out == null) return;

		raw(getPrefix(level) + o, throwable);
	}

	private void raw(Object o, Throwable throwable) {
		synchronized(lock) {
			if(out != null) {
				out.println(o);
				if(throwable != null) throwable.printStackTrace(out);
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
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String time = sdf.format(new Date());
		return "[" + time + "] [" + Thread.currentThread().getName() + "/" + level.name() + "] (" + name + "): ";
	}

	@Override
	public void close() {
		if(out != null) {
			out.close();
		}
	}

	private void compress(File file) {
		String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		File newFile = new File(file.getParent() + File.separator + date + ".log");
		file.renameTo(newFile);
		file = newFile;

		try {
			byte[] data = Files.readAllBytes(file.toPath());
			byte[] compressed = GZipProcessor.compress(data);
			Files.write(Path.of(file.getAbsolutePath() + ".gz"), compressed);
		} catch (IOException e) {
			file.delete();
			throw unchecked(e);
		}
		file.delete();
	}

	private void deleteOldFiles(File file) {
		if(file.isFile()) return;

		long month = TimeUnit.DAYS.convert(30, TimeUnit.MILLISECONDS);

		File[] files = file.listFiles(f -> f.getName().endsWith(".gz") && System.currentTimeMillis() - f.lastModified() > month);

		if(files == null) return;

		for(File f : files) {
			f.delete();
		}
	}

	private void setupOut() {
		try {
			this.out = new PrintStream(new FileOutputStream(file), true);
			file.createNewFile();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}


	public static final class CompressOrDelete {
		private final EpicLogger logger;

		private CompressOrDelete(EpicLogger logger) {
			this.logger = logger;
		}

		public EpicLogger compress() {
			logger.compress(logger.file);
			logger.setupOut();
			return logger;
		}

		public EpicLogger delete() {
			if(logger.file != null && logger.file.exists()) {
				logger.file.delete();
			}
			logger.setupOut();
			return logger;
		}
	}

	public enum Level {
		DEBUG, TRACE, INFO, WARN, ERROR, FATAL
	}
}
