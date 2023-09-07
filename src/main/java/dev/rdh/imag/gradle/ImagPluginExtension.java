package dev.rdh.imag.gradle;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;

public abstract class ImagPluginExtension {
	@Input
	public abstract Property<Boolean> getEnabled();

	@Input
	public abstract Property<String> getFinalizeAfter();

	@Input
	public abstract SetProperty<String> getAdditionalTasks();

	@Input
	public abstract SetProperty<String> getIgnoredTasks();

	@Input
	public abstract Property<Boolean> getPng();

	@Input
	public abstract Property<Boolean> getNbt();

	@Input
	public abstract Property<Boolean> getOgg();

	public abstract Property<Integer> getPasses();

	{
		getEnabled().convention(true);
		getFinalizeAfter().convention("assemble");
		getPng().convention(true);
		getNbt().convention(true);
		getOgg().convention(true);
		getPasses().convention(3);
	}
}
