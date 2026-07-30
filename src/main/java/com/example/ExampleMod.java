package com.example;

import com.example.config.BluewireConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("bluewire");

    @Override
    public void onInitialize() {
        // 加载持久化配置（首次运行则创建默认配置），然后用加载的值重建颜色表
        BluewireConfig.load();
        BluewireConfig.updateColors();

        LOGGER.info("Bluewire 模组已初始化 — 红石线颜色可自由配置！");
    }
}
