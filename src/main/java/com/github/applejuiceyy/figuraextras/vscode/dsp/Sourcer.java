package com.github.applejuiceyy.figuraextras.vscode.dsp;

import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura.LuaRuntimeAccessor;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.Tuple;
import org.eclipse.lsp4j.debug.LoadedSourceEventArguments;
import org.eclipse.lsp4j.debug.LoadedSourceEventArgumentsReason;
import org.eclipse.lsp4j.debug.Source;
import org.figuramc.figura.avatar.Avatar;
import org.luaj.vm2.Prototype;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class Sourcer {

    private final DebugProtocolServer owner;

    Sourcer(DebugProtocolServer owner) {
        this.owner = owner;
    }

    void registerNewDynamicLoad(String chunkName, int name) {
        LoadedSourceEventArguments arguments = new LoadedSourceEventArguments();
        arguments.setSource(dynamicSourceScript(name, chunkName));
        arguments.setReason(LoadedSourceEventArgumentsReason.NEW);
        owner.client.loadedSource(arguments);
    }

    public void unregisterDynamicLoad(String chunkName, int name) {
        LoadedSourceEventArguments arguments = new LoadedSourceEventArguments();
        arguments.setSource(dynamicSourceScript(name, chunkName));
        arguments.setReason(LoadedSourceEventArgumentsReason.REMOVED);
        owner.client.loadedSource(arguments);
    }

    void regularSourceRegistered(String str) {
        LoadedSourceEventArguments arguments = new LoadedSourceEventArguments();
        Source source = new Source();
        source.setPath(convertFiguraPathToRealLifePath(str));
        arguments.setSource(source);
        arguments.setReason(LoadedSourceEventArgumentsReason.NEW);
        owner.client.loadedSource(arguments);
    }

    String getSourceContent(Source source, boolean onlyLoaded) {
        Avatar currentAvatar = owner.getCurrentAvatar();
        if (source.getPath() != null) {
            if (currentAvatar == null || currentAvatar.luaRuntime == null) {
                return null;
            }
            String key = convertRealLifePathToFiguraPath(source.getPath());
            if (!onlyLoaded || ((LuaRuntimeAccessor) currentAvatar.luaRuntime).getLoadedScripts().containsKey(key)) {
                return ((LuaRuntimeAccessor) currentAvatar.luaRuntime).getScripts().get(key);
            }
        } else {
            Integer sourceReference = source.getSourceReference();
            if (sourceReference != null) {
                return ((LuaRuntimeAccess) currentAvatar.luaRuntime).figuraExtrass$getSource(sourceReference);
            }
        }
        return null;
    }

    Source[] listLoadedSources() {
        if (owner.getCurrentAvatar() == null || owner.getCurrentAvatar().luaRuntime == null) {
            return new Source[0];
        }
        List<Source> sources = new ArrayList<>();
        for (String source : ((LuaRuntimeAccessor) owner.getCurrentAvatar().luaRuntime).getLoadedScripts().keySet()) {
            sources.add(sourceScript(source));
        }
        for (Map.Entry<Integer, Tuple<String, String>> entry : ((LuaRuntimeAccess) owner.getCurrentAvatar().luaRuntime).figuraExtrass$getRegisteredDynamicSources().entrySet()) {
            sources.add(dynamicSourceScript(entry.getKey(), entry.getValue().getB()));
        }
        return sources.toArray(new Source[0]);
    }

    Source sourceScript(String name) {
        Source source = new Source();
        source.setPath(convertFiguraPathToRealLifePath(name));
        source.setName(name);
        return source;
    }

    Source dynamicSourceScript(int name, String filename) {
        Source source = new Source();
        source.setSourceReference(name);
        source.setName(filename);
        source.setPath("generated-" + name + ".lua");
        source.setOrigin("Dynamically loaded source");
        return source;
    }

    Either<String, Integer> toEither(Source source) {
        if (source.getPath() != null) {
            return Either.left(convertRealLifePathToFiguraPath(source.getPath()));
        } else {
            Integer sourceReference = source.getSourceReference();
            if (sourceReference != null) {
                return Either.right(sourceReference);
            }
        }
        throw new RuntimeException("Unknown source");
    }

    public Source toSource(Prototype proto) {
        WeakHashMap<Prototype, Integer> weakHashMap = ((LuaRuntimeAccess) owner.getCurrentAvatar().luaRuntime)
                .figuraExtrass$getPrototypesMarkedAsLoadStringed();

        if (weakHashMap.containsKey(proto)) {
            return dynamicSourceScript(weakHashMap.get(proto), proto.source.tojstring());
        } else {
            return sourceScript(proto.source.tojstring());
        }
    }

    public String convertFiguraPathToRealLifePath(String in) {
        return getAvatarPath().resolve(in.replace("/", FileSystems.getDefault().getSeparator()) + ".lua").toString();
    }

    public String convertRealLifePathToFiguraPath(String in) {
        Path of = Path.of(in);
        String filename = of.getFileName().toString();
        Path cropped = of.getParent().resolve(filename.substring(0, filename.length() - 4));
        return getAvatarPath().relativize(cropped).toString().replace(FileSystems.getDefault().getSeparator(), "/");
    }

    public Path getAvatarPath() {
        return new File((String) owner.launchArgs.get("avatarPath")).toPath();
    }
}
