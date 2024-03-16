package com.github.applejuiceyy.figuraextras.tech.gui.layout;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Rectangle;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class Grid extends ParentElement<Grid.GridSettings> {
    // TODO: caching?
    List<Spacing> rows = new ArrayList<>();
    List<Spacing> columns = new ArrayList<>();

    int[] rowSizings = new int[0];
    int[] columnSizings = new int[0];

    private static void applySizingsToElements(Iterable<Tuple<Element, GridSettings>> elements, Function<Element, Integer> positionGetter, TriConsumer<Element, Integer, Integer> positionSetter, int[] sizings) {
        for (Tuple<Element, GridSettings> e : elements) {
            Element element = e.getA();
            int position = positionGetter.apply(element);
            int o = 0;
            for (int i = 0; i < position; i++) {
                o += sizings[i];
            }
            positionSetter.accept(element, o, sizings[position]);
        }
    }

    private static int[] computeSizing(Iterable<Tuple<Element, GridSettings>> elements, List<Spacing> spacing, Function<Element, Integer> optimalSizeGetter, Function<Element, Integer> positionGetter) {
        int[] spacingSizes = new int[spacing.size()];
        float totalPercentage = 0;
        int percentageOccupation = 0;

        for (int i = 0, spacingSize = spacing.size(); i < spacingSize; i++) {
            Spacing space = spacing.get(i);
            if (space.kind == SpacingKind.FIXED) {
                spacingSizes[i] = (int) space.value;
            } else if (space.kind == SpacingKind.PERCENTAGE) {
                totalPercentage += space.value;
            }
        }

        for (Tuple<Element, GridSettings> e : elements) {
            Element element = e.getA();
            int position = positionGetter.apply(element);
            if (spacing.get(position).kind == SpacingKind.CONTENT) {
                spacingSizes[position] = Math.max(spacingSizes[position], optimalSizeGetter.apply(element));
            } else if (spacing.get(position).kind == SpacingKind.PERCENTAGE) {
                int optimalSize = optimalSizeGetter.apply(element);
                float thisPercentage = spacing.get(position).value / totalPercentage;
                int spacingOccupation = (int) (percentageOccupation * thisPercentage);
                if (optimalSize > spacingOccupation) {
                    percentageOccupation = (int) (optimalSize / thisPercentage);
                }
            }
        }

        for (int i = 0, spacingSize = spacing.size(); i < spacingSize; i++) {
            Spacing space = spacing.get(i);
            if (space.kind == SpacingKind.PERCENTAGE) {
                float thisPercentage = space.value / totalPercentage;
                spacingSizes[i] = (int) (percentageOccupation * thisPercentage);
            }
        }

        return spacingSizes;
    }

    @Override
    public void renderLayoutDebug(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Rectangle inner = getInnerSpace();
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY(), 0);
        renderDebugSpacing(graphics, columnSizings, columns, inner.getHeight());
        graphics.pose().rotateAround(Axis.ZP.rotation((float) Math.toRadians(90)), 0, 0, 0);
        graphics.pose().translate(0, -getWidth(), 0);
        renderDebugSpacing(graphics, rowSizings, rows, inner.getWidth());
        graphics.pose().popPose();
    }

    private void renderDebugSpacing(GuiGraphics graphics, int[] spaces, List<Spacing> spacings, int vertical) {
        float totalPercentage = 0;
        for (Spacing row : spacings) {
            if (row.kind == SpacingKind.PERCENTAGE) {
                totalPercentage += row.value;
            }
        }

        int v = 0;
        int textReach = -1;
        int textw = 0;
        for (int i = 0; i < spaces.length; i++) {
            int rowSizing = spaces[i];
            String px = spaces[i] + "px";
            Spacing kind = spacings.get(i);
            Component toRender = Component.literal(switch (spacings.get(i).kind) {
                case FIXED -> "Fixed:" + px;
                case CONTENT -> "Content: " + px;
                case PERCENTAGE -> Math.floor(kind.value / totalPercentage * 100) + "% -> " + px;
            });
            // implementation could cause issues, but it's not going to be too common to be an issue
            if (textReach > v) {
                textw += 6;
            } else {
                textw = 0;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(v, -6 - textw, 0);
            graphics.pose().scale(0.5f, 0.5f, 0);
            graphics.drawString(Minecraft.getInstance().font, toRender, 0, 0, 0xff5555ff);
            graphics.pose().popPose();
            textReach = (int) (v + Minecraft.getInstance().font.width(toRender) * 0.5);
            v += rowSizing;
            if (i != spaces.length - 1) {
                for (int w = 1; w < vertical - 1; w++) {
                    int o = w % 2 - 1;
                    graphics.fill(v + o, w, v + 1 + o, w + 1, 0x55ff0000);
                }
            }
        }
    }

    @Override
    protected GridSettings constructSettings(BooleanSupplier willCauseReflow, Runnable reflowDetached) {
        return new GridSettings(willCauseReflow, reflowDetached, this);
    }

    @Override
    public void positionElements(Iterable<Tuple<Element, GridSettings>> elements) {
        int[] cols = positionAlongSpacing(
                elements,
                columns,
                getWidth(),
                e -> {
                    if (getSettings(e).isOptimalWidth()) {
                        return e.getOptimalWidth();
                    }
                    return getSettings(e).getWidth();
                },
                element -> getSettings(element).column,
                (element, x, width) -> {
                    GridSettings settings = getSettings(element);
                    if (!settings.isOptimalWidth()) {
                        width = settings.getWidth();
                    }
                    element.setX(getX() + x + settings.getX() - getXView());
                    element.setWidth(width + settings.getOffsetWidth());
                },
                xViewSize,
                !doConstrainX()
        );
        columnSizings = cols;
        rowSizings = positionAlongSpacing(
                elements,
                rows,
                getHeight(),
                e -> {
                    GridSettings settings = getSettings(e);
                    if (settings.isOptimalHeight()) {
                        return e.getOptimalHeight(cols[settings.column]);
                    }
                    return settings.getHeight();
                },
                element -> getSettings(element).row,
                (element, y, height) -> {
                    GridSettings settings = getSettings(element);
                    if (!settings.isOptimalHeight()) {
                        height = settings.getHeight();
                    }
                    element.setY(getY() + y + settings.getY() - getYView());
                    element.setHeight(height + settings.getOffsetHeight());
                },
                yViewSize,
                !doConstrainY()
        );
    }

    private int[] positionAlongSpacing(
            Iterable<Tuple<Element, GridSettings>> elements,
            List<Spacing> spacing,
            int availableSpace,
            Function<Element, Integer> optimalSizeGetter,
            Function<Element, Integer> positionGetter,
            TriConsumer<Element, Integer, Integer> positionSetter,
            Observers.WritableObserver<Integer> viewSetter,
            boolean onlyExpand
    ) {
        int[] sizings = computeSizing(elements, spacing, optimalSizeGetter, positionGetter);
        constrainToFitSpace(spacing, sizings, availableSpace, onlyExpand);
        viewSetter.set(Arrays.stream(sizings).sum());
        applySizingsToElements(elements, positionGetter, positionSetter, sizings);
        return sizings;
    }

    private void constrainToFitSpace(List<Spacing> spacing, int[] sizings, int availableSpace, boolean onlyExpand) {
        // first shrink or grow the percentages
        if (Arrays.stream(sizings).sum() > availableSpace && onlyExpand) {
            return;
        }

        int spaceTaken = 0;
        int totalSpace = 0;
        float totalPercentage = 0;

        for (int i = 0, spacingSize = spacing.size(); i < spacingSize; i++) {
            Spacing space = spacing.get(i);
            if (space.kind == SpacingKind.PERCENTAGE) {
                spaceTaken += sizings[i];
                totalPercentage += space.value;
            }
            totalSpace += sizings[i];
        }

        int availableSpaceOnlyPercentages = availableSpace - (totalSpace - spaceTaken);

        if (availableSpaceOnlyPercentages > 0) {
            for (int i = 0, spacingSize = spacing.size(); i < spacingSize; i++) {
                Spacing space = spacing.get(i);
                if (space.kind == SpacingKind.PERCENTAGE) {
                    sizings[i] = (int) (availableSpaceOnlyPercentages * (space.value / totalPercentage));
                }
            }
            return;
        }

        for (int i = 0, spacingSize = spacing.size(); i < spacingSize; i++) {
            Spacing space = spacing.get(i);
            if (space.kind == SpacingKind.PERCENTAGE) {
                sizings[i] = 0;
            }
        }

        totalSpace -= spaceTaken;

        // then the content ones

        for (int i = 0, spacingSize = spacing.size(); i < spacingSize && totalSpace > availableSpace; i++) {
            Spacing space = spacing.get(i);
            if (space.kind == SpacingKind.CONTENT) {
                int toShrink = Math.min(sizings[i], totalSpace - availableSpace);
                sizings[i] -= toShrink;
                totalSpace -= toShrink;
            }
        }

        // then the fixed ones

        for (int i = 0, spacingSize = spacing.size(); i < spacingSize && totalSpace < availableSpace; i++) {
            Spacing space = spacing.get(i);
            if (space.kind == SpacingKind.FIXED) {
                int toShrink = Math.min(sizings[i], totalSpace - availableSpace);
                sizings[i] -= toShrink;
                totalSpace -= toShrink;
            }
        }
    }

    @Override
    public int getOptimalWidth() {
        return Arrays.stream(computeSizing(flowElements(true), columns, e -> {
            if (getSettings(e).isOptimalWidth()) {
                return e.getOptimalWidth();
            }
            return getSettings(e).getWidth();
        }, element -> getSettings(element).column)).sum();
    }

    @Override
    public int getOptimalHeight(int width) {
        int[] cols = computeSizing(flowElements(true), columns, e -> {
            if (getSettings(e).isOptimalWidth()) {
                return e.getOptimalWidth();
            }
            return getSettings(e).getWidth();
        }, element -> getSettings(element).column);
        constrainToFitSpace(columns, cols, width, !doConstrainX());
        return Arrays.stream(computeSizing(flowElements(true), rows, e -> {
            GridSettings settings = getSettings(e);
            if (settings.isOptimalHeight()) {
                return e.getOptimalHeight(cols[settings.column]);
            }
            return settings.getHeight();
        }, element -> getSettings(element).row)).sum();
    }

    @Override
    protected boolean willCauseReflow(GridSettings settings) {
        return super.willCauseReflow(settings) && (columns.get(settings.getColumn()).kind != SpacingKind.FIXED || rows.get(settings.getRow()).kind != SpacingKind.FIXED);
    }

    public Grid addRow(float value, SpacingKind kind) {
        rows.add(new Spacing(kind, value));
        markLayoutDirty();
        return this;
    }

    public Grid addColumn(float value, SpacingKind kind) {
        columns.add(new Spacing(kind, value));
        markLayoutDirty();
        return this;
    }

    public enum SpacingKind {
        PERCENTAGE, FIXED, CONTENT
    }

    public static class GridSettings extends ParentElement.Settings {
        public int column;
        public int row;

        GridSettings(BooleanSupplier willCauseReflow, Runnable reflowDetached, ParentElement<?> owner) {
            super(willCauseReflow, reflowDetached, owner);
        }

        public int getColumn() {
            return column;
        }

        public GridSettings setColumn(int column) {
            mayDirtLayout(() -> this.column = column);
            return this;
        }

        public int getRow() {
            return row;
        }

        public GridSettings setRow(int row) {
            mayDirtLayout(() -> this.row = row);
            return this;
        }
    }

    record Spacing(SpacingKind kind, float value) {
    }
}
