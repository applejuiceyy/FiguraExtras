package com.github.applejuiceyy.figuraextras.tech.trees.lua;

import com.github.applejuiceyy.figuraextras.mixin.figura.printer.FiguraLuaPrinterAccessor;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.figuraextras.views.Hover;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.utils.TextUtils;
import org.luaj.vm2.LuaValue;

import java.util.Optional;
import java.util.function.Function;

public class LuaValueInterpreter implements ObjectInterpreter<LuaValue> {
    private final Avatar avatar;

    public LuaValueInterpreter(Avatar avatar) {
        this.avatar = avatar;
    }

    public static Object hash(LuaValue value) {
        return switch (value.type()) {
            case LuaValue.TFUNCTION, LuaValue.TTHREAD, LuaValue.TTABLE, LuaValue.TNIL -> value;
            case LuaValue.TSTRING -> value.toString();
            case LuaValue.TBOOLEAN -> value.checkboolean();
            case LuaValue.TNUMBER, LuaValue.TINT -> value.checknumber();
            case LuaValue.TUSERDATA -> value.checkuserdata();
            default -> value;
        };
    }

    @Override
    public Class<LuaValue> getObjectClass() {
        return LuaValue.class;
    }

    @Override
    public void populateHeader(Grid root, Observers.Observer<LuaValue> updater,
                               Observers.Observer<Optional<LuaValue>> freeRoamUpdater,
                               ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover, Event<Runnable>.Source ticker) {
        populateHeader(root, updater, freeRoamUpdater, objectViewChanger, popper, referenceConsumer, true);
    }

    public void populateHeader(Grid root, Observers.Observer<LuaValue> updater,
                               Observers.Observer<Optional<LuaValue>> freeRoamUpdater,
                               ViewChanger objectViewChanger, PopperConsumer popper,
                               CyclicReferenceConsumer referenceConsumer, boolean quoteStrings) {
        root.rows().percentage(1).cols().percentage(1);
        Button value = Button.minimal();
        arrangeFor(value, updater, freeRoamUpdater, objectViewChanger, popper, referenceConsumer, Function.identity(), quoteStrings);
        Button metatable = Button.minimal();
        arrangeFor(metatable,
                updater.derive(thing -> thing.getmetatable() == null ? LuaValue.NIL : thing.getmetatable()),
                freeRoamUpdater.derive(o -> o.map(thing -> thing.getmetatable() == null ? LuaValue.NIL : thing.getmetatable())),
                objectViewChanger, popper, referenceConsumer,
                in -> Component.literal("  metatable").append(in),
                quoteStrings
        );

        root.add(value);

        updater.observe(v -> {
            root.remove(metatable);
            if (v.type() == LuaValue.TTABLE && v.getmetatable() != null) {
                root.add(metatable);
            }
        });
    }

    void arrangeFor(Button button,
                    Observers.Observer<LuaValue> updater,
                    Observers.Observer<Optional<LuaValue>> freeRoamUpdater,
                    ViewChanger objectViewChanger, PopperConsumer popper,
                    CyclicReferenceConsumer referenceConsumer, Function<Component, Component> prefix, boolean quoteStrings) {
        popper.accept(button, freeRoamUpdater.derive(v -> v.map(p -> FiguraLuaPrinterAccessor.invokeGetPrintText(avatar.luaRuntime.typeManager, p, false, quoteStrings))
                .orElse(Component.literal("Not Found").withStyle(ChatFormatting.RED))));
        Object identity = new Object();
        button.activation.subscribe(event -> objectViewChanger.accept(freeRoamUpdater, identity));

        Hover.elementHoverObject(button, () -> hash(updater.get()));

        Observers.Observer<Component> observer = referenceConsumer.accept(updater.derive(value -> {
            if (value.istable() || value.isfunction() || value.isuserdata()) {
                return Optional.of(hash(value));
            }
            return Optional.empty();
        }));

        updater.merge(observer).observe(live -> {
            button.setText(TextUtils.replaceTabs(prefix.apply(Util.appendReference(avatar.luaRuntime.typeManager, live.getA(), live.getB(), quoteStrings))));
        });
    }

    /*
    @Override
    public Iterator<Tuple<LuaValue, Supplier<Optional<LuaValue>>>> iterator(LuaValue value) {
        if (value.istable()) {
            return new LuaValueIterator(value);
        }
        if (value.isfunction() && value instanceof LuaClosure closure) {
            return new ClosureIterator(closure);
        }
        return Collections.emptyIterator();
    }

    @Override
    public boolean compareValues(LuaValue one, LuaValue other) {
        return one.raweq(other);
    }

    @Override
    public boolean compareKeys(LuaValue one, LuaValue other) {
        return one.raweq(other);
    }

    @Override
    public Object hashKey(LuaValue luaValue) {
        return unwrapLuaValue(luaValue);
    }

    @Override
    public Object hashValue(LuaValue luaValue) {
        return unwrapLuaValue(luaValue);
    }

    @Override
    public boolean shouldDisplayCyclicReferencesKey(LuaValue obj) {
        return shouldDisplayCyclicReferences(obj);
    }

    @Override
    public boolean shouldDisplayCyclicReferencesValue(LuaValue obj) {
        return shouldDisplayCyclicReferences(obj);
    }

    @Override
    public void applyKeyStages(CyclicObjectTree.StageConsumer<LuaValue> consumer) {
        consumer.add(new GenericScrapingStage<>(avatar));
        consumer.add(this);
    }

    @Override
    public void applyValueStages(CyclicObjectTree.StageConsumer<LuaValue> consumer) {
        consumer.add(new GenericScrapingStage<>(avatar));
        consumer.add(this);
    }

    @Override
    public MutableComponent valueToComponent(LuaValue value) {
        return toComponent(value, true);
    }

    @Override
    public MutableComponent keyToComponent(LuaValue string) {
        return toComponent(string, false);
    }

    private MutableComponent toComponent(LuaValue obj, boolean quote) {
        MutableComponent text = FiguraLuaPrinterAccessor.invokeGetPrintText(avatar.luaRuntime.typeManager, obj, false, quote);

        if (text == null) {
            return Element.empty();
        }
        if (obj.istable() && !obj.get("name").isnil()) {
            text.append(" (Named ").append(toComponent(obj.get("name"), true)).append(")");
        }

        return text;
    }

    private Object unwrapLuaValue(LuaValue value) {
        return switch (value.type()) {
            case LuaValue.TFUNCTION, LuaValue.TTHREAD, LuaValue.TTABLE, LuaValue.TNIL -> value;
            case LuaValue.TSTRING -> value.toString();
            case LuaValue.TBOOLEAN -> value.checkboolean();
            case LuaValue.TNUMBER, LuaValue.TINT -> value.checknumber();
            case LuaValue.TUSERDATA -> value.checkuserdata();
            default -> value;
        };
    }

    private boolean shouldDisplayCyclicReferences(LuaValue obj) {
        return switch (obj.type()) {
            case LuaValue.TUSERDATA, LuaValue.TFUNCTION, LuaValue.TTHREAD, LuaValue.TTABLE -> true;
            default -> false;
        };
    }

*/
}
