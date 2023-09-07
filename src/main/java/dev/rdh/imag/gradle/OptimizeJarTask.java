package dev.rdh.imag.gradle;

import dev.rdh.imag.Main;
import dev.rdh.imag.processors.Util;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

public abstract class OptimizeJarTask extends DefaultTask {
	@Input
	public abstract Property<File> getBuildDir();

	@Input
	public abstract Property<ImagPluginExtension> getExtension();

	@TaskAction
	public void optimizeJars() {
		getInputs().getFiles().forEach(file -> {
			File tempJarDir = getBuildDir()
					.get()
					.toPath()
					.resolve("root-jar")
					.toFile();
			Util.deleteRecursively(tempJarDir);
			tempJarDir.mkdirs();

			new JarOptimizer(tempJarDir, file, getExtension().get(), getProject())
					.unpack()
					.optimize()
					.repackTo(file);
			file.renameTo(new File(file.getParentFile(), file.getName().replace(".jar", "-original.jar")));
		});

		Main.logger.lifecycle("Finished optimizing jars for task " + getName() + "!");
	}
}
