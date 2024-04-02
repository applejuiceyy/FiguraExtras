package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.CaptureOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.FlameGraph;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.util.Differential;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.views.views.capture.FlameGraphView;
import net.minecraft.ChatFormatting;
import org.luaj.vm2.Globals;

import java.util.Map;

public class CaptureView implements Lifecycle {
    InfoViews.Context context;
    Differential<Map.Entry<Object, CaptureOpportunity>, Object, Instance> differential;
    Flow root = new Flow();

    public CaptureView(InfoViews.Context context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;
        differential = new Differential<>(
                ((LuaRuntimeAccess) context.getAvatar().luaRuntime).figuraExtrass$getNoticedPotentialCaptures().entrySet(),
                Map.Entry::getValue,
                o -> {
                    Instance i = new Instance(o.getValue());
                    root.add(i.root);
                    return i;
                },
                o -> {
                }
        );

        additionPoint.accept(root);
    }

    @Override
    public void tick() {

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
        public Button root;
        public Label label = new Label();

        public Instance(CaptureOpportunity value) {
            this.value = value;

            root = (Button) Button.minimal().addAnd(label);
            root.mouseDown.subscribe(event -> {
                Globals globals = ((LuaRuntimeAccessor) context.getAvatar().luaRuntime).getUserGlobals();
                ((GlobalsAccess) globals).figuraExtrass$setCurrentlySearchingForCapture(
                        new ActiveOpportunity<SecondaryCallHook>(value, new FlameGraph(context.getAvatar().luaRuntime.typeManager, frame -> {
                            context.setView((context, additionPoint) -> new FlameGraphView(context, additionPoint, frame));
                        })));
            });
        }

        public void update() {
            label.setText(
                    net.minecraft.network.chat.Component.literal(value.name)
                            .append(net.minecraft.network.chat.Component.literal(
                                    " (last called " + (System.currentTimeMillis() - value.mostRecentCallMillis) + " milliseconds ago)"
                            ).withStyle(ChatFormatting.YELLOW))
            );
        }
    }
}
