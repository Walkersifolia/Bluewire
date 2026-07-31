package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bluewire");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[BluewireBuild] version=1.0.2");
    }
}
