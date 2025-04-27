package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.Rectangle;
import com.github.applejuiceyy.figuraextras.util.MathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
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

    public interface BakedAxisRenderer extends InGraphRenderer {
        void renderAxis(GuiGraphics context);
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

        private DoubleDoublePair computeBestSpace(double dataMinSpace, double dataMaxSpace, float physicalSpace, float spacePerDot) {
            double step = 1;
            double dataWorkingSpace = dataMaxSpace - dataMinSpace;
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

            return DoubleDoublePair.of(step, Math.ceil(dataMinSpace / step) * step);
        }

        @Override
        BakedAxisRenderer getBaked(int width, int height, Bounds dataBounds) {
            Font font = Minecraft.getInstance().font;

            int textSize = 0;
            int topOccupation = computeOccupation(top, 9);
            int bottomOccupation = computeOccupation(bottom, 9);

            int verticalSpace = height - topOccupation - bottomOccupation;

            DoubleDoublePair verticalNumbers = computeBestSpace(dataBounds.yMin(), dataBounds.yMax(), verticalSpace, font.lineHeight * 2);

            double verticalStepSize = verticalNumbers.firstDouble();
            double verticalStepStart = verticalNumbers.secondDouble();

            if(right == SideKind.NUMBERS || left == SideKind.NUMBERS) {
                for(double i = verticalStepStart; i < dataBounds.yMax(); i += verticalStepSize) {
                    String string = i % 1 == 0 ? Integer.toString((int) i) : Double.toString(i);
                    textSize = Math.max(textSize, font.width(string));
                }
            }

            int leftOccupation = computeOccupation(left, textSize);
            int rightOccupation = computeOccupation(right, textSize);

            int horizontalSpace = width - leftOccupation - rightOccupation;

            DoubleDoublePair horizontalNumbers = computeBestSpace(dataBounds.xMin(), dataBounds.xMax(), verticalSpace, 50);

            double horizontalStepSize = horizontalNumbers.firstDouble();
            double horizontalStepStart = horizontalNumbers.secondDouble();


            Rectangle innerGraph = Rectangle.of(leftOccupation, topOccupation, width - leftOccupation - rightOccupation, height - topOccupation - bottomOccupation);

            return new BakedAxisRenderer() {
                @Override
                public void render(GuiGraphics context) {

                    for(double i = verticalStepStart; i <= dataBounds.yMax(); i += verticalStepSize) {
                        int mapped = (int) MathUtil.map(i, dataBounds.yMin(), dataBounds.yMax(), verticalSpace, 0);

                        context.fill(0, mapped, innerGraph.getWidth(), mapped + 1, 0x33ffffff);
                    }
                    for(double i = horizontalStepStart; i <= dataBounds.xMax(); i += horizontalStepSize) {
                        int mapped = (int) MathUtil.map(i, dataBounds.xMin(), dataBounds.xMax(), 0, horizontalSpace);

                        context.fill(mapped, 0, mapped + 1, innerGraph.getHeight(), 0x33ffffff);
                    }
                }

                @Override
                public boolean doTooltip(DefaultCancellableEvent.ToolTipEvent toolTipEvent, double x, double y) {
                    return false;
                }

                @Override
                public boolean handleMouseDown(double x, double y, int button) {
                    return false;
                }

                @Override
                public void renderAxis(GuiGraphics context) {
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
                            for(double i = verticalStepStart; i <= dataBounds.yMax(); i += verticalStepSize) {
                                renderVerticalNumber(context, calculateTextPos, i);
                            }
                        case NOTCHES:
                            for(double i = verticalStepStart; i <= dataBounds.yMax(); i += verticalStepSize) {
                                int mapped = (int) MathUtil.map(i, dataBounds.yMin(), dataBounds.yMax(), verticalSpace, 0);
                                context.fill(notchPos, mapped, notchPos - 1, mapped + 1, 0xffaaaaaa);
                            }
                        case BAR:
                            context.fill(barPos, 0, barPos - 1, verticalSpace, 0xffaaaaaa);
                    }
                }

                private void renderVerticalNumber(GuiGraphics context, Object2IntFunction<String> calculateTextPos, double i) {
                    String string = i % 1 == 0 ? Integer.toString((int) i) : Double.toString(i);
                    int mapped = (int) MathUtil.map(i, dataBounds.yMin(), dataBounds.yMax(), verticalSpace, 0);
                    context.drawString(Minecraft.getInstance().font, string, calculateTextPos.applyAsInt(string), (int) MathUtil.constrain(mapped - 4, 0, verticalSpace), 0xffaaaaaa);
                }

                private void renderHorizontally(GuiGraphics context, SideKind sideKind, int barPos, int notchPos, int textPos) {
                    switch (sideKind) {
                        case NONE:
                            break;
                        case NUMBERS:
                            for(double i = horizontalStepStart; i <= dataBounds.xMax(); i += horizontalStepSize) {
                                String string = i % 1 == 0 ? Integer.toString((int) i) : Double.toString(i);
                                int mapped = (int) MathUtil.map(i, dataBounds.xMin(), dataBounds.xMax(), 0, horizontalSpace);
                                int size = font.width(string);
                                context.drawString(Minecraft.getInstance().font, string, (int) MathUtil.constrain(mapped - size / 2f, 0, horizontalSpace - size), textPos, 0xffaaaaaa);
                            }
                        case NOTCHES:
                            for(double i = horizontalStepStart; i <= dataBounds.xMax(); i += horizontalStepSize) {
                                int mapped = (int) MathUtil.map(i, dataBounds.xMin(), dataBounds.xMax(), 0, horizontalSpace);
                                context.fill(mapped, notchPos, mapped + 1, notchPos - 1, 0xffaaaaaa);
                            }
                        case BAR:
                            context.fill(0, barPos, horizontalSpace, barPos - 1, 0xffaaaaaa);
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
