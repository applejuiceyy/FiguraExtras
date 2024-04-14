package com.github.applejuiceyy.figuraextras.vscode.dsp;

public interface SourceListener {
    void added(String prototype, String source, int name);

    void removed(String a, String b, int name);
}
