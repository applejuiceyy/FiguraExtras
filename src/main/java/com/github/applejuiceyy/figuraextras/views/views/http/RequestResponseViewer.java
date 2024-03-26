package com.github.applejuiceyy.figuraextras.views.views.http;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RequestResponseViewer implements InfoViews.View {
    public final HttpRequest request;
    FlowLayout root;
    FlowLayout tabs;

    Component viewRoot;

    InfoViews.View currentView;

    public RequestResponseViewer(Optional<String> requestBody, CompletableFuture<String> bodyBytes, HttpRequest request, CompletableFuture<HttpResponse<InputStream>> future) {
        tabs = Containers.horizontalFlow(Sizing.content(), Sizing.fill(100));
        this.request = request;

        root = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(90));
        root.child(Containers.horizontalScroll(Sizing.fill(100), Sizing.fill(10), tabs));

        addTab(Components.button(net.minecraft.network.chat.Component.literal("Request Headers"), o -> changeTo(new HeaderViewer(request.headers()))));
        addTab(Components.button(
                        net.minecraft.network.chat.Component.literal("Response Headers"), o -> changeTo(
                                InfoViews.<Void>onlyIf(null, p -> future.isDone(),
                                        n -> InfoViews.<Void>onlyIf(
                                                null,
                                                p -> !future.isCompletedExceptionally(),
                                                p -> {
                                                    try {
                                                        return new HeaderViewer(future.get().headers());
                                                    } catch (InterruptedException | ExecutionException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                },
                                                "No useful information as it errored before the html stage"),
                                        "Still Fetching")
                        )
                )
        );

        addTab(Components.button(net.minecraft.network.chat.Component.literal("Request Body"), o -> changeTo(
                InfoViews.<Void>onlyIf(null, p -> requestBody.isPresent(), p -> {
                    //noinspection OptionalGetWithoutIsPresent
                    return new BodyViewer(requestBody.get());
                }, net.minecraft.network.chat.Component.literal("Empty")))));

        addTab(Components.button(net.minecraft.network.chat.Component.literal("Response Body"), o -> changeTo(
                InfoViews.<Void>onlyIf(null, p -> !future.isCompletedExceptionally(), n -> InfoViews.<Void>onlyIf(
                                null,
                                p -> bodyBytes.isDone(),
                                p -> {
                                    try {
                                        return new BodyViewer(bodyBytes.get());
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, "Still Fetching"
                        ),
                        "No useful information as it errored before the html stage"))));

    }

    void addTab(Component button) {
        tabs.child(button);
    }

    void changeTo(InfoViews.View content) {
        if (currentView != null) {
            currentView.dispose();
            viewRoot.remove();
            currentView = null;
            viewRoot = null;
        }
        if (content != null) {
            currentView = content;
            // TODO
            // viewRoot = content.getRoot();
            viewRoot.sizing(Sizing.fill(100), Sizing.fill(90));
            root.child(viewRoot);
        }
    }

    @Override
    public void tick() {
        if (currentView != null)
            currentView.tick();
    }

    @Override
    public void render() {
        if (currentView != null)
            currentView.render();
    }

    @Override
    public void dispose() {
        if (currentView != null)
            currentView.dispose();
    }

    @Override
    public Element getRoot() {
        // TODO
        return new Grid();
    }
}
