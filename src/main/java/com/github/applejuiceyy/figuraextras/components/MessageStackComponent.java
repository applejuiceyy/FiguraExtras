package com.github.applejuiceyy.figuraextras.components;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
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
    }

    private int getLineCapacity() {
        return (int) Math.ceil(height.get() / 9f);
    }

    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        int messageQuantity = this.lines.size();
        if (messageQuantity == 0) {
            return;
        }

        if (previousWidth != width.get()) {
            previousWidth = width.get();
            refreshLines();
        }

        Font font = Minecraft.getInstance().font;

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

    // TODO: tooltips
    public void drawTooltip(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        //super.drawTooltip(context, mouseX, mouseY, partialTicks, delta);
        //if (this.isInBoundingBox(mouseX, mouseY)) {
        //    context.renderComponentHoverEffect(
        //             Minecraft.getInstance().font,
        //             getClickedComponentStyleAt(mouseX, mouseY),
        //             mouseX, mouseY
        //     );
        // }
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

    // TODO: correct optimal sizes
    @Override
    public int computeOptimalWidth() {
        return 0;
    }

    @Override
    public int computeOptimalHeight(int width) {
        return 0;
    }

    record Message(Component content, BooleanSupplier shouldAppear) {
    }
}
