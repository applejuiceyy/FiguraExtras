package com.github.applejuiceyy.figuraextras;


import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.config.ConfigType;

import java.util.ArrayList;
import java.util.logging.Logger;

public class FiguraExtras implements ClientModInitializer {
    public static ArrayList<DetachedWindow> windows = new ArrayList<>();

    private static final ConfigType.Category category = new ConfigType.Category("FiguraExtras");
    public static ConfigType.StringConfig progName =  new ConfigType.StringConfig("prog_name", category, "Visual Studio Code");
    public static ConfigType.StringConfig progCmd = new ConfigType.StringConfig("prog_cmd", category, "code \"$folder\"");

    static {
        category.name = Component.literal("FiguraExtras");
        progName.name = Component.literal("Open With Program Name");
        progCmd.name = Component.literal("Open With Program Command");
    }

    public static Logger logger = Logger.getLogger("FiguraExtras");

    @Override
    public void onInitializeClient() {
    }
}
