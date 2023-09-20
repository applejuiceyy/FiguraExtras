package com.github.applejuiceyy.figuraextras.screen.contentpopout;

import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.util.Observers;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.VanillaWidgetComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.event.MouseDown;
import io.wispforest.owo.ui.event.MouseEnter;
import io.wispforest.owo.ui.event.MouseLeave;
import io.wispforest.owo.util.EventSource;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class ContentPopOut {
    private final FlowLayout root;
    private final ArrayList<PopOutInstance<?>> instances = new ArrayList<>();
    private final ContentPopOutHost host;


    public ContentPopOut(FlowLayout root, ContentPopOutHost host) {
        this.root = root;
        this.host = host;
    }

    public <T extends BaseComponent> PopOutInstance<T> createPopOut(T component, Observers.Observer<Component> value) {
        PopOutInstance<T> o = new PopOutInstance<>(component, value);
        instances.add(o);
        return o;
    }

    public <T extends BaseComponent> PopOutInstance<VanillaWidgetComponent> createPopOut(AbstractWidget component, Observers.Observer<Component> value) {
        PopOutInstance<VanillaWidgetComponent> o = new PopOutInstance<>(Components.wrapVanillaWidget(component), value);
        instances.add(o);
        return o;
    }

    public void render() {
        for (PopOutInstance<?> instance : instances) {
            instance.render();
        }
    }

    public class PopOutInstance<T extends BaseComponent> {
        boolean showingButton = false;
        boolean shouldShowButton = false;
        SmallButtonComponent popper;
        T component;

        EventSource<MouseEnter>.Subscription enterSub;
        EventSource<MouseLeave>.Subscription leaveSub;
        EventSource<MouseEnter>.Subscription enterSub2;
        EventSource<MouseLeave>.Subscription leaveSub2;
        EventSource<MouseDown>.Subscription clickSub;


        PopOutInstance(T component, Observers.Observer<Component> value) {
            this.component = component;

            popper = new SmallButtonComponent();
            popper.sizing(Sizing.fixed(10), Sizing.fixed(10));

            clickSub = popper.mouseDown().subscribe((x, y, button) -> {
                host.add(value);
                shouldShowButton = false;
                return true;
            });

            enterSub = component.mouseEnter().subscribe(() -> shouldShowButton = true);

            leaveSub = component.mouseLeave().subscribe(() -> shouldShowButton = false);

            enterSub2 = popper.mouseEnter().subscribe(() -> shouldShowButton = true);

            leaveSub2 = popper.mouseLeave().subscribe(() -> shouldShowButton = false);
        }

        public void render() {
            if (showingButton != shouldShowButton) {
                if (shouldShowButton) {
                    root.child(popper);
                } else {
                    root.removeChild(popper);
                }
                showingButton = shouldShowButton;
            }
            popper.positioning(
                    Positioning.absolute(component.x() - 10, component.y())
            );
        }

        public void remove() {
            if (showingButton) {
                showingButton = false;
                root.removeChild(popper);
            }
            clickSub.cancel();
            enterSub.cancel();
            clickSub.cancel();
            enterSub2.cancel();
            leaveSub2.cancel();
        }
    }


}
