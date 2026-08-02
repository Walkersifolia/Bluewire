package com.bluewire;

import com.bluewire.config.DynamicWireRenderer_argb;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluewireModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bluewire");

    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance()
            .getModContainer("bluewire")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
        LOGGER.info("[BluewireBuild] version={} renderer=vanilla-block-model", version);

        DynamicWireRenderer_argb.register();
    }
}
