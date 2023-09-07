package dev.rdh.imag.gradle;

import dev.rdh.imag.Main;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskOutputs;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ImagPlugin implements Plugin<Project> {
	@Override
	public void apply(Project target) {
		ImagPluginExtension config = target.getExtensions().create("imag", ImagPluginExtension.class);

		Task taskToFinalizeAfter = target.getTasks().findByName(config.getFinalizeAfter().get());
		if(taskToFinalizeAfter == null)
			taskToFinalizeAfter = target.getTasks().findByName("assemble");
		if(taskToFinalizeAfter == null) return;
		target.afterEvaluate(project -> {
			Main.init(project);
			if(!config.getEnabled().get()) {
				project.getLogger().lifecycle("imag disabled on this build through the 'enabled' option");
				return;
			}
			Main.logger.info("1");

			Set<String> tasks = new HashSet<>(Set.of("jar", "remapJar", "shadowJar"));
			tasks.addAll(config.getAdditionalTasks().get());
			tasks.removeAll(config.getIgnoredTasks().get());

			tasks.forEach(name -> {
				Task taskToHook = project.getTasks().findByName(name);
				if(taskToHook == null) return;
				FileCollection toOptimize = taskToHook.getOutputs().getFiles();
				Task optimizeTask = project.getTasks().create("imag-" + name, OptimizeJarTask.class, task -> {
					task.setGroup("imag");
					task.setDescription("Auto-generated task to optimize " + name + " with imag");

					task.getInputs().files(toOptimize);
					task.getOutputs().file(toOptimize);

					File buildDir = project.getBuildDir()
							.toPath()
							.resolve("imag")
							.resolve(name)
							.toFile();
					task.getBuildDir().set(buildDir);
					task.getExtension().set(config);
				});

				String after = config.getFinalizeAfter().get();
				Task finalizeTask = project.getTasks().findByName(after);
				if(finalizeTask != null) {
					finalizeTask.finalizedBy(optimizeTask);
				}
				optimizeTask.dependsOn(taskToHook);
			});
		});
	}
}
