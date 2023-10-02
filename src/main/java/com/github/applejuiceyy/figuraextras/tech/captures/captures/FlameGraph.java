package com.github.applejuiceyy.figuraextras.tech.captures.captures;

import com.github.applejuiceyy.figuraextras.ducks.statics.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import com.github.applejuiceyy.figuraextras.mixin.figura.printer.FiguraLuaPrinterAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.utils.TextUtils;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FlameGraph implements SecondaryCallHook {
    private final Consumer<Frame> after;
    private final LuaTypeManager manager;
    private Frame currentFrame = new Frame(null, LuaDuck.CallType.NORMAL, null, null, null);
    private int instructions = 0;

    public FlameGraph(LuaTypeManager manager, Consumer<Frame> after) {
        this.after = after;
        this.manager = manager;
    }

    @Override
    public void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.CallType type, String possibleName) {
        flushInstructions();
        MutableComponent text = Component.empty();
        FiguraLuaPrinterDuck.skipUserdataStuff = true;
        boolean doVarargs = false;
        if (luaClosure.p.numparams == 0) {
            if (varargs.narg() > 0) {
                if (luaClosure.p.is_vararg == 0) {
                    text.append("Even though this function has no arguments, these values were supplied");
                }
                doVarargs = true;
            } else {
                text.append("no call arguments");
            }
        } else {
            text.append("call arguments:\n");
            for (int i = 0; i < luaClosure.p.numparams; i++) {
                Component component =
                        FiguraLuaPrinterAccessor.invokeTableToText(manager, stack[i], 1, 2, false);

                text.append("\t").append(component);
                if (i + 1 < luaClosure.p.numparams) {
                    text.append("\n");
                }
            }
            if (varargs.narg() > 0) {
                text.append("\n");
                if (luaClosure.p.is_vararg == 0) {
                    text.append("Even though this function is not a vararg, these values were also supplied");
                }
                doVarargs = true;
            }
        }
        if (doVarargs) {
            for (int i = 0; i < varargs.narg(); i++) {
                Component component =
                        FiguraLuaPrinterAccessor.invokeTableToText(manager, varargs.arg(i + 1), 1, 2, false);

                text.append("\t").append(component);
                if (i + 1 < luaClosure.p.numparams) {
                    text.append("\n");
                }
            }
        }

        FiguraLuaPrinterDuck.skipUserdataStuff = false;
        Frame newFrame = new Frame(currentFrame, type, possibleName, luaClosure, TextUtils.replaceTabs(text));
        currentFrame.children.add(newFrame);
        currentFrame = newFrame;
    }

    @Override
    public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.ReturnType type) {
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
        currentFrame.returnType = type;
        currentFrame = currentFrame.previous;
        assert currentFrame != null;
        currentFrame.invalidateCachedInstructions();
    }

    @Override
    public void lineAdvanced() {

    }

    @Override
    public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack) {
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
        public final LuaDuck.CallType type;

        public final @Nullable String possibleName;
        public final Component argumentComponent;

        @Nullable
        public final LuaClosure boundClosure;
        private LuaDuck.ReturnType returnType = LuaDuck.ReturnType.NORMAL;

        private int cachedInstructions = -1;

        private Region currentlyConstructingRegion = null;
        ArrayList<Child> children = new ArrayList<>();

        ArrayList<Marker> markers = new ArrayList<>();

        ArrayList<Region> regions = new ArrayList<>();

        Frame(@Nullable Frame previous, LuaDuck.CallType type, @Nullable String possibleName, @Nullable LuaClosure luaClosure, Component argumentComponent) {
            this.possibleName = possibleName;
            this.type = type;
            this.previous = previous;
            this.argumentComponent = argumentComponent;
            this.boundClosure = luaClosure;
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

        public LuaDuck.ReturnType getReturnType() {
            return returnType;
        }
    }

    public record Marker(String name, int instruction) {
    }

    public record Region(String name, int instruction, int duration) {
    }
}
