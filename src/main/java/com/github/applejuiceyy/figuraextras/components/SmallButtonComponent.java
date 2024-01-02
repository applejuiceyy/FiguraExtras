package com.github.applejuiceyy.figuraextras.components;

import com.github.applejuiceyy.figuraextras.screen.Blocker;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.utils.TextUtils;

public class SmallButtonComponent extends BaseComponent implements Blocker {
    private Component text;
    private int color;

    public SmallButtonComponent() {
        this(Component.empty().withStyle());
    }

    public SmallButtonComponent(Component text) {
        this(text, 0xaaff0000);
    }

    public SmallButtonComponent(Component text, int color) {
        this.text = TextUtils.trim(text);
        this.color = color;
        cursorStyle(CursorStyle.HAND);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        context.fill(x, y, x + width, y + height, 0, color);
        Font font = Minecraft.getInstance().font;
        context.drawWordWrap(font, text, x + width / 2 - font.width(text) / 2, y + height / 2 - font.wordWrapHeight(text, width) / 2, width, 0xffffffff);
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        Font font = Minecraft.getInstance().font;
        return font.width(text);
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        Font font = Minecraft.getInstance().font;
        return font.wordWrapHeight(text, 999);
    }

    @Override
    public boolean shouldBlock(double mouseX, double mouseY) {
        return true;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setText(Component text) {
        this.text = TextUtils.trim(text);
        notifyParentIfMounted();
    }
}
