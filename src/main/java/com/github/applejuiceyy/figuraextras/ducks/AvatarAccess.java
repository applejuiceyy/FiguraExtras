package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.ducks.statics.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.dummy.DummyExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.util.TriConsumer;
import org.luaj.vm2.LuaValue;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

public interface AvatarAccess {
    boolean figuraExtras$isCleaned();

    Expander<LuaValue> figuraExtras$getObjectViewTree();

    Expander<DummyExpander.Dummy> figuraExtras$getModelViewTree();

    Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> figuraExtras$getChatRedirect();

    Event<TriConsumer<CompletableFuture<HttpResponse<InputStream>>, HttpRequest, CompletableFuture<String>>> figuraExtras$getNetworkLogger();

    CompoundTag figuraExtras$getGuestNbt();

    int figuraExtras$getGuestFileSize();

    void figuraExtras$setGuestNbt(CompoundTag tag);
}
