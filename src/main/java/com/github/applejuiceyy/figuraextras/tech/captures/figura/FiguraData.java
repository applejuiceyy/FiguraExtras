package com.github.applejuiceyy.figuraextras.tech.captures.figura;

import com.github.applejuiceyy.figuraextras.tech.captures.Hook;
import org.figuramc.figura.avatar.Avatar;
import org.luaj.vm2.Varargs;

public record FiguraData(String reason, Avatar.Instructions instructions, Object toRun, Varargs values) implements Hook.Discernible {
}
