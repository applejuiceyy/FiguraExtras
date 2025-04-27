package com.github.applejuiceyy.figuraextras.constants;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Paths {
    public static final Path globalMinecraftDirectory;
    public static final Path figuraExtrasDirectory;
    public static final Path globalBackendDirectory;
    public static final Path avatarPostProcessorsDirectory;
    public static Logger logger = LogUtils.getLogger();

    static {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        String s = switch (Util.getPlatform()) {
            case WINDOWS -> System.getenv("APPDATA") + "/.minecraft";
            case OSX -> System.getProperty("user.home") + "/Library/Application Support/minecraft";
            default -> System.getProperty("user.home") + "/.minecraft";
        };

        globalMinecraftDirectory = Path.of(s, "figura_extras", "global");
        figuraExtrasDirectory = gameDir.resolve("figura_extras");
        globalBackendDirectory = globalMinecraftDirectory.resolve("backend");
        avatarPostProcessorsDirectory = figuraExtrasDirectory.resolve("avatar-postprocessors");
        try {
            try {
                Files.createDirectories(figuraExtrasDirectory);
            } catch (FileAlreadyExistsException ignored) {
            }

            if (FiguraExtras.DEBUG) {
                try {
                    Files.createDirectory(figuraExtrasDirectory.resolve("compile_debug"));
                } catch (FileAlreadyExistsException ignored) {
                }
            }

            try {
                Files.createDirectories(globalMinecraftDirectory);
            } catch (FileAlreadyExistsException ignored) {
            }

            try {
                Files.createDirectory(globalBackendDirectory);
            } catch (FileAlreadyExistsException ignored) {
            }

            try {
                Files.createDirectory(avatarPostProcessorsDirectory);
                writeExamplesPostProcessor(avatarPostProcessorsDirectory, Util.getPlatform() == Util.OS.WINDOWS);
            } catch (FileAlreadyExistsException ignored) {
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeExamplesPostProcessor(Path path, boolean isWindows) {
        String prefix = "assets/figuraextras/files/StartupPostProcessors/" + (isWindows ? "Windows" : "Other") + "/";
        try (BufferedReader is = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Paths.class.getClassLoader().getResourceAsStream(prefix + "refs"))))) {
            String s;
            while ((s = is.readLine()) != null) {
                try (
                        InputStream fileStream = Objects.requireNonNull(Paths.class.getClassLoader().getResourceAsStream(prefix + s));
                        OutputStream destination = Files.newOutputStream(path.resolve(s))
                ) {
                    fileStream.transferTo(destination);
                }
            }
        } catch (IOException e) {
            logger.error("Error while writing post-processor examples", e);
            throw new RuntimeException(e);
        }
    }
}
