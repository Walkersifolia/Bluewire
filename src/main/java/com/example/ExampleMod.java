package com.example;

import com.example.config.BluewireConfig;
import com.example.mixin.ExampleMixin;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("bluewire");

	@Override
	public void onInitialize() {
		// Load persisted config (or create defaults on first run),
		// then rebuild the wire colour table with the loaded values.
		BluewireConfig.load();
		ExampleMixin.updateColors();

		LOGGER.info("Bluewire mod initialized — redstone dust is now configurable!");
	}
}