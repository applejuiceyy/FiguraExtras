package com.github.applejuiceyy.figuraextras.screen.contentpopout;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ContentPopOut {

    private final ArrayList<PopOutInstance<?>> instances = new ArrayList<>();
    private final Supplier<ContentPopOutHost> host;


    public ContentPopOut(Supplier<ContentPopOutHost> host) {
        this.host = host;
    }

    public <T extends Element> PopOutInstance<T> createPopOut(T component, Observers.Observer<Component> value) {
        PopOutInstance<T> o = new PopOutInstance<>(component, value);
        instances.add(o);
        return o;
    }

    public void render() {
        for (PopOutInstance<?> instance : instances) {
            instance.render();
        }
    }

    public class PopOutInstance<T extends Element> {

        private final Runnable subscribe;
        T component;

        PopOutInstance(T component, Observers.Observer<Component> value) {

            this.component = component;

            subscribe = component.mouseDown.subscribe(event -> {
                if (event.button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    host.get().add(value);
                    event.cancelPropagation();
                    event.cancelDefault();
                }
            });
        }

        public void render() {

        }

        public void remove() {
            subscribe.run();
        }
    }


}
