package dev.rdh.imag.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Main {
	static final File imagJar = new File(System.getProperty("user.home") + File.separator + ".imag" + File.separator + "imag.jar");

	public static void main(final String[] originalArgs) throws Exception {
		List<String> args = new ArrayList<>(Arrays.asList(originalArgs));
		if(!imagJar.exists()) {
			System.err.println("imag.jar not found in ~/.imag\nDownloading...");
			download();
		}
		final String jvmArgs = "-XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+AlwaysActAsServerClassMachine -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+UseNUMA -XX:NmethodSweepActivity=1 -XX:ReservedCodeCacheSize=400M -XX:NonNMethodCodeHeapSize=12M -XX:ProfiledCodeHeapSize=194M -XX:NonProfiledCodeHeapSize=194M -XX:-DontCompileHugeMethods -XX:MaxNodeLimit=240000 -XX:NodeLimitFudgeFactor=8000 -XX:+UseVectorCmov -XX:+PerfDisableSharedMem -XX:+UseFastUnorderedTimeStamps -XX:+UseCriticalJavaThreadPriority -XX:ThreadPriorityPolicy=1 -XX:AllocatePrefetchStyle=3 -XX:+UseG1GC -XX:MaxGCPauseMillis=37 -XX:+PerfDisableSharedMem -XX:G1HeapRegionSize=16M -XX:G1NewSizePercent=23 -XX:G1ReservePercent=20 -XX:SurvivorRatio=32 -XX:G1MixedGCCountTarget=3 -XX:G1HeapWastePercent=20 -XX:InitiatingHeapOccupancyPercent=10 -XX:G1RSetUpdatingPauseTimePercent=0 -XX:MaxTenuringThreshold=1 -XX:G1SATBBufferEnqueueingThresholdPercent=30 -XX:G1ConcMarkStepDurationMillis=5.0 -XX:G1ConcRSHotCardLimit=16 -XX:G1ConcRefinementServiceIntervalMillis=150 -XX:GCTimeRatio=99";
		StringBuilder userArgs = new StringBuilder();
		for(String a : args) {
			if(a.startsWith("-X") || a.startsWith("-D") || a.startsWith("-XX")) {
				userArgs.append(" ").append(a);
				args.remove(a);
			}
			if(a.startsWith("--memory=")) {
				String mem = a.substring(9);
				userArgs.append(" -Xmx").append(mem).append(" -Xms").append(mem);
				args.remove(a);
			}
		}
		if(!userArgs.toString().contains("-Xmx")) {
			String mem = Runtime.getRuntime().maxMemory() / 1024 / 1024 + "M";
			userArgs.append(" -Xmx").append(mem).append(" -Xms").append(mem);
		}

		String java = System.getProperty("os.name").toLowerCase().contains("win") ? "javaw" : "java";

		String cmd = java + " " + jvmArgs + " " + userArgs + " -jar " + imagJar.getAbsolutePath() + " " + String.join(" ", args);

		Process p = new ProcessBuilder(cmd).inheritIO().start();
		p.waitFor();
	}

	static void download() throws Exception {
		String version = getVersion();

		try(InputStream stream = onlineResource(getUrl("imag-" + version + ".jar"))) {
			if(stream == null) throw new IOException("URL not valid or you are offline");

			Files.copy(stream, imagJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("imag downloaded to " + imagJar.getAbsolutePath());
		} catch (IOException e) {
			System.err.println("Failed to download version.txt");
			e.printStackTrace();
		}
	}

	public static @Nullable InputStream onlineResource(@NotNull String url) {
		try {
			return new URL(url).openStream();
		} catch (IOException e) {
			return null;
		}
	}

	public static String getUrl(String path) {
		return "https://github.com/rhysdh540/imag/releases/latest/download/" + path;
	}

	public static String getVersion() throws IOException {
		try(InputStream resource = onlineResource(getUrl("version.txt"))) {
			if(resource == null) throw new IOException("URL not valid or you are offline");
			return new String(resource.readAllBytes());
		}
	}
}
