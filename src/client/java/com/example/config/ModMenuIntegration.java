package com.example.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Bridges BluewireConfig into the Mod Menu mod list so users can click the
 * blue gear icon to reach the settings screen.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BluewireConfigScreen::create;
    }
}
