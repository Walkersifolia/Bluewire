package com.example;

import com.example.config.RainbowOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RainbowOverlayRenderer.register();
    }
}
