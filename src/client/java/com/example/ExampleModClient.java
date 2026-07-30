package com.example;

import com.example.config.RainbowAnimator;
import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RainbowAnimator.register();
    }
}
