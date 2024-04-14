package com.github.applejuiceyy.figuraextras.vscode.ipc;

import com.github.applejuiceyy.figuraextras.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;

public abstract class IPCFactory {
    static public IPCFactory getIPCFactory() {
        if (net.minecraft.Util.getPlatform() == net.minecraft.Util.OS.WINDOWS) {
            return new WindowsIPCFactory();
        }
        throw new UnsupportedOperationException();
    }

    public abstract boolean exists(String path);

    public abstract IPC createServer(String path);

    public abstract net.minecraft.util.Tuple<InputStream, OutputStream> connectAsClient(String path) throws IOException;

    public static abstract class IPC {

        private net.minecraft.util.Tuple<InputStream, OutputStream> connect;

        public net.minecraft.util.Tuple<InputStream, OutputStream> connect() throws IOException {
            return connect(false);
        }

        public abstract net.minecraft.util.Tuple<InputStream, OutputStream> connect(boolean awaitIfFull) throws IOException;

        public void continuousConnect(BiConsumer<InputStream, OutputStream> consumer) {
            Util.thread(() -> {
                try {
                    while (true) {
                        connect = connect(true);
                        consumer.accept(connect.getA(), connect.getB());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
