package dev.rdh.imag.gradle;

import dev.rdh.imag.Main;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class JarOptimizer {
	public final File workdir;
	public final File file;
	public final ImagPluginExtension config;
	public final Project project;
	public final boolean isChild;

	private JarOptimizer(File workdir, File file,
						 ImagPluginExtension config,
						 Project project, boolean isChild) {
		this.workdir = workdir;
		this.file = file;
		this.config = config;
		this.project = project;
		this.isChild = isChild;
	}

	private final Map<String, File> children = new HashMap<>();
	private final List<String> toIgnore = new ArrayList<>();

	public JarOptimizer(File workdir, File jarFile, ImagPluginExtension config, Project project) {
		this(workdir, jarFile, config, project, false);
	}

	public JarOptimizer unpack() {
		try (JarFile jar = new JarFile(file)) {
			if (jar.getManifest() != null) {
				jar.getManifest().getEntries().forEach((t, u) -> {
					for(var entry : u.entrySet()) {
						if(entry.getKey().toString().contains("Digest")) {
							toIgnore.add(t.split("/")[t.split("/").length - 1]);
							project.getLogger().warn("Ignoring " + t.split("/")[t.split("/").length - 1] + " as it is signed");
						}
					}

				});
			}

			var zip = new ZipInputStream(Files.newInputStream(file.toPath()));

			ZipEntry entry = zip.getNextEntry();
			while(entry != null) {
				final Path resolvedPath = workdir.toPath().resolve(entry.getName());

				if (!resolvedPath.startsWith(workdir.getPath())) {
					throw new RuntimeException("Zip slip somehow, don't do that: " + entry.getName());
				}

				if (!entry.isDirectory()) {
					Files.createDirectories(resolvedPath.getParent());
					Files.copy(zip, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
				} else Files.createDirectories(resolvedPath);

				entry = zip.getNextEntry();
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public JarOptimizer optimize() {
		List<File> files = Main.getFiles(workdir);
		files.removeIf(file -> toIgnore.contains(file.getName()));
		Main.run(files, config.getPasses().get());

		optimizeJarJar();
		return this;
	}

	public void repackTo(File file) {
		file.delete();
		try(JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
			if(isChild)
				jar.setLevel(Deflater.NO_COMPRESSION);
			else
				jar.setLevel(Deflater.BEST_COMPRESSION);

			Main.getFiles(workdir).forEach(optimizedFile -> {
				String path = optimizedFile
						.toPath()
						.relativize(workdir.toPath())
						.toFile()
						.getPath()
						.replace("\\", "/");
				JarEntry entry = new JarEntry(path);

				try {
					System.out.println("Adding " + path);
					jar.putNextEntry(entry);
					Files.copy(optimizedFile.toPath(), jar);
					jar.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			children.forEach((path, childJar) -> {
				JarEntry entry = new JarEntry(path.replace("\\", "/"));

				try {
					jar.putNextEntry(entry);
					Files.copy(childJar.toPath(), jar);
					jar.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void optimizeJarJar() {
		List<File> files = Main.getFiles(workdir);
		files.removeIf(file -> !file.getName().endsWith(".jar") || toIgnore.contains(file.getName())); // only jar files in list

		files.forEach(file -> {
			File workdir = this.workdir.toPath()
					.resolveSibling("jarjar")
					.resolve(file.getName())
					.toFile();
			workdir.mkdirs();

			JarOptimizer jar = new JarOptimizer(workdir, file, config, project, true)
					.unpack()
					.optimize();

			File outJarDir = this.workdir.toPath()
					.resolveSibling("optimizedJarJars")
					.toFile();
			outJarDir.mkdirs();

			File outJar = outJarDir.toPath()
					.resolve(file.getName())
					.toFile();

			try {
				outJar.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			jar.repackTo(outJar);

			String path = file.toPath()
					.relativize(this.workdir.toPath())
					.toFile()
					.getPath();
			children.put(path, outJar);
		});
	}
}
