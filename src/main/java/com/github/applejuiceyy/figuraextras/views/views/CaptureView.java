package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.CaptureOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.FlameGraph;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.views.views.capture.FlameGraphView;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import org.luaj.vm2.Globals;

import java.util.HashMap;
import java.util.Map;

public class CaptureView implements InfoViews.View {
    InfoViews.Context context;
    HashMap<Object, Instance> knownCaptures = new HashMap<>();

    FlowLayout root = Containers.verticalFlow(Sizing.content(), Sizing.fill(100));

    public CaptureView(InfoViews.Context context) {
        this.context = context;
    }

    @Override
    public void tick() {
        HashMap<Object, CaptureOpportunity> captures
                = ((LuaRuntimeAccess) context.getAvatar().luaRuntime).figuraExtrass$getNoticedPotentialCaptures();

        for (Map.Entry<Object, CaptureOpportunity> entry : captures.entrySet()) {
            if (!knownCaptures.containsKey(entry.getKey())) {
                Instance instance = new Instance(entry.getValue());
                root.child(instance.root);
                knownCaptures.put(entry.getKey(), instance);
            }

            knownCaptures.get(entry.getKey()).update();
        }
    }

    @Override
    public Component getRoot() {
        return root;
    }

    @Override
    public void render() {

    }

    @Override
    public void dispose() {

    }

    class Instance {
        private final CaptureOpportunity value;
        public SmallButtonComponent root;

        public Instance(CaptureOpportunity value) {
            this.value = value;
            root = new SmallButtonComponent(net.minecraft.network.chat.Component.empty());
            root.mouseDown().subscribe((x, y, d) -> {
                Globals globals = ((LuaRuntimeAccessor) context.getAvatar().luaRuntime).getUserGlobals();
                ((GlobalsAccess) globals).figuraExtrass$setCurrentlySearchingForCapture(new ActiveOpportunity<SecondaryCallHook>(value, new FlameGraph(globals, frame -> {
                    context.setView(context -> new FlameGraphView(context, frame));
                })));
                return true;
            });
        }

        public void update() {
            root.setText(
                    net.minecraft.network.chat.Component.literal(value.name)
                            .append(net.minecraft.network.chat.Component.literal(
                                    " (last called " + (System.currentTimeMillis() - value.mostRecentCallMillis) + " milliseconds ago)"
                            ).withStyle(ChatFormatting.YELLOW))
            );
        }
    }
}
