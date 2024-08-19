package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.components.MessageStackComponent;
import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class ChatLikeView implements Lifecycle {
    private final View.Context<Avatar> context;
    private final MessageStackComponent stack = new MessageStackComponent();

    private final List<FiguraLuaPrinterDuck.Kind> show = new ArrayList<>();
    private final Grid root;
    private final Flow scrollReceptacle;
    Runnable sub;

    public ChatLikeView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;
        Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> event = ((AvatarAccess) context.getValue()).figuraExtrass$getChatRedirect();
        if (!event.hasSubscribers() && (Configs.LOG_OTHERS.value || FiguraMod.isLocal(context.getValue().owner))) {
            FiguraExtras.sendBrandedMessage("Redirecting output to informational screens");
        }

        root = new Grid();

        root
                .rows()
                .content()
                .percentage(1)
                .cols()
                .percentage(1);

        additionPoint.accept(root);

        // FlowLayout controls = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(10));
        // root.add(controls);

        // CheckboxComponent showPings = Components.checkbox(net.minecraft.network.chat.Component.literal("Show pings"));
        // CheckboxComponent showLogs = Components.checkbox(net.minecraft.network.chat.Component.literal("Show logs"));
        // CheckboxComponent showErrors = Components.checkbox(net.minecraft.network.chat.Component.literal("Show Errors"));
        // CheckboxComponent showOthers = Components.checkbox(net.minecraft.network.chat.Component.literal("Show others"));
        // showPings.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.PINGS, stack::refreshLines));
        // showLogs.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.PRINT, stack::refreshLines));
        // showErrors.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.ERRORS, stack::refreshLines));
        // showOthers.onChanged(hideOrShow(show, FiguraLuaPrinterDuck.Kind.OTHER, stack::refreshLines));
        // showPings.checked(true);
        // showLogs.checked(true);
        // showErrors.checked(true);
        // showOthers.checked(true);
        // controls.child(showPings);
        // controls.child(showLogs);
        // controls.child(showErrors);
        // controls.child(showOthers);

        this.scrollReceptacle = new Flow();
        root.add(Elements.withVerticalScroll(scrollReceptacle.addAnd(stack), true)).setRow(1);

        sub = event.getSource().subscribe((message, kind) -> {
            stack.addMessage(TextUtils.replaceTabs(message), () -> show.contains(kind) || show.isEmpty());
            return false;
        });

        scrollReceptacle.yViewSize.observe(new Consumer<>() {
            int prev = 0;

            @Override
            public void accept(Integer v) {
                if (v < scrollReceptacle.getHeight()) {
                    scrollReceptacle.yView.set(0);
                } else if (prev < scrollReceptacle.getHeight() || prev - scrollReceptacle.getYView() == scrollReceptacle.getHeight()) {
                    scrollReceptacle.yView.set(v - scrollReceptacle.getHeight());
                }
                prev = v;
            }
        });
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
    public void render() {

    }

    @Override
    public void dispose() {
        sub.run();
        Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> event = ((AvatarAccess) context.getValue()).figuraExtrass$getChatRedirect();
        if (!event.hasSubscribers() && (Configs.LOG_OTHERS.value || FiguraMod.isLocal(context.getValue().owner))) {
            FiguraExtras.sendBrandedMessage("No longer redirecting output to informational screens");
        }
    }
}
