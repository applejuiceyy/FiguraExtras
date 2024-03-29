package com.github.applejuiceyy.figuraextras.views.views.http;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RequestResponseViewer implements Lifecycle {
    public final HttpRequest request;
    Grid root;
    Grid tabs;

    ParentElement.AdditionPoint additionPoint;
    Lifecycle currentView;

    public RequestResponseViewer(Optional<String> requestBody, ParentElement.AdditionPoint a, CompletableFuture<String> bodyBytes, HttpRequest request, CompletableFuture<HttpResponse<InputStream>> future) {
        tabs = new Grid();
        this.request = request;

        tabs.rows().content(); // columns added later on

        root = new Grid();
        root
                .rows()
                .content()
                .content()
                .percentage(1)
                .cols()
                .percentage(1);

        a.accept(root);

        root.add(tabs);
        Scrollbar scrollbar = new Scrollbar();
        scrollbar.setHorizontal(true);
        root.add(scrollbar).setRow(1).setOptimalHeight(false).setHeight(5);
        Elements.makeHorizontalContainerScrollable(tabs, scrollbar, true);

        additionPoint = root.adder(g -> g.setRow(2));

        addTab("Request Headers", (o, ap) -> new HeaderViewer(request.headers(), ap));
        addTab("Response Headers", InfoViews.voiding()
                .predicate(future::isDone)
                .ifFalse("Still Fetching")
                .ifTrue(
                        InfoViews.voiding()
                                .predicate(() -> !future.isCompletedExceptionally())
                                .ifFalse("No useful information as it failed before the html stage")
                                .ifTrue(ap -> {
                                    try {
                                        return new HeaderViewer(future.get().headers(), ap);
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                )
        );

        //noinspection OptionalGetWithoutIsPresent
        addTab("Request Body", InfoViews.voiding()
                .predicate(requestBody::isPresent)
                .ifFalse("Empty")
                .ifTrue(ap -> new BodyViewer(requestBody.get(), ap))
        );

        addTab("Response Body", InfoViews.voiding()
                .predicate(bodyBytes::isDone)
                .ifFalse("Still Fetching")
                .ifTrue(
                        InfoViews.voiding()
                                .predicate(() -> !bodyBytes.isCompletedExceptionally())
                                .ifFalse("No useful information as it failed before the html stage")
                                .ifTrue(ap -> {
                                    try {
                                        return new BodyViewer(bodyBytes.get(), ap);
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                )
        );
    }

    void addTab(String text, InfoViews.ViewConstructor<Void, ? extends Lifecycle> supplier) {
        int column = tabs.columnCount();
        tabs.addColumn(0, Grid.SpacingKind.CONTENT);
        Button button = Button.vanilla();
        button.add(text);
        button.activation.subscribe(e -> changeTo(supplier));
        tabs.add(button).setColumn(column);
    }

    void changeTo(InfoViews.ViewConstructor<Void, ? extends Lifecycle> content) {
        if (currentView != null) {
            currentView.dispose();
            additionPoint.remove();
            currentView = null;
        }
        if (content != null) {
            currentView = content.apply(null, additionPoint);
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
}
