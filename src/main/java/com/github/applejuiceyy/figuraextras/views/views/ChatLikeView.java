package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.MessageStackComponent;
import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatLikeView implements InfoViews.View {
    private final InfoViews.Context context;
    private final MessageStackComponent stack = new MessageStackComponent();

    private final FlowLayout root;

    private final List<FiguraLuaPrinterDuck.Kind> show = new ArrayList<>();
    Runnable sub;

    public ChatLikeView(InfoViews.Context context) {
        this.context = context;
        Event<BiConsumer<net.minecraft.network.chat.Component, FiguraLuaPrinterDuck.Kind>> event = ((AvatarAccess) context.getAvatar()).figuraExtrass$getChatRedirect();
        if (!event.hasSubscribers() && (Configs.LOG_OTHERS.value || FiguraMod.isLocal(context.getAvatar().owner))) {
            FiguraMod.sendChatMessage(
                    net.minecraft.network.chat.Component.literal("[FiguraExtras] ")
                            .withStyle(ChatFormatting.BLUE)
                            .append(net.minecraft.network.chat.Component.literal("Redirecting output to informational screens").withStyle(ChatFormatting.WHITE))
            );
        }

        root = Containers.verticalFlow(Sizing.fill(100), Sizing.content(100));

        FlowLayout controls = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(10));
        root.child(controls);

        CheckboxComponent showPings = Components.checkbox(net.minecraft.network.chat.Component.literal("Show pings"));
        CheckboxComponent showLogs = Components.checkbox(net.minecraft.network.chat.Component.literal("Show logs"));
        CheckboxComponent showErrors = Components.checkbox(net.minecraft.network.chat.Component.literal("Show Errors"));
        CheckboxComponent showOthers = Components.checkbox(net.minecraft.network.chat.Component.literal("Show others"));
        showPings.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.PINGS, stack::refreshLines));
        showLogs.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.PRINT, stack::refreshLines));
        showErrors.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.ERRORS, stack::refreshLines));
        showOthers.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.OTHER, stack::refreshLines));
        showPings.checked(true);
        showLogs.checked(true);
        showErrors.checked(true);
        showOthers.checked(true);
        controls.child(showPings);
        controls.child(showLogs);
        controls.child(showErrors);
        controls.child(showOthers);

        stack.sizing(Sizing.fill(100), Sizing.fill(90));

        root.child(stack);

        sub = event.getSource().subscribe((message, kind) -> stack.addMessage(message, () -> show.contains(kind)));
    }

    private <T> Consumer<Boolean> hideOrShow(List<T> list, T value, Runnable after) {
        return p -> {
            if (p != list.contains(value)) {
                if (p) {
                    list.add(value);
                } else {
                    list.remove(value);
                }
                after.run();
            }
        };
    }

    @Override
    public void tick() {

    }

    @Override
    public Component getRoot() {
        return root;
    }

    @Override
    public void render() {

    }

    @Override
    public void dispose() {
        sub.run();
        Event<BiConsumer<net.minecraft.network.chat.Component, FiguraLuaPrinterDuck.Kind>> event = ((AvatarAccess) context.getAvatar()).figuraExtrass$getChatRedirect();
        if (!event.hasSubscribers() && (Configs.LOG_OTHERS.value || FiguraMod.isLocal(context.getAvatar().owner))) {
            FiguraMod.sendChatMessage(
                    net.minecraft.network.chat.Component.literal("[FiguraExtras] ")
                            .withStyle(ChatFormatting.BLUE)
                            .append(net.minecraft.network.chat.Component.literal("No longer redirecting output to informational screens").withStyle(ChatFormatting.WHITE))
            );
        }
    }
}
