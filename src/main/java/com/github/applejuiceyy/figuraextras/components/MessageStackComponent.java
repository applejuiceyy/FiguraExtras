package com.github.applejuiceyy.figuraextras.components;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

public class MessageStackComponent extends Element {
    private final ArrayList<FormattedCharSequence> lines = new ArrayList<>();
    private final ArrayList<Message> messages = new ArrayList<>();

    private int messageOffset = 0;
    private int previousWidth = 0;

    public MessageStackComponent() {

    }

    public void addMessage(Component message) {
        addMessage(message, () -> true);
    }

    public void addMessage(Component message, BooleanSupplier shouldShow) {
        if (shouldShow.getAsBoolean()) {
            List<FormattedCharSequence> newLines = ComponentRenderUtils.wrapComponents(message, width.get(), Minecraft.getInstance().font);
            if (!(messageOffset == 0)) {
                messageOffset += newLines.size();
            }
            lines.addAll(newLines);
        }
        messages.add(new Message(message, shouldShow));
        while (lines.size() > 100) {
            lines.remove(0);
        }
        while (messages.size() > 100) {
            messages.remove(0);
        }
        updateUpperBound();
        enqueueDirtySection(false, false);
        optimalSizeChanged();
    }

    public void refreshLines() {
        lines.clear();
        for (Message message : messages) {
            if (message.shouldAppear.getAsBoolean()) {
                List<FormattedCharSequence> newLines = ComponentRenderUtils.wrapComponents(message.content(), width.get(), Minecraft.getInstance().font);
                lines.addAll(newLines);
            }
        }
        while (lines.size() > 100) {
            lines.remove(0);
        }
        updateUpperBound();
        enqueueDirtySection(false, false);
        optimalSizeChanged();
    }

    private int getLineCapacity() {
        return (int) Math.ceil(height.get() / 9f);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int messageQuantity = this.lines.size();
        Font font = Minecraft.getInstance().font;
        if (messageQuantity == 0) {
            context.drawString(font, Component.literal("No messages yet"), x.get(), y.get(), 0xff777777);
            return;
        }

        if (previousWidth != width.get()) {
            previousWidth = width.get();
            refreshLines();
        }



        int yPos = height.get();

        for (int i = lines.size() - 1 - messageOffset; i >= 0; i--) {
            if (yPos < 0) {
                break;
            }
            FormattedCharSequence message = lines.get(i);
            yPos -= font.lineHeight;
            context.drawString(font, message, x.get(), y.get() + yPos, 0xffffffff);
        }
        if (getLineCapacity() < lines.size()) {
            int lowerHeight = height.get() - (int) (((messageOffset + getLineCapacity()) / (float) lines.size()) * height.get());
            int higherHeight = height.get() - (int) ((messageOffset / (float) lines.size()) * height.get());
            context.fill(x.get() + width.get() - 5, y.get() + higherHeight, x.get() + width.get(), y.get() + lowerHeight, 0xaa000000);
        }
    }

    public Style getClickedComponentStyleAt(double x, double y) {
        int i = (int) ((height.get() - (y - this.y.get()) - 1) / Minecraft.getInstance().font.lineHeight);
        if (i >= 0 && i < this.lines.size()) {
            FormattedCharSequence line = this.lines.get(this.lines.size() - 1 - i);

            return Minecraft.getInstance().font.getSplitter().componentStyleAtWidth(line, Mth.floor(x - this.x.get()));
        }
        return null;
    }

    private void updateUpperBound() {
        if (messageOffset > lines.size() - getLineCapacity()) {
            messageOffset = lines.size() - getLineCapacity();
        }
        if (messageOffset < 0) {
            messageOffset = 0;
        }
    }

    @Override
    protected void defaultMouseScrolledBehaviour(DefaultCancellableEvent.MousePositionAmountEvent event) {
        messageOffset += (int) event.amount;
        updateUpperBound();
    }

    @Override
    protected void defaultToolTipBehaviour(DefaultCancellableEvent.ToolTipEvent event) {
        // TODO: tooltips
        /*context.renderComponentHoverEffect(
            Minecraft.getInstance().font,
            getClickedComponentStyleAt(event.x, event.y),
                event.x, event.y
        );*/
    }

    @Override
    public int computeOptimalWidth() {
        int ret = 0;
        for (Message message : messages) {
            if (message.shouldAppear.getAsBoolean()) {
                ret = Math.max(
                        Minecraft.getInstance().font
                                .split(message.content(), Integer.MAX_VALUE)
                                .stream()
                                .map(o -> Minecraft.getInstance().font.width(o))
                                .max(Comparator.comparingInt(e -> e))
                                .orElse(0), ret
                );
            }
        }
        return ret;
    }

    @Override
    public int computeOptimalHeight(int width) {
        int ret = 0;
        for (Message message : messages) {
            if (message.shouldAppear.getAsBoolean()) {
                List<FormattedCharSequence> newLines = ComponentRenderUtils.wrapComponents(message.content(), width, Minecraft.getInstance().font);
                ret += newLines.size() * 9;
            }
        }
        return Math.max(ret, 9);
    }

    record Message(Component content, BooleanSupplier shouldAppear) {
    }
}
