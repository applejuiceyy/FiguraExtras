package com.github.applejuiceyy.figuraextras.views.views.http;

import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.utils.ColorUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;

public class NetworkView implements InfoViews.View {
    FlowLayout content = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
    ScrollContainer<FlowLayout> logging = Containers.verticalScroll(Sizing.content(), Sizing.fill(100), content);

    FlowLayout root = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));

    RequestResponseViewer viewer;
    Runnable unsub;

    public NetworkView(InfoViews.Context context) {
        logging.scrollbar(ScrollContainer.Scrollbar.vanilla());
        root.child(logging);

        Style dismissed = Style.EMPTY.withColor(0xaaaaaa);
        Style host = Style.EMPTY.withColor(0xffffff);
        Style keys = Style.EMPTY.withColor(0x7799aa);
        Style values = Style.EMPTY.withColor(0x77aa99);
        Style fragment = Style.EMPTY.withColor(0x229922);

        unsub = ((AvatarAccess) context.getAvatar()).figuraExtrass$getNetworkLogger().getSource().subscribe((future, request, bodyBytes) -> {
            URI uri = request.uri();

            net.minecraft.network.chat.MutableComponent component = net.minecraft.network.chat.Component.literal("HTTP ");
            component.append(withColor(request.method(), ColorUtils.Colors.SOFT_BLUE.style));
            component.append(withColor(" -> ", dismissed));

            if (uri.getScheme() != null) {
                component.append(withColor(uri.getScheme(), dismissed));
                component.append(withColor("://", dismissed));
            }

            if (uri.getHost() != null)
                component.append(withColor(uri.getHost(), host));

            if (uri.getRawPath() != null)
                component.append(withColor(uri.getRawPath(), dismissed));

            if (uri.getRawQuery() != null) {
                final String[] pairs = uri.getRawQuery().split("&");
                boolean firstTime = true;
                for (String pair : pairs) {
                    final int idx = pair.indexOf("=");

                    String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
                    String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : null;

                    component.append(withColor(firstTime ? "?" : "&", dismissed));
                    firstTime = false;
                    component.append(withColor(key, keys));

                    if (value != null) {
                        component.append(withColor("=", dismissed));
                        component.append(withColor(value, values));
                    }
                }
            }

            if (uri.getRawFragment() != null) {
                component.append(withColor("#" + uri.getRawFragment(), fragment));
            }

            MutableComponent copy = component.copy();

            component.append(withColor(" [ONGOING]", dismissed));

            SmallButtonComponent c = new SmallButtonComponent(component, 0x00000000);

            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    copy.append(withColor(" [ERROR]", ColorUtils.Colors.LUA_ERROR.style));
                } else {
                    copy.append(withColor(" [" + response.statusCode() + "]", Style.EMPTY.withColor(
                            switch (Integer.toString(response.statusCode()).charAt(0)) {
                                case '3' -> 0xccaa22;
                                case '4', '5' -> ColorUtils.Colors.LUA_ERROR.hex;
                                default -> ColorUtils.Colors.PURPLE.hex;
                            }
                    )));
                }
                c.setText(copy);
            });

            c.mouseDown().subscribe((x, y, m) -> {
                logging.verticalSizing(Sizing.fill(10));
                if (viewer != null) {
                    viewer.root.remove();
                    if (viewer.request == request) {
                        logging.verticalSizing(Sizing.fill(100));
                    }
                    viewer = null;
                }

                Optional<String> requestBody = request.bodyPublisher().map(p -> {
                    var bodySubscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
                    var flowSubscriber = new StringSubscriber(bodySubscriber);
                    p.subscribe(flowSubscriber);
                    return bodySubscriber.getBody().toCompletableFuture().join();
                });
                viewer = new RequestResponseViewer(requestBody, bodyBytes, request, future);
                root.child(viewer.root);
                return true;
            });
            content.child(c);
        });
    }

    public MutableComponent withColor(String text, Style style) {
        return net.minecraft.network.chat.Component.literal(text).withStyle(style);
    }

    @Override
    public void tick() {
        if (viewer != null)
            viewer.tick();
    }

    @Override
    public Component getRoot() {
        return root;
    }

    @Override
    public void render() {
        if (viewer != null)
            viewer.render();
    }

    @Override
    public void dispose() {
        unsub.run();
        if (viewer != null)
            viewer.dispose();
    }

    record StringSubscriber(HttpResponse.BodySubscriber<?> wrapped) implements Flow.Subscriber<ByteBuffer> {
        // what kind of joke is this
        // https://stackoverflow.com/questions/55816226/how-to-read-the-body-of-a-httprequest-in-java-11
        // this is why java has gotten the reputation of being a hard language
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            wrapped.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer item) {
            wrapped.onNext(List.of(item));
        }

        @Override
        public void onError(Throwable throwable) {
            wrapped.onError(throwable);
        }

        @Override
        public void onComplete() {
            wrapped.onComplete();
        }
    }
}
