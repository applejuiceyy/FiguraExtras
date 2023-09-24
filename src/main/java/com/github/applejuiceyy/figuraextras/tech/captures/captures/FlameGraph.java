package com.github.applejuiceyy.figuraextras.tech.captures.captures;

import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FlameGraph implements SecondaryCallHook {

    private final Globals globals;
    private final Consumer<Frame> after;
    private Frame currentFrame = new Frame(null);
    private int instructions = 0;

    public FlameGraph(Globals globals, Consumer<Frame> after) {
        this.globals = globals;
        this.after = after;
    }

    @Override
    public void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack) {
        flushInstructions();
        Frame newFrame = new Frame(currentFrame).setClosure(luaClosure);
        currentFrame.children.add(newFrame);
        currentFrame = newFrame;
    }

    @Override
    public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack) {
        flushInstructions();
        if (currentFrame.currentlyConstructingRegion != null) {
            currentFrame.regions.add(
                    new Region(
                            currentFrame.currentlyConstructingRegion.name,
                            currentFrame.currentlyConstructingRegion.instruction,
                            currentFrame.getInstructions() - currentFrame.currentlyConstructingRegion.instruction
                    )
            );
            currentFrame.currentlyConstructingRegion = null;
        }
        currentFrame = currentFrame.previous;
        assert currentFrame != null;
        currentFrame.invalidateCachedInstructions();
    }

    @Override
    public void lineAdvanced() {

    }

    @Override
    public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack) {
        // guaranteed instruction call before an intoFunction
        if (currentFrame.boundClosure == null) {
            currentFrame.setClosure(luaClosure);
        }
        instructions++;
    }

    @Override
    public void end() {
        after.accept(currentFrame);
    }

    @Override
    public void marker(String name) {
        int instruction = currentFrame.getInstructions() + instructions;
        currentFrame.markers.add(new Marker(name, instruction));
    }

    @Override
    public void region(String regionName) {
        int instruction = currentFrame.getInstructions() + instructions;
        if (currentFrame.currentlyConstructingRegion != null) {
            int start = currentFrame.currentlyConstructingRegion.instruction;
            currentFrame.regions.add(
                    new Region(
                            currentFrame.currentlyConstructingRegion.name,
                            currentFrame.currentlyConstructingRegion.instruction,
                            instruction - start
                    )
            );
        }
        currentFrame.currentlyConstructingRegion = null;
        if (regionName != null) {
            currentFrame.currentlyConstructingRegion = new Region(regionName, instruction, 0);
        }
    }

    private void flushInstructions() {
        if (instructions > 0) {
            Space space = new Space(instructions);
            currentFrame.children.add(space);
            instructions = 0;
            currentFrame.invalidateCachedInstructions();
        }
    }

    public static abstract class Child {
        public abstract int getInstructions();
    }

    public static class Space extends Child {

        private final int instructions;

        Space(int instructions) {
            this.instructions = instructions;
        }

        @Override
        public int getInstructions() {
            return instructions;
        }
    }

    public static class Frame extends Child {
        @Nullable
        final Frame previous;

        @Nullable
        public LuaClosure boundClosure;

        private int cachedInstructions = -1;

        private Region currentlyConstructingRegion = null;
        ArrayList<Child> children = new ArrayList<>();

        ArrayList<Marker> markers = new ArrayList<>();

        ArrayList<Region> regions = new ArrayList<>();

        Frame(@Nullable Frame previous) {
            this.previous = previous;
        }

        @Override
        public int getInstructions() {
            if (cachedInstructions != -1) {
                return cachedInstructions;
            }
            return cachedInstructions = children.stream().mapToInt(Child::getInstructions).sum();
        }

        protected void invalidateCachedInstructions() {
            cachedInstructions = -1;
        }

        public List<Child> getChildren() {
            return children;
        }

        public List<Marker> getMarkers() {
            return markers;
        }

        public List<Region> getRegions() {
            return regions;
        }

        Frame setClosure(LuaClosure closure) {
            boundClosure = closure;
            return this;
        }
    }

    public record Marker(String name, int instruction) {
    }

    public record Region(String name, int instruction, int duration) {
    }
}
