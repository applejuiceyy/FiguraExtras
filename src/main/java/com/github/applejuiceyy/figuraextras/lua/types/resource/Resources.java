package com.github.applejuiceyy.figuraextras.lua.types.resource;

import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import org.luaj.vm2.LuaError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@LuaClass
public class Resources {

    private final Path avatarPath;

    public Resources(Path avatarPath) {
        this.avatarPath = avatarPath;
    }

    @LuaMethod
    String readFile(Path file) {
        Path filePath = avatarPath.resolve(file).toAbsolutePath().normalize();
        if (!filePath.startsWith(avatarPath)) {
            throw new LuaError("Attempt to read outside avatar file");
        }
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new LuaError(e.getMessage());
        }
    }
}
