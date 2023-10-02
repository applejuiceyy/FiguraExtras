package com.github.applejuiceyy.figuraextras.components;

import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.FlameGraph;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.joml.Matrix4f;

public class FlameGraphComponent extends BaseComponent {

    private final FlameGraph.Frame frame;
    public int viewStart;
    public int viewEnd;

    public FlameGraphComponent(FlameGraph.Frame frame) {
        this.frame = frame;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        Tuple<FlameGraph.Frame, Integer> thing = getFrameInPos(mouseX - x, mouseY - y);
        FlameGraph.Frame selectedFrame = thing == null ? null : thing.getA();

        RenderSystem.setShader(GameRenderer::getRendertypeGuiShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        PoseStack stack = context.pose();
        stack.pushPose();

        stack.translate(x, y, 0);

        populate(stack.last().pose(), bufferBuilder, mouseX - x, mouseY - y, frame, selectedFrame, 0, 0, 1);

        BufferUploader.drawWithShader(bufferBuilder.end());

        stack.translate(0, 0, 10);
        renderOthers(context, frame, 0, 0);
        stack.popPose();

        cursorStyle(thing == null ? CursorStyle.POINTER : CursorStyle.HAND);
    }

    int toView(int in) {
        return (int) Mth.map(in, viewStart, viewEnd, 0, width);
    }

    int getColor(int pos) {
        return ((0xaa / pos) << 16) + (Math.min(0xff, Math.max(0x55 / pos, pos * 5)) << 8) + Math.min(0xff, Math.max(0x22 / pos, pos * 2));
    }

    private void populate(Matrix4f stack, BufferBuilder bufferBuilder, int mouseX, int mouseY, FlameGraph.Frame frame, FlameGraph.Frame selectedFrame, int offset, int y, int pos) {
        int startOffset = toView(offset);
        int endOffset = toView(offset + frame.getInstructions());
        int nonAlpha = getColor(pos);
        bufferBuilder.vertex(stack, startOffset, y, 0).color(0xff000000 + nonAlpha).endVertex();
        bufferBuilder.vertex(stack, startOffset, y + 20, 0).color(0xff000000 + nonAlpha).endVertex();
        bufferBuilder.vertex(stack, endOffset, y + 20, 0).color(0xff000000 + nonAlpha).endVertex();
        bufferBuilder.vertex(stack, endOffset, y, 0).color(0xff000000 + nonAlpha).endVertex();
        int selectionColor = selectedFrame == frame ? 0xffffffff : 0xff000000;

        int startRegionEnd = Math.min(startOffset + 5, (startOffset + endOffset) / 2);
        if (selectedFrame == frame && mouseX < startRegionEnd) {
            bufferBuilder.vertex(stack, startOffset, y, 0).color(0xffaaaaff).endVertex();
            bufferBuilder.vertex(stack, startOffset, y + 20, 0).color(0xffaaaaff).endVertex();
            bufferBuilder.vertex(stack, startRegionEnd, y + 20, 0).color(0xffaaaaff).endVertex();
            bufferBuilder.vertex(stack, startRegionEnd, y, 0).color(0xffaaaaff).endVertex();
        }

        if (endOffset - startOffset > 3) {
            bufferBuilder.vertex(stack, startOffset, y, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, startOffset, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, startOffset + 1, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, startOffset + 1, y, 0).color(selectionColor).endVertex();

            bufferBuilder.vertex(stack, endOffset - 1, y, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, endOffset - 1, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, endOffset, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, endOffset, y, 0).color(selectionColor).endVertex();

            bufferBuilder.vertex(stack, startOffset, y, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, startOffset, y + 1, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, endOffset, y + 1, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, endOffset, y, 0).color(selectionColor).endVertex();

            bufferBuilder.vertex(stack, startOffset, y + 19, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, startOffset, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, endOffset, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, endOffset, y + 19, 0).color(selectionColor).endVertex();

            if (frame.type == LuaDuck.CallType.TAIL) {
                int previous = getColor(pos - 1);

                bufferBuilder.vertex(stack, startOffset - 1, y + 4, 0).color(selectionColor).endVertex();
                bufferBuilder.vertex(stack, startOffset - 1, y + 16, 0).color(selectionColor).endVertex();
                bufferBuilder.vertex(stack, startOffset + 4, y + 16, 0).color(selectionColor).endVertex();
                bufferBuilder.vertex(stack, startOffset + 4, y + 4, 0).color(selectionColor).endVertex();

                bufferBuilder.vertex(stack, startOffset - 1, y + 5, 0).color(0xff000000 + previous).endVertex();
                bufferBuilder.vertex(stack, startOffset - 1, y + 15, 0).color(0xff000000 + previous).endVertex();
                bufferBuilder.vertex(stack, startOffset + 3, y + 15, 0).color(0xff000000 + previous).endVertex();
                bufferBuilder.vertex(stack, startOffset + 3, y + 5, 0).color(0xff000000 + previous).endVertex();
            }
        }

        if (frame.getReturnType() == LuaDuck.ReturnType.ERROR) {
            if (selectedFrame == frame) {
                bufferBuilder.vertex(stack, endOffset - 1, y, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(stack, endOffset - 1, y + 20, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(stack, endOffset, y + 20, 0).color(0xffff0000).endVertex();
                bufferBuilder.vertex(stack, endOffset, y, 0).color(0xffff0000).endVertex();
            } else {
                for (int yy = 0; yy < 20; yy += 5) {
                    bufferBuilder.vertex(stack, endOffset - 1, y + yy + 1, 0).color(0xffff0000).endVertex();
                    bufferBuilder.vertex(stack, endOffset - 1, y + yy + 2 + 1, 0).color(0xffff0000).endVertex();
                    bufferBuilder.vertex(stack, endOffset, y + yy + 2 + 1 + 1, 0).color(0xffff0000).endVertex();
                    bufferBuilder.vertex(stack, endOffset, y + yy + 1 + 1, 0).color(0xffff0000).endVertex();
                }
            }
        }

        for (FlameGraph.Marker child : frame.getMarkers()) {
            int toView = toView(offset + child.instruction());

            bufferBuilder.vertex(stack, toView, y + 17, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, toView - 3, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, toView, y + 20, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, toView + 3, y + 20, 0).color(selectionColor).endVertex();

            bufferBuilder.vertex(stack, toView, y + 18, 0).color(0xff00aa00).endVertex();
            bufferBuilder.vertex(stack, toView - 2, y + 20, 0).color(0xff00aa00).endVertex();
            bufferBuilder.vertex(stack, toView, y + 20, 0).color(0xff00aa00).endVertex();
            bufferBuilder.vertex(stack, toView + 2, y + 20, 0).color(0xff00aa00).endVertex();
        }

        for (FlameGraph.Region child : frame.getRegions()) {
            int start = toView(offset + child.instruction());
            int finish = toView(offset + child.instruction() + child.duration());

            bufferBuilder.vertex(stack, start - 1, y, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, start - 1, y + 3, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, finish + 1, y + 3, 0).color(selectionColor).endVertex();
            bufferBuilder.vertex(stack, finish + 1, y, 0).color(selectionColor).endVertex();

            bufferBuilder.vertex(stack, start, y, 0).color(0xff00aa00).endVertex();
            bufferBuilder.vertex(stack, start, y + 2, 0).color(0xff00aa00).endVertex();
            bufferBuilder.vertex(stack, finish, y + 2, 0).color(0xff00aa00).endVertex();
            bufferBuilder.vertex(stack, finish, y, 0).color(0xff00aa00).endVertex();
        }

        int i = 1;
        for (FlameGraph.Child child : frame.getChildren()) {
            int instructions = child.getInstructions();
            if (child instanceof FlameGraph.Frame f) {
                populate(stack, bufferBuilder, mouseX, mouseY, f, selectedFrame, offset, y + 20, i++);
            }
            offset += instructions;
        }
    }

    private String figureOutFrameName(FlameGraph.Frame frame) {
        if (frame.possibleName != null) {
            return "function " + frame.possibleName;
        }
        if (frame.boundClosure != null) {
            return "function " + frame.boundClosure.name();
        }
        return "[JAVA]";
    }

    private String figureOutSmallFrameName(FlameGraph.Frame frame) {
        if (frame.possibleName != null) {
            return frame.possibleName;
        }
        if (frame.boundClosure != null) {
            return frame.boundClosure.name();
        }
        return "[JAVA]";
    }

    private void renderOthers(OwoUIDrawContext context, FlameGraph.Frame frame, int offset, int y) {
        int start = toView(offset);
        if (start < 0) {
            start = 0;
        }
        int end = toView(offset + frame.getInstructions());
        if (end > width) {
            end = width;
        }
        Font font = Minecraft.getInstance().font;
        Component component = Component.literal(figureOutFrameName(frame) + " (" + frame.getInstructions() + " instructions)");

        if (end - start <= font.width(component)) {
            component = Component.literal(figureOutFrameName(frame));
            if (end - start <= font.width(component)) {
                component = Component.literal(figureOutSmallFrameName(frame));
                if (end - start <= font.width(component)) {
                    component = Component.literal("...");
                    if (end - start <= font.width(component) + 4) {
                        return;
                    }
                }
            }
        }

        context.drawString(
                font,
                component,
                (start + end) / 2 - font.width(component) / 2,
                y + 10 - 4,
                0xffffffff
        );

        for (FlameGraph.Child child : frame.getChildren()) {
            int instructions = child.getInstructions();
            if (viewStart < offset + instructions && child instanceof FlameGraph.Frame f) {
                renderOthers(context, f, offset, y + 20);
            }
            offset += instructions;
            if (offset > viewEnd) {
                break;
            }
        }
    }

    @Override
    public void drawTooltip(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.isInBoundingBox(mouseX, mouseY)) {
            mouseX -= x;
            mouseY -= y;

            Tuple<FlameGraph.Frame, Integer> thing = getFrameInPos(mouseX, mouseY);

            if (thing != null) {
                FlameGraph.Frame frame = thing.getA();


                Component component = null;

                FlameGraph.Marker marker = getMarkerInPos(frame, thing.getB(), mouseX, mouseY);
                if (marker != null) {
                    component = Component.literal(marker.name() + " (" + marker.instruction() + " instructions into the function)");
                }

                if (component == null) {
                    FlameGraph.Region region = getRegionInPos(frame, thing.getB(), mouseX, mouseY);
                    if (region != null) {
                        component = Component.literal(region.name() + " (" + region.instruction() + " instructions into the function taking " + region.duration() + " instructions)");
                    }
                }

                if (component == null) {
                    int x = toView(thing.getB());
                    int xn = toView(thing.getA().getInstructions() + thing.getB());
                    if (mouseX < Math.min(x + 5, (x + xn) / 2)) {
                        MutableComponent c;
                        component = c = Component.empty();

                        if (frame.type == LuaDuck.CallType.TAIL) {
                            c.append("Function was called on a tail return\n\n");
                        }
                        if (frame.argumentComponent != null) {
                            c.append(frame.argumentComponent);
                        }
                    }

                }

                if (component == null) {
                    if (frame.getReturnType() == LuaDuck.ReturnType.ERROR) {
                        int x = toView(thing.getB() + frame.getInstructions());
                        if (mouseX > x - 3) {
                            component = Component.literal("Function returned non-graciously with an error");
                        }
                    }
                }

                if (component == null) {
                    component = Component.literal(figureOutFrameName(frame) + " (" + frame.getInstructions() + " instructions)");
                }

                Font font = Minecraft.getInstance().font;
                context.drawTooltip(font, mouseX + x, mouseY + y,
                        font.split(component, 500).stream().map(ClientTooltipComponent::create).toList()
                );
            }
        }
    }

    public FlameGraph.Marker getMarkerInPos(FlameGraph.Frame frame, int offset, int x, int y) {
        if ((y - 1) % 20 > 16) {
            for (FlameGraph.Marker marker : frame.getMarkers()) {
                int thisX = toView(offset + marker.instruction());
                if (thisX < x + 4 && thisX > x - 4) {
                    return marker;
                }
            }
        }
        return null;
    }

    public FlameGraph.Region getRegionInPos(FlameGraph.Frame frame, int offset, int x, int y) {
        if ((y - 1) % 20 < 3) {
            for (FlameGraph.Region region : frame.getRegions()) {
                int start = toView(offset + region.instruction());
                int end = toView(offset + region.instruction() + region.duration());
                if (x > start && x < end) {
                    return region;
                }
            }
        }
        return null;
    }

    public Tuple<FlameGraph.Frame, Integer> getFrameInPos(int x, int y) {
        return getFrameInPos(frame, 0, x, y);
    }

    private Tuple<FlameGraph.Frame, Integer> getFrameInPos(FlameGraph.Frame frame, int offset, int x, int y) {
        if (y > 20) {
            for (FlameGraph.Child child : frame.getChildren()) {
                if (child instanceof FlameGraph.Frame f) {
                    Tuple<FlameGraph.Frame, Integer> r;
                    if ((r = getFrameInPos(f, offset, x, y - 20)) != null) {
                        return r;
                    }
                }
                offset += child.getInstructions();
            }
            return null;
        }

        if (x >= toView(offset) && x <= toView(frame.getInstructions() + offset)) {
            return new Tuple<>(frame, offset);
        }

        return null;
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {

        Tuple<FlameGraph.Frame, Integer> frame = getFrameInPos((int) mouseX, (int) (mouseY));

        if (frame != null) {
            frameSelected(frame.getA(), frame.getB());
            return true;
        }

        return false;
    }

    protected void frameSelected(FlameGraph.Frame a, Integer b) {

    }
}
