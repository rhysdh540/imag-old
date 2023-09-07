package dev.rdh.imag.gradle;

import dev.rdh.imag.Main;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
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
		taskToFinalizeAfter.doLast(a -> {
			Main.init(target);
			if(!config.getEnabled().get()) {
				Main.logger.lifecycle("imag disabled on this build through the 'enabled' option");
				return;
			}

			Set<String> tasks = new HashSet<>(Set.of("jar", "remapJar", "shadowJar"));
			tasks.addAll(config.getAdditionalTasks().get());
			tasks.removeAll(config.getIgnoredTasks().get());

			tasks.forEach(name -> {
				Task t = target.getTasks().findByName(name);
				if(t == null) return;
				t.doLast(task -> {
					Main.logger.lifecycle("Running imag on task " + name);
					TaskOutputs out = task.getOutputs();
					if (!out.getFiles().getFiles().isEmpty()) {
						out.getFiles().getFiles().forEach(file -> {
							File workdir = Main.workdir
									.toPath()
									.resolve("imag-" + name)
									.resolve(String.valueOf(file.hashCode()))
									.toFile();

							new JarOptimizer(workdir, file, config, target)
									.unpack()
									.optimize()
									.repackTo(file);
						});
					}
				});
			});
		});
	}
}
