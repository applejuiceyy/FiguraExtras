package com.github.applejuiceyy.figuraextras;


import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import net.fabricmc.api.ClientModInitializer;

import java.util.ArrayList;
import java.util.logging.Logger;

public class FiguraExtras implements ClientModInitializer {
    public static ArrayList<DetachedWindow> windows = new ArrayList<>();


    public static Logger logger = Logger.getLogger("FiguraExtras");

    @Override
    public void onInitializeClient() {
    }
}
