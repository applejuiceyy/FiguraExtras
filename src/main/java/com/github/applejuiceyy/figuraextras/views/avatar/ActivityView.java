package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.PossibleCapture;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.GraphBuilder;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Differential;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.avatar.capture.FlameGraphView;
import net.minecraft.ChatFormatting;
import org.figuramc.figura.avatar.Avatar;
import org.luaj.vm2.Globals;

import java.util.Map;

public class ActivityView implements Lifecycle {
    View.Context<Avatar> context;
    Differential<Map.Entry<Object, PossibleCapture>, Object, Instance> differential;
    Grid root = new Grid();

    public ActivityView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;
        root.cols().percentage(1).content().fixed(10).content();
        differential = new Differential<>(
                ((GlobalsAccess) ((LuaRuntimeAccessor) context.getValue().luaRuntime).getUserGlobals()).figuraExtrass$getCaptureState().getAvailableSingularCaptures().entrySet(),
                Map.Entry::getValue,
                o -> {
                    Instance i = new Instance(o);
                    root.rows().content();
                    root.add(i.label).setRow(root.rowCount() - 1);
                    root.add(i.measureButton).setRow(root.rowCount() - 1).setColumn(1);
                    root.add(i.nowButton).setRow(root.rowCount() - 1).setColumn(3);
                    return i;
                },
                o -> {}
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
        private final Map.Entry<Object, PossibleCapture> value;
        public Button nowButton;
        public Button measureButton;
        public Label label = new Label();


        public Instance(Map.Entry<Object, PossibleCapture> o) {
            this.value = o;

            nowButton = (Button) Button.minimal().addAnd("Capture Next");
            measureButton = (Button) Button.minimal().addAnd("Measure");

            nowButton.activation.subscribe(event -> {
                Globals globals = ((LuaRuntimeAccessor) context.getValue().luaRuntime).getUserGlobals();
                ((GlobalsAccess) globals).figuraExtrass$getCaptureState().queueSingularCapture(
                        new ActiveOpportunity<>(value.getValue(), new GraphBuilder(context.getValue().luaRuntime.typeManager, frame -> {
                            context.setView((context, additionPoint) -> new FlameGraphView(additionPoint, frame));
                        })));
            });

            measureButton.activation.subscribe(event -> {
                context.setView((c, ap) -> new MetricsView(c, ap, value.getKey()));
            });
        }

        public void update() {
            PossibleCapture possibleCapture = value.getValue();
            label.setText(
                    net.minecraft.network.chat.Component.literal(possibleCapture.name)
                            .append(net.minecraft.network.chat.Component.literal(
                                    " (last called " + (System.currentTimeMillis() - possibleCapture.mostRecentCallMillis) + " milliseconds ago)"
                            ).withStyle(ChatFormatting.YELLOW))
            );
        }
    }
}
