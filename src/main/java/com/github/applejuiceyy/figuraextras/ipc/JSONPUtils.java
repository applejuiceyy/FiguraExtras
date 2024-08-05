package com.github.applejuiceyy.figuraextras.ipc;

import com.github.applejuiceyy.figuraextras.util.Util;
import com.google.gson.GsonBuilder;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class JSONPUtils {


    public static <T> CompletableFuture<T> listenForCompletion(Future<T> fut) {
        CompletableFuture<T> cfut = new CompletableFuture<>();
        cfut.completeAsync(() -> {
            try {
                return fut.get();
            } catch (InterruptedException | ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof RuntimeException r ? r : new RuntimeException(cause);
            }
        });
        return cfut;
    }

    public static <T> T configureLauncher(Object local, Class<? extends T> remote, InputStream i, OutputStream o, Consumer<T> backwardsSetter) {
        Launcher<T> launcher = new Launcher.Builder<T>()
                .setLocalService(local)
                .setRemoteInterface(remote)
                .setInput(i)
                .setOutput(o)
                .configureGson(GsonBuilder::serializeNulls)
                .create();

        T remoteProxy = launcher.getRemoteProxy();
        backwardsSetter.accept(remoteProxy);
        startWithTermination(local, i, o, launcher, () -> {
        });
        return launcher.getRemoteProxy();
    }

    public static <T> void startWithTermination(Object local, InputStream i, OutputStream o, Launcher<T> launcher, Runnable closer) {
        listenForCompletion(launcher.startListening()).handle((a, b) -> {
            if (local instanceof DisconnectAware da) {
                da.onDisconnect();
            }
            Util.closeMultiple(i, o, closer::run);
            return null;
        });
    }
}
