package com.github.applejuiceyy.figuraextras.fsstorage;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class FolderManager implements Iterable<Tuple<String, Path>> {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:FolderManager");


    private static final String INDEXJSON = "index.json";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final Path path;
    private final JsonObject actualToFolder;
    private final Map<String, String> folderToActual;
    private final Path INDEXJSONPATH;


    public FolderManager(Path path, JsonObject object) {
        this.path = path;
        INDEXJSONPATH = path.resolve(INDEXJSON);
        actualToFolder = object;
        folderToActual = object.entrySet().stream().collect(Collectors.toMap(p -> p.getValue().getAsString(), Map.Entry::getKey));
    }

    public static FolderManager open(Path path) {
        JsonObject object = null;
        Path resolved = path.resolve(INDEXJSON);
        if (Files.exists(resolved)) {
            try {
                object = (JsonObject) JsonParser.parseString(Files.readString(resolved));
            } catch (IOException | ClassCastException ignored) {
            }
        }
        if (object == null) {
            object = new JsonObject();
        }
        return new FolderManager(path, object);
    }

    public Path getPath() {
        return path;
    }

    public Path getOrCreateFolder(String name) throws IOException {
        Path folder = getFolder(name);
        if (folder == null) {
            return createFolder(name);
        }
        return folder;
    }

    public Path getFolder(String name) {
        Path folderPath;
        if (actualToFolder.has(name)) {
            folderPath = path.resolve(actualToFolder.get(name).getAsString());
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                logger.warn("Unexpected deletion of folder with alternate name {}", folderPath);
                actualToFolder.remove(name);
                try {
                    saveIndex();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (name.contains(path.getFileSystem().getSeparator())) return null;
            try {
                folderPath = path.resolve(name);
            } catch (InvalidPathException ignored) {
                return null;
            }
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                return null;
            }
        }

        return folderPath;
    }

    public Path createFolder(String name) throws IOException {
        Path metadata = createFolderMetadata(name);
        Files.createDirectory(metadata);
        return metadata;
    }

    private Path createFolderMetadata(String name) throws IOException {
        if (actualToFolder.has(name)) {
            throw new IllegalArgumentException("Already has a folder with this name");
        }

        String actual = name;
        if (!INDEXJSON.equals(name)) {
            if (!name.contains(path.getFileSystem().getSeparator()) && !folderToActual.containsKey(name)) {
                if (Files.exists(path.resolve(name))) {
                    throw new IllegalArgumentException("Already has a folder with this name");
                }

                try {
                    return path.resolve(name);
                } catch (InvalidPathException ignored) {
                }
            }
        } else {
            name = "index_json";
        }

        name = findSuitableName(name);
        folderToActual.put(name, actual);
        actualToFolder.addProperty(actual, name);
        saveIndex();
        return path.resolve(name);
    }

    String findSuitableName(String in) {
        String blacklisted = path.getFileSystem() == FileSystems.getDefault() && Util.getPlatform() == Util.OS.WINDOWS ? "<>:\"|?*" : "";
        blacklisted += path.getFileSystem().getSeparator();
        for (char c : blacklisted.toCharArray()) {
            in = in.replace(c, '_');
        }
        try {
            path.resolve(in);
        } catch (InvalidPathException ignored) {
            in = "folder";
        }

        int i = 1;
        String name = in;
        while (folderToActual.containsKey(name) || Files.exists(path.resolve(name))) {
            name = in + i++;
        }
        ;
        return name;
    }

    void saveIndex() throws IOException {

        if (actualToFolder.size() == 0) {
            if (Files.exists(INDEXJSONPATH)) {
                Files.delete(INDEXJSONPATH);
            }
        } else {
            Files.writeString(INDEXJSONPATH, GSON.toJson(actualToFolder));
        }
    }

    public void deleteFolder(String name) throws IOException {
        String folder = actualToFolder.has(name) ? actualToFolder.get(name).getAsString() : name;
        Files.walkFileTree(path.resolve(folder), new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        if (actualToFolder.has(name)) {
            actualToFolder.remove(name);
            folderToActual.remove(folder);
            saveIndex();
        }
    }

    @NotNull
    @Override
    public Iterator<Tuple<String, Path>> iterator() {

        try {
            //noinspection resource
            return Streams.concat(
                    actualToFolder.entrySet().stream().map(e -> new Tuple<>(e.getKey(), path.resolve(e.getValue().getAsString()))),
                    Files.list(path)
                            .filter(Files::isDirectory)
                            .map(p -> new Tuple<>(p.getFileName().toString(), p))
                            .filter(t -> !folderToActual.containsKey(t.getA()))
            ).iterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
