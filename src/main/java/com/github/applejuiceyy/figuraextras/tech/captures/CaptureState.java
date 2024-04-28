package com.github.applejuiceyy.figuraextras.tech.captures;

import com.github.applejuiceyy.figuraextras.util.Event;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.Globals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class CaptureState {
    private final Event<Hook> event = Event.interfacing(Hook.class);
    private final HashMap<Object, PossibleCapture> availableSingularCaptures = new HashMap<>();
    private final Globals globals;
    ArrayList<ActiveOpportunity<?>> currentWaitingCaptures = new ArrayList<>();

    public CaptureState(Globals globals) {
        this.globals = globals;
    }

    public Event<Hook> getEvent() {
        return event;
    }

    public @Nullable Hook getSink() {
        return isListening() ? event.getNullableSink() : null;
    }

    public boolean isListening() {
        return !event.isActive();
    }

    public void startEvent(Object toRun) {
        if (!isListening()) {
            return;
        }

        PossibleCapture current;

        if (!availableSingularCaptures.containsKey(toRun)) {
            current = new PossibleCapture();
            current.name = toRun.toString();
            availableSingularCaptures.put(toRun, current);
        } else {
            current = availableSingularCaptures.get(toRun);
        }

        current.mostRecentCallMillis = System.currentTimeMillis();

        for (Iterator<ActiveOpportunity<?>> iterator = currentWaitingCaptures.iterator(); iterator.hasNext(); ) {
            ActiveOpportunity<?> activeOpportunity = iterator.next();
            PossibleCapture opportunity = activeOpportunity.opportunity();

            if (opportunity == current) {
                singularCapture(activeOpportunity.thing());
                iterator.remove();
            }
        }
    }

    public void singularCapture(Hook in) {
        Runnable[] runnables = new Runnable[1];
        runnables[0] = event.subscribe(new SingularCapture(in, () -> runnables[0].run()));
    }

    public void queueSingularCapture(ActiveOpportunity<Hook> hookActiveOpportunity) {
        currentWaitingCaptures.add(hookActiveOpportunity);
    }

    public HashMap<Object, PossibleCapture> getAvailableSingularCaptures() {
        return availableSingularCaptures;
    }
}
