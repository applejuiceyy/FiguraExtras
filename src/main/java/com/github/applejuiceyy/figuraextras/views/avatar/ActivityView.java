package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.PossibleCapture;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.GraphBuilder;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
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
    Flow root = new Flow();
    Flow hoistedMeasurements = new Flow();
    boolean doneEntityInit = false;

    public ActivityView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;

        root.add(hoistedMeasurements);

        GraphBuilder.Frame frame = ((LuaRuntimeAccess) context.getValue().luaRuntime).figuraExtras$getInitFrame();

        hoistedMeasurement(context, "Init", frame);

        GraphBuilder.Frame eframe = ((LuaRuntimeAccess) context.getValue().luaRuntime).figuraExtras$getEntityInitFrame();

        if (eframe != null) {
            doneEntityInit = true;
            hoistedMeasurement(context, "Entity Init", eframe);
        }

        root.add(Elements.separator());

        ((LuaRuntimeAccess) context.getValue().luaRuntime).figuraExtras$getEntityInitFrame();


        differential = new Differential<>(
                ((GlobalsAccess) ((LuaRuntimeAccessor) context.getValue().luaRuntime).getUserGlobals()).figuraExtras$getCaptureState().getAvailableSingularCaptures().entrySet(),
                Map.Entry::getValue,
                o -> {
                    Instance i = new Instance(o);

                    Grid g = new Grid();
                    g.cols().percentage(1).content().fixed(10).content().rows().content();

                    g.setSurface(Surface.solid(root.getElements().size() % 2 == 0 ? 0xff000000 : 0xff222222));

                    root.add(g);
                    g.add(i.label);
                    g.add(i.measureButton).setColumn(1);
                    g.add(i.nowButton).setColumn(3);
                    return i;
                },
                o -> {}
        );

        additionPoint.accept(Elements.withVerticalScroll(root));
    }

    private void hoistedMeasurement(View.Context<Avatar> context, String name, GraphBuilder.Frame frame) {
        if (frame != null) {
            Grid grid = new Grid();
            hoistedMeasurements.add(grid);

            grid.cols().percentage(1).content();
            grid.rows().content();

            grid.add(name + " (" + frame.getInstructions() + " Instructions)").setRow(0);

            if (frame.getInstructions() > 0) {
                ParentElement<Grid.GridSettings> element = Button.minimal().addAnd("View Graph");
                element.activation.subscribe(ev ->
                    context.setView((ctx, ap) -> new FlameGraphView(ap, frame))
                );
                grid.add(element).setColumn(1);
            }
        }
    }

    @Override
    public void tick() {
        if (!doneEntityInit) {
            GraphBuilder.Frame eframe = ((LuaRuntimeAccess) context.getValue().luaRuntime).figuraExtras$getEntityInitFrame();

            if (eframe != null) {
                doneEntityInit = true;
                hoistedMeasurement(context, "Entity Init", eframe);
            }
        }
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
            nowButton.setNormalTexture(Surface.solid(0x00000000));
            measureButton = (Button) Button.minimal().addAnd("Measure");
            measureButton.setNormalTexture(Surface.solid(0x00000000));

            nowButton.activation.subscribe(event -> {
                Globals globals = ((LuaRuntimeAccessor) context.getValue().luaRuntime).getUserGlobals();
                ((GlobalsAccess) globals).figuraExtras$getCaptureState().queueSingularCapture(
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
