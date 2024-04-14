package com.github.applejuiceyy.figuraextras.vscode.dsp;

import java.util.function.IntSupplier;

public class IdGenerator implements IntSupplier {
    private int next = 0;

    @Override
    public int getAsInt() {
        return next++;
    }
}
