package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.views.views.http.SplittingSubscription;
import org.figuramc.figura.lua.api.data.FiguraFuture;
import org.figuramc.figura.lua.api.net.HttpRequestsAPI;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

@Mixin(value = HttpRequestsAPI.HttpRequestBuilder.class, remap = false)
public class HttpRequestBuilderMixin {
    @Unique
    CompletableFuture<String> wireTapped = null;
    @Shadow
    @Final
    private HttpRequestsAPI parent;

    @ModifyArg(
            method = "send",
            at = @At(value = "INVOKE", target = "Ljava/net/http/HttpClient;sendAsync(Ljava/net/http/HttpRequest;Ljava/net/http/HttpResponse$BodyHandler;)Ljava/util/concurrent/CompletableFuture;"),
            index = 1

    )
    HttpResponse.BodyHandler<InputStream> e(HttpResponse.BodyHandler<InputStream> responseBodyHandler) {

        if (((AvatarAccess) ((NetworkingAPIAccessor) ((HttpRequestsAPIAccessor) parent).getParent()).getOwner())
                .figuraExtrass$getNetworkLogger().hasSubscribers()) {
            CompletableFuture<String> thisWrapped = new CompletableFuture<>();
            wireTapped = thisWrapped;
            HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
            return responseInfo -> {
                HttpResponse.BodySubscriber<String> wireTap = bodyHandler.apply(responseInfo);
                wireTap.getBody().whenComplete(((bytes, throwable) -> thisWrapped.complete(bytes)));
                return new HttpResponse.BodySubscriber<>() {
                    final HttpResponse.BodySubscriber<InputStream> wrapped = responseBodyHandler.apply(responseInfo);
                    final Flow.Subscriber<List<ByteBuffer>> fork = SplittingSubscription.bind(
                            thing -> thing.stream().map(ByteBuffer::duplicate).collect(Collectors.toList()),
                            wrapped, wireTap
                    );

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        fork.onSubscribe(subscription);
                    }

                    @Override
                    public void onNext(List<ByteBuffer> item) {
                        fork.onNext(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        fork.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        fork.onComplete();
                    }

                    @Override
                    public CompletionStage<InputStream> getBody() {
                        return wrapped.getBody();
                    }
                };
            };
        }
        return responseBodyHandler;
    }

    @Inject(
            method = "send",
            at = @At(value = "RETURN"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    void e(CallbackInfoReturnable<FiguraFuture<HttpRequestsAPI.HttpResponse>> cir, String uri, HttpRequest req, FiguraFuture<HttpRequestsAPI.HttpResponse> future, CompletableFuture<HttpResponse<InputStream>> asyncResponse) {
        ((AvatarAccess) ((NetworkingAPIAccessor) ((HttpRequestsAPIAccessor) parent).getParent()).getOwner())
                .figuraExtrass$getNetworkLogger()
                .getSink()
                .run(v -> v.accept(asyncResponse, req, wireTapped));
        wireTapped = null;
    }
}
