package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.PossibleCapture;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.GraphBuilder;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.util.Differential;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.avatar.capture.FlameGraphView;
import net.minecraft.ChatFormatting;
import org.figuramc.figura.avatar.Avatar;
import org.luaj.vm2.Globals;

import java.util.Map;

public class CaptureView implements Lifecycle {
    View.Context<Avatar> context;
    Differential<Map.Entry<Object, PossibleCapture>, Object, Instance> differential;
    Flow root = new Flow();

    public CaptureView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;
        differential = new Differential<>(
                ((GlobalsAccess) ((LuaRuntimeAccessor) context.getValue().luaRuntime).getUserGlobals()).figuraExtrass$getCaptureState().getAvailableSingularCaptures().entrySet(),
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
        private final PossibleCapture value;
        public Button root;
        public Label label = new Label();

        public Instance(PossibleCapture value) {
            this.value = value;

            root = (Button) Button.minimal().addAnd(label);
            root.mouseDown.subscribe(event -> {
                Globals globals = ((LuaRuntimeAccessor) context.getValue().luaRuntime).getUserGlobals();
                ((GlobalsAccess) globals).figuraExtrass$getCaptureState().queueSingularCapture(
                        new ActiveOpportunity<>(value, new GraphBuilder(context.getValue().luaRuntime.typeManager, frame -> {
                            context.setView((context, additionPoint) -> new FlameGraphView(additionPoint, frame));
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
