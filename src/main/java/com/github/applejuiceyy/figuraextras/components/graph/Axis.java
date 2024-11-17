package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.tech.gui.geometry.Rectangle;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

@MethodsReturnNonnullByDefault
@FieldsAreNonnullByDefault
public abstract class Axis extends Invalidates {
    public static DefaultAxis createDefault() {
        return new DefaultAxis();
    }

    abstract BakedAxisRenderer getBaked(int width, int height, Bounds dataBounds);

    public interface BakedAxisRenderer {
        void render(GuiGraphics context);
        Rectangle getInnerGraph();
    }

    static public class DefaultAxis extends Axis {
        SideKind top = SideKind.NONE;
        SideKind bottom = SideKind.NUMBERS;
        SideKind left = SideKind.NUMBERS;
        SideKind right = SideKind.NONE;

        public SideKind getTop() {
            return top;
        }

        public DefaultAxis setTop(SideKind top) {
            this.top = top;
            invalidate();
            return this;
        }

        public SideKind getBottom() {
            return bottom;
        }

        public DefaultAxis setBottom(SideKind bottom) {
            this.bottom = bottom;
            invalidate();
            return this;
        }

        public SideKind getLeft() {
            return left;
        }

        public DefaultAxis setLeft(SideKind left) {
            this.left = left;
            invalidate();
            return this;
        }

        public SideKind getRight() {
            return right;
        }

        public DefaultAxis setRight(SideKind right) {
            this.right = right;
            invalidate();
            return this;
        }

        private DefaultAxis() {

        }

        private int computeOccupation(SideKind kind, int textSize) {
            switch (kind) {
                case NONE -> { return 0; }
                case BAR -> {
                    return 1;
                }
                case NOTCHES -> {
                    return 3;
                }
                case NUMBERS -> {
                    return 3 + textSize;
                }
            }
            throw new IllegalStateException(kind + " not expected");
        }

        private FloatFloatPair computeBestSpace(float dataMinSpace, float dataMaxSpace, float physicalSpace, float spacePerDot) {
            float step = 1;
            float dataWorkingSpace = dataMaxSpace - dataMinSpace;
            float realEstate = physicalSpace / spacePerDot;

            if((dataWorkingSpace / step) > realEstate) {
                while((dataWorkingSpace / step) > realEstate) {
                    step *= 2;
                }
            }
            else {
                while((dataWorkingSpace / step) < realEstate / 2) {
                    step /= 2;
                }
            }

            return FloatFloatPair.of(step, (float) (Math.ceil(dataMinSpace / step) * step));
        }

