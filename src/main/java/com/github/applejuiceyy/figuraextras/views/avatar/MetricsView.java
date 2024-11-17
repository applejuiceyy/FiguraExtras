package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.components.graph.*;
import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.CaptureState;
import com.github.applejuiceyy.figuraextras.tech.captures.Hook;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.GraphBuilder;
import com.github.applejuiceyy.figuraextras.tech.captures.figura.FiguraData;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.util.MathUtil;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.avatar.capture.FlameGraphView;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.permissions.Permissions;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MetricsView implements Lifecycle {
    private final GraphComponent chart;
    private final long start;
    private final Runnable unsubscriber;
    private boolean collecting = true;
    private boolean onlyInstructions = false;

    private final Map<Avatar.Instructions, Permissions> instructionMapping;
    private final Set<Avatar.Instructions> instructionsToInclude = new HashSet<>();

    public MetricsView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint, Object toMeasure) {
        Avatar value = context.getValue();
        instructionMapping = doBindings(value);

        Grid root = new Grid();

        root.rows().content().percentage(1).cols().percentage(1);

        Grid top = new Grid();
        root.add(top);

        top.cols().percentage(1).fixed(10).content().rows().content();

        Button button = Button.minimal();
        button.addAnd("Pause");
        button.activation.subscribe(eitherCausedEvent -> {
            collecting = !collecting;
            button.setText(Component.literal(collecting ? "Pause" : "Resume"));
        });

        top.add(button);

        Button onlyInstructionsButton = Button.minimal();
        onlyInstructionsButton.addAnd("Only Instruction Count");
        onlyInstructionsButton.activation.subscribe(eitherCausedEvent -> {
            onlyInstructions = !onlyInstructions;
            onlyInstructionsButton.setText(Component.literal(onlyInstructions ? "Capture Everything" : "Only Instruction Count"));
        });

        top.add(onlyInstructionsButton).setColumn(2);

        chart = new GraphComponent();
        root.add(chart).setRow(1);


        LineCollection lineCollection = LineCollection.of(0xff776622, 0xffff6622);
        chart.addCollection(lineCollection);
        ((Axis.DefaultAxis) Objects.requireNonNull(chart.getAxisRenderer())).setBottom(Axis.SideKind.NOTCHES);

        chart.addRenderer(new InGraphRendererBaker() {
            @Override
            public InGraphRenderer getBaked(BakingContext bakingContext) {
                return new InGraphRenderer() {
                    @Override
                    public void render(GuiGraphics context) {
                        Permissions.Category[] values = Permissions.Category.values();
                        Font font = Minecraft.getInstance().font;

                        MutableComponent footer = Component.empty();
                        MutableComponent ceiling = Component.empty();

                        for (Avatar.Instructions instructions : instructionsToInclude) {
                            for (Permissions.Category category : values) {
                                if(category == Permissions.Category.BLOCKED || category == Permissions.Category.MAX) {
                                    continue;
                                }

                                Permissions permissions = instructionMapping.get(instructions);
                                int defaultInstructions = permissions.getDefault(category);

                                Bounds bounds = bakingContext.renderingBounds();
                                if(bounds.yMin() < defaultInstructions && defaultInstructions < bounds.yMax()) {
                                    int mapped = (int) MathUtil.map(defaultInstructions, bounds.yMin(), bounds.yMax(), bakingContext.viewportHeight(), 0);
                                    String friendlyInstructionName = permissions.name.substring(0, permissions.name.length() - 5).toLowerCase();
                                    String fullName = friendlyInstructionName + " " + category.name().toLowerCase();
                                    context.drawString(font, fullName, 0, mapped - 4, category.color);
                                    context.fill(font.width(fullName) + 2, mapped, bakingContext.viewportWidth(), mapped + 1, category.color | 0xaa000000);
                                }
                                else if(bounds.yMin() > defaultInstructions) {
                                    footer.append(Component.literal("v").withStyle(s -> s.withColor(category.color)));
                                }
                                else if(bounds.yMax() < defaultInstructions) {
                                    ceiling.append(Component.literal("^").withStyle(s -> s.withColor(category.color)));
                                }
                            }
                        }
                        context.pose().scale(2, 2, 0);
                        context.drawString(font, footer, 0, bakingContext.viewportHeight() / 2 - font.lineHeight, 0xff000000);
                        context.pose().scale(0.5f, 0.5f, 0);
                        context.drawString(font, ceiling, 0, 0, 0xff000000);
                    }

                    @Override
                    public boolean doTooltip(DefaultCancellableEvent.ToolTipEvent toolTipEvent, double x, double y) {
                        return false;
                    }

                    @Override
                    public boolean handleMouseDown(double x, double y, int button) {
                        return false;
                    }
                };
            }
        });

        this.start = System.currentTimeMillis();


        Globals globals = ((LuaRuntimeAccessor) value.luaRuntime).getUserGlobals();
        CaptureState captureState = ((GlobalsAccess) globals).figuraExtrass$getCaptureState();

        unsubscriber = captureState.getEvent().subscribe(new MetricHook(value.luaRuntime.typeManager, toMeasure, result -> {
            if(!collecting) return;

            long l = System.currentTimeMillis();
            float o = l - start;

            int i = result.instructions();
            GraphBuilder.Frame frame = result.frame();

            lineCollection.add(new LineCollection.DataPoint() {
                @Override
                public double getY() {
                    return i;
                }

                @Override
                public double getX() {
                    return o;
                }

                @Override
                public void onHover(DefaultCancellableEvent.ToolTipEvent event) {
                    MutableComponent literal = Component.literal(i + " instructions");
                    if(frame == null) {
                        literal.append(Component.literal("\n\n(Frames not available)").withStyle(ChatFormatting.RED));
                    }
                    event.add(literal);
                }

                @Override
                public void onClick(double x, double y) {
                    if(frame == null) return;
                    context.setView((c, ap) -> new FlameGraphView(ap, frame));
                }
            });
            while(lineCollection.size() > 0) {
                LineCollection.DataPoint dataPoint = lineCollection.get(0);
                if(dataPoint.getX() < (l - start) - 5000) {
                    lineCollection.remove(0);
                }
                else {
                    break;
                }
            }
        }));

        additionPoint.accept(root);
    }

    private Map<Avatar.Instructions, Permissions> doBindings(Avatar avatar) {
        HashMap<Avatar.Instructions, Permissions> map = new HashMap<>();
        map.put(avatar.init, Permissions.INIT_INST);
        map.put(avatar.render, Permissions.RENDER_INST);
        map.put(avatar.worldRender, Permissions.WORLD_RENDER_INST);
        map.put(avatar.tick, Permissions.TICK_INST);
        map.put(avatar.worldTick, Permissions.WORLD_TICK_INST);
        map.put(avatar.animation, Permissions.ANIMATION_INST);
        return map;
    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {
        if(!collecting) return;
        long l = System.currentTimeMillis();
        chart.setXMin((float) (l - start - 5000));
        chart.setXMax((float) (l - start));
    }

    @Override
    public void dispose() {
        unsubscriber.run();
    }

    class MetricHook implements Hook {
        @Nullable Hook inner = null;
        LuaTypeManager manager;
        Object toRun;
        Consumer<CaptureResult> frameConsumer;

        MetricHook(LuaTypeManager manager, Object toRun, Consumer<CaptureResult> frameConsumer) {
            this.manager = manager;
            this.toRun = toRun;
            this.frameConsumer = frameConsumer;
        }

        @Override
        public void startEvent(Discernible discernible) {
            if(inner == null) {
                FiguraData figuraData = discernible.getAs(FiguraData.class).orElseThrow();
                if(Objects.equals(figuraData.toRun(), this.toRun)) {
                    inner = onlyInstructions ? new OnlyInstructions(instructions -> {
                        inner = null;
                        frameConsumer.accept(new CaptureResult(instructions, null));
                    }) :  new GraphBuilder(manager, frame -> {
                        inner = null;
                        frameConsumer.accept(new CaptureResult(frame.getInstructions(), frame));
                    });
                    Avatar.Instructions instructions = figuraData.instructions();
                    instructionsToInclude.add(instructions);
                }
            }
            if(inner != null) {
                inner.startEvent(discernible);
            }
        }

        @Override
        public void end() {
            if(inner != null) {
                inner.end();
            }
        }

        @Override
        public void marker(String name) {
            if(inner == null) return;
            inner.marker(name);
        }

        @Override
        public void region(String regionName) {
            if(inner == null) return;
            inner.region(regionName);
        }

        @Override
        public void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.CallType type, String possibleName) {
            if(inner == null) return;
            inner.intoFunction(luaClosure, varargs, stack, type, possibleName);
        }

        @Override
        public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, Object returns, LuaDuck.ReturnType type) {
            if(inner == null) return;
            inner.outOfFunction(luaClosure, varargs, stack, returns, type);
        }

        @Override
        public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int i, int pc) {
            if(inner == null) return;
            inner.instruction(luaClosure, varargs, stack, i, pc);
        }
    }

    static class OnlyInstructions implements Hook {
        int eventCount = 0;
        int instructions = 0;
        IntConsumer consumer;

        OnlyInstructions(IntConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void startEvent(Discernible discernible) {
            eventCount++;
        }

        @Override
        public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
            instructions++;
        }

        @Override
        public void end() {
            if(--eventCount == 0) {
                consumer.accept(instructions);
            }
        }
    }

    record CaptureResult(int instructions, @Nullable GraphBuilder.Frame frame) {}
}
