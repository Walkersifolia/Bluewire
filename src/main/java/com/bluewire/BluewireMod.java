package com.bluewire;

import com.bluewire.config.BluewireConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluewireMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("bluewire");

    @Override
    public void onInitialize() {
        BluewireConfig.load();
        BluewireConfig.updateColors();
        LOGGER.info("Bluewire 模组已初始化 — 红石线颜色可自由配置！");
    }
}
