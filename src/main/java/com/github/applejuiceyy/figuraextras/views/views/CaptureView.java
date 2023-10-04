package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.CaptureOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.FlameGraph;
import com.github.applejuiceyy.figuraextras.util.Differential;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.views.views.capture.FlameGraphView;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import org.luaj.vm2.Globals;

import java.util.Map;

public class CaptureView implements InfoViews.View {
    InfoViews.Context context;
    Differential<Map.Entry<Object, CaptureOpportunity>, Object, Instance> differential;
    FlowLayout root = Containers.verticalFlow(Sizing.content(), Sizing.fill(100));

    public CaptureView(InfoViews.Context context) {
        this.context = context;
        differential = new Differential<>(
                () -> ((LuaRuntimeAccess) context.getAvatar().luaRuntime).figuraExtrass$getNoticedPotentialCaptures().entrySet().iterator(),
                Map.Entry::getValue,
                o -> {
                    Instance i = new Instance(o.getValue());
                    root.child(i.root);
                    return i;
                },
                o -> {
                }
        );
    }

    @Override
    public void tick() {

    }

    @Override
    public Component getRoot() {
        return root;
    }

    @Override
    public void render() {
        differential.update(Instance::update);
    }

    @Override
    public void dispose() {
        differential.dispose();
    }

    class Instance {
        private final CaptureOpportunity value;
        public SmallButtonComponent root;

        public Instance(CaptureOpportunity value) {
            this.value = value;
            root = new SmallButtonComponent(net.minecraft.network.chat.Component.empty());
            root.mouseDown().subscribe((x, y, d) -> {
                Globals globals = ((LuaRuntimeAccessor) context.getAvatar().luaRuntime).getUserGlobals();
                ((GlobalsAccess) globals).figuraExtrass$setCurrentlySearchingForCapture(
                        new ActiveOpportunity<SecondaryCallHook>(value, new FlameGraph(context.getAvatar().luaRuntime.typeManager, frame -> {
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
