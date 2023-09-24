package com.github.applejuiceyy.figuraextras.tech.captures;

import java.util.ArrayList;

public class Capture {
    ArrayList<Action> capturedActions = new ArrayList<>();

    public void collect(Action action) {
        capturedActions.add(action);
    }
}