        @Override
        BakedAxisRenderer getBaked(int width, int height, Bounds dataBounds) {
            Font font = Minecraft.getInstance().font;

            int textSize = 0;
            int topOccupation = computeOccupation(top, 9);
            int bottomOccupation = computeOccupation(bottom, 9);

            int verticalSpace = height - topOccupation - bottomOccupation;

            FloatFloatPair verticalNumbers = computeBestSpace(dataBounds.yMin(), dataBounds.yMax(), verticalSpace, font.lineHeight * 2);

            float verticalStepSize = verticalNumbers.firstFloat();
            float verticalStepStart = verticalNumbers.secondFloat();

            if(right == SideKind.NUMBERS || left == SideKind.NUMBERS) {
                for(float i = verticalStepStart; i < dataBounds.yMax(); i += verticalStepSize) {
                    String string = i % 1 == 0 ? Integer.toString((int) i) : Float.toString(i);
                    textSize = Math.max(textSize, font.width(string));
                }
            }

            int leftOccupation = computeOccupation(left, textSize);
            int rightOccupation = computeOccupation(right, textSize);

            int horizontalSpace = width - leftOccupation - rightOccupation;

            FloatFloatPair horizontalNumbers = computeBestSpace(dataBounds.xMin(), dataBounds.xMax(), verticalSpace, 50);

            float horizontalStepSize = horizontalNumbers.firstFloat();
            float horizontalStepStart = horizontalNumbers.secondFloat();


            Rectangle innerGraph = Rectangle.of(leftOccupation, topOccupation, width - rightOccupation, height - bottomOccupation);

            return new BakedAxisRenderer() {
                @Override
                public void render(GuiGraphics context) {
                    PoseStack pose = context.pose();
                    pose.pushPose();

                    pose.translate(0, topOccupation, 0);
                    int l = innerGraph.getX() + innerGraph.getWidth() + 1;
                    renderVertically(context, left, innerGraph.getX(), innerGraph.getX() - 1, string -> innerGraph.getX() - 2 - font.width((String) string));
                    renderVertically(context, right, l, l + 1, str -> l + 2);
                    pose.translate(0, -topOccupation, 0);

                    pose.translate(leftOccupation - 1, 0, 0);
                    renderHorizontally(context, top, innerGraph.getY(), innerGraph.getY() - 1, innerGraph.getY() - font.lineHeight);
                    int b = innerGraph.getY() + innerGraph.getHeight() + 1;
                    renderHorizontally(context, bottom, b, b + 1, b + 2);
                    pose.translate(-leftOccupation, 0, 0);

                    pose.popPose();
                }

                @Override
                public Rectangle getInnerGraph() {
                    return innerGraph;
                }

                private void renderVertically(GuiGraphics context, SideKind sideKind, int barPos, int notchPos, Object2IntFunction<String> calculateTextPos) {
                    switch (sideKind) {
                        case NONE:
                            break;
                        case NUMBERS:
                            boolean forceRenderLast = false;
                            if((dataBounds.yMax() - verticalStepStart) % verticalStepSize != 0) {
                                renderVerticalNumber(context, calculateTextPos, dataBounds.yMax());
                                forceRenderLast = true;
                            }
                            for(float i = verticalStepStart; i <= dataBounds.yMax(); i += verticalStepSize) {
                                if(forceRenderLast) {
                                    float mapped = Util.map(i, dataBounds.yMin(), dataBounds.yMax(), verticalSpace, 0);
                                    if(mapped - 4 < dataBounds.yMax() + font.lineHeight) {
                                        continue;
                                    }
                                }
                                renderVerticalNumber(context, calculateTextPos, i);
                            }
                        case NOTCHES:
                            for(float i = verticalStepStart; i <= dataBounds.yMax(); i += verticalStepSize) {
                                int mapped = (int) Util.map(i, dataBounds.yMin(), dataBounds.yMax(), verticalSpace, 0);
                                context.fill(notchPos, mapped, notchPos - 1, mapped + 1, 0xffffffff);
                            }
                        case BAR:
                            context.fill(barPos, 0, barPos - 1, verticalSpace, 0xffffffff);
                    }
                }

                private void renderVerticalNumber(GuiGraphics context, Object2IntFunction<String> calculateTextPos, float i) {
                    String string = i % 1 == 0 ? Integer.toString((int) i) : Float.toString(i);
                    int mapped = (int) Util.map(i, dataBounds.yMin(), dataBounds.yMax(), verticalSpace, 0);
                    context.drawString(Minecraft.getInstance().font, string, calculateTextPos.applyAsInt(string), (int) Util.constrain(mapped - 4, 0, verticalSpace), 0xffffffff);
                }

                private void renderHorizontally(GuiGraphics context, SideKind sideKind, int barPos, int notchPos, int textPos) {
                    switch (sideKind) {
                        case NONE:
                            break;
                        case NUMBERS:
                            for(float i = horizontalStepStart; i <= dataBounds.xMax(); i += horizontalStepSize) {
                                String string = i % 1 == 0 ? Integer.toString((int) i) : Float.toString(i);
                                int mapped = (int) Util.map(i, dataBounds.xMin(), dataBounds.xMax(), 0, horizontalSpace);
                                int size = font.width(string);
                                context.drawString(Minecraft.getInstance().font, string, (int) Util.constrain(mapped - size / 2f, 0, horizontalSpace - size), textPos, 0xffffffff);
                            }
                        case NOTCHES:
                            for(float i = horizontalStepStart; i <= dataBounds.xMax(); i += horizontalStepSize) {
                                int mapped = (int) Util.map(i, dataBounds.xMin(), dataBounds.xMax(), 0, horizontalSpace);
                                context.fill(mapped, notchPos, mapped + 1, notchPos - 1, 0xffffffff);
                            }
                        case BAR:
                            context.fill(0, barPos, horizontalSpace, barPos - 1, 0xffffffff);
                    }
                }
            };
        }




    }

    public enum SideKind {
        NONE,
        BAR,
        NOTCHES,
        NUMBERS
    }
}
