package com.github.applejuiceyy.figuraextras;


import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.config.ConfigType;

import java.util.ArrayList;
import java.util.logging.Logger;

public class FiguraExtras implements ClientModInitializer {
    public static ArrayList<DetachedWindow> windows = new ArrayList<>();

    public static final ConfigType.StringConfig progName;
    public static final ConfigType.BoolConfig disableServerToasts;
    public static final ConfigType.BoolConfig disableCachedRendering;
    public static final ConfigType.StringConfig progCmd;
    private static final ConfigType.Category category;

    static {
        try {
            Class.forName("org.figuramc.figura.config.Configs");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        category = new ConfigType.Category("FiguraExtras");
        progName = new ConfigType.StringConfig("prog_name", category, "Visual Studio Code");
        disableServerToasts = new ConfigType.BoolConfig("disable_server_toasts", category, false);
        disableCachedRendering = new ConfigType.BoolConfig("disable_cached_rendering", category, false);
        progCmd = new ConfigType.StringConfig("prog_cmd", category, "code \"$folder\"");

        category.name = Component.literal("FiguraExtras");
        progName.name = Component.literal("Open With Program Name");
        progCmd.name = Component.literal("Open With Program Command");
        disableServerToasts.name = Component.literal("Disable Backend Toasts");
        disableCachedRendering.name = Component.literal("Disable Cached Rendering");
    }

    public static Logger logger = Logger.getLogger("FiguraExtras");

    @Override
    public void onInitializeClient() {

    }
}
