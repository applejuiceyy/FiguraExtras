package com.github.applejuiceyy.figuraextras.ipc.backend;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ReceptionistServerBackend {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:Backend");
    private final Path backendDirectory;
    private final Path equipmentPath;
    private final Path avatarPath;

    public ReceptionistServerBackend() {
        backendDirectory = FiguraExtras.getGlobalMinecraftDirectory().resolve("backend");
        equipmentPath = backendDirectory.resolve("players");
        avatarPath = backendDirectory.resolve("avatars");
        List<CleanableBackendObject<?>> cleaningObjects = new ArrayList<>();
        getAvatars().forEach(cleaningObjects::add);
        getUsers().forEach(cleaningObjects::add);
        for (CleanableBackendObject<?> cleaningObject : cleaningObjects) {
            // 10 days
            if (System.currentTimeMillis() > cleaningObject.getUpkeep().getTime() + 1000 * 60 * 60 * 24 * 10) {
                cleaningObject.delete();
            }
        }
    }

    private void write(Path path, CapriciousConsumer<DataOutputStream> stream) {
        write(path, stream, false);
    }

    private void write(Path path, byte[] bytes) {
        write(path, bytes, false);
    }

    private void write(Path path, CapriciousConsumer<DataOutputStream> stream, boolean append) {
        File file = path.toFile();
        File parent = path.getParent().toFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Could not create necessary folders");
        }
        logger.info("Writing to " + path);
        try (DataOutputStream fp = new DataOutputStream(new FileOutputStream(file, append))) {
            stream.accept(fp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(Path path, byte[] bytes, boolean append) {
        write(path, o -> o.write(bytes), append);
    }

    private void delete(Path path) {
        if (!path.startsWith(backendDirectory)) {
            return;
        }
        File file = path.toFile();
        if (file.exists()) {
            logger.info("Deleting " + path);
            if (!file.delete()) {
                throw new RuntimeException("Could not delete file");
            }
        }
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        if (parent.startsWith(backendDirectory) && !backendDirectory.equals(parent)) {
            File folder = parent.toFile();
            if (Objects.requireNonNull(folder.list()).length == 0) {
                delete(parent);
            }
        }
    }

    private <T> T read(Path path, CapriciousFunction<DataInputStream, T> stream) {
        File file = path.toFile();
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        logger.info("Reading from " + path);
        try (DataInputStream fp = new DataInputStream(new FileInputStream(file))) {
            return stream.apply(fp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte @Nullable [] read(Path path) {
        return read(path, InputStream::readAllBytes);
    }

    private <T> List<T> repeatedRead(Path path, CapriciousFunction<DataInputStream, T> consumer) {
        return read(path, i -> {
            ArrayList<T> things = new ArrayList<>();

            while (true) {
                try {
                    things.add(consumer.apply(i));
                } catch (EOFException ignored) {
                    break;
                }
            }

            return things;
        });
    }

    public BackendAvatar getAvatar(UUID owner, String id) {
        return new BackendAvatar(owner, id);
    }

    public BackendUser getUser(UUID id) {
        return new BackendUser(id);
    }

    public Iterable<BackendAvatar> getAvatars() {
        return () -> {
            File file = avatarPath.toFile();
            if (!file.exists()) {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public BackendAvatar next() {
                        throw new NoSuchElementException();
                    }
                };
            }
            return Arrays.stream(
                            Objects.requireNonNull(file.list())
                    )
                    .filter(l -> avatarPath.resolve(l).toFile().isDirectory())
                    .flatMap(l ->
                            Arrays.stream(
                                            Objects.requireNonNull(avatarPath.resolve(l).toFile().list())
                                    )
                                    .filter(o -> o.endsWith(".content"))
                                    .map(o -> new Tuple<>(l, o.substring(0, o.length() - ".content".length())))
                    )
                    .map(t -> new BackendAvatar(UUID.fromString(t.getA()), t.getB())).iterator();
        };
    }

    public Iterable<BackendUser> getUsers() {
        return () -> {
            File file = equipmentPath.toFile();
            if (!file.exists()) {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public BackendUser next() {
                        throw new NoSuchElementException();
                    }
                };
            }
            return Arrays.stream(
                            Objects.requireNonNull(file.list())
                    )
                    .filter(s -> s.endsWith(".equipped"))
                    .map(s -> new BackendUser(UUID.fromString(s.substring(0, s.length() - ".equipped".length()))))
                    .iterator();
        };
    }

    interface CapriciousConsumer<T> {
        void accept(T thing) throws IOException;
    }

    interface CapriciousFunction<T, V> {
        V apply(T thing) throws IOException;
    }

    public record Collateral(List<BackendUser> changedUsers) {
    }

    public class BackendAvatar extends CleanableBackendObject<Collateral> {
        private final UUID owner;
        private final String id;

        public BackendAvatar(UUID owner, String id) {
            this.owner = owner;
            this.id = id;
        }

        public UUID getOwner() {
            return owner;
        }

        public String getId() {
            return id;
        }

        public byte[] getContent() {
            checkExists();
            return Objects.requireNonNull(
                    read(getExtension("content"), InputStream::readAllBytes)
            );
        }

        public void setContent(byte[] bytes) {
            if (!exists()) {
                write(getExtension("use"), new byte[0]);
            }
            write(getExtension("content"), bytes);
            MessageDigest instance;
            try {
                instance = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            Formatter formatter = new Formatter();
            for (byte b : instance.digest(bytes)) {
                formatter.format("%02x", b);
            }
            String hash = formatter.toString();
            write(getExtension("hash"), o -> o.writeUTF(hash));
            upkeep();
        }

        public BackendUser[] getUsers() {
            checkExists();
            return repeatedRead(getExtension("use"), i -> new BackendUser(new UUID(i.readLong(), i.readLong())))
                    .toArray(new BackendUser[0]);
        }

        public String getHash() {
            checkExists();
            return read(getExtension("hash"), DataInput::readUTF);
        }

        public Collateral delete() {
            checkExists();
            Collateral collateral = new Collateral(new ArrayList<>());
            for (BackendUser user : getUsers()) {
                user.setEquippedAvatars(
                        Arrays.stream(user.getEquippedAvatars())
                                .filter(avatar -> !(avatar.id.equals(this.id) && avatar.owner.equals(this.owner)))
                                .toArray(BackendAvatar[]::new),
                        collateral
                );
            }
            for (String ext : new String[]{"content", "hash", "use", "time"}) {
                ReceptionistServerBackend.this.delete(getExtension(ext));
            }
            return collateral;
        }

        public void addUser(UUID user) {
            checkExists();
            write(getExtension("use"), o -> {
                o.writeLong(user.getMostSignificantBits());
                o.writeLong(user.getLeastSignificantBits());
            }, true);
            upkeep();
        }

        public void removeUser(UUID user) {
            checkExists();
            byte[] users = read(getExtension("use"));
            assert users != null;
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(users));
            write(getExtension("use"), o -> {
                while (true) {
                    long most;
                    long least;
                    try {
                        most = dis.readLong();
                        least = dis.readLong();
                    } catch (EOFException ignored) {
                        break;
                    }
                    if (most == user.getMostSignificantBits() && least == user.getLeastSignificantBits()) {
                        dis.transferTo(o);
                        break;
                    } else {
                        o.writeLong(most);
                        o.writeLong(least);
                    }
                }
            });
            upkeep();
        }

        @Override
        protected Path getContainer() {
            return avatarPath.resolve(owner.toString());
        }

        @Override
        protected String getSelfName() {
            return id;
        }

        @Override
        public boolean exists() {
            return getExtension("content").toFile().exists();
        }
    }

    public class BackendUser extends CleanableBackendObject<Void> {

        private final UUID uuid;

        public BackendUser(UUID uuid) {
            this.uuid = uuid;
        }

        public BackendAvatar[] getEquippedAvatars() {
            checkExists();
            return repeatedRead(getExtension("equipped"), i -> new BackendAvatar(
                    new UUID(i.readLong(), i.readLong()),
                    i.readUTF()
            )).toArray(new BackendAvatar[0]);
        }

        public Iterable<BackendAvatar> getUploadedAvatars() {
            return () -> {
                File folder = avatarPath.resolve(uuid.toString()).toFile();
                if (!folder.exists()) {
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return false;
                        }

                        @Override
                        public BackendAvatar next() {
                            throw new NoSuchElementException();
                        }
                    };
                }
                return Arrays.stream(
                                Objects.requireNonNull(folder.list())
                        )
                        .filter(o -> o.endsWith(".content"))
                        .map(o -> o.substring(0, o.length() - ".content".length()))
                        .map(s -> new BackendAvatar(uuid, s))
                        .iterator();
            };
        }

        public Collateral setEquippedAvatars(BackendAvatar[] newAvatars) {
            Collateral collateral = new Collateral(new ArrayList<>());
            setEquippedAvatars(newAvatars, collateral);
            return collateral;
        }

        protected void setEquippedAvatars(BackendAvatar[] newAvatars, Collateral collateral) {
            boolean p = !exists();
            BackendAvatar[] avatars = exists() ? getEquippedAvatars() : new BackendAvatar[0];
            write(getExtension("equipped"), o -> {
                for (BackendAvatar newAvatar : newAvatars) {
                    o.writeLong(newAvatar.getOwner().getMostSignificantBits());
                    o.writeLong(newAvatar.getOwner().getLeastSignificantBits());
                    o.writeUTF(newAvatar.getId());
                }
            });
            for (BackendAvatar newAvatar : newAvatars) {
                newAvatar.addUser(uuid);
            }
            for (BackendAvatar avatar : avatars) {
                avatar.removeUser(uuid);
            }
            collateral.changedUsers().add(this);
            upkeep();
            if (p) {
                setPrideBadges(new BitSet());
                setSpecialBadges(new BitSet());
            }
        }

        public BitSet getPrideBadges() {
            checkExists();
            return BitSet.valueOf(Objects.requireNonNull(read(getExtension("pbadges"))));
        }

        public void setPrideBadges(BitSet badges) {
            checkExists();
            write(getExtension("pbadges"), badges.toByteArray());
        }

        public BitSet getSpecialBadges() {
            checkExists();
            return BitSet.valueOf(Objects.requireNonNull(read(getExtension("sbadges"))));
        }

        public void setSpecialBadges(BitSet badges) {
            checkExists();
            write(getExtension("sbadges"), badges.toByteArray());
        }

        public UUID getUuid() {
            return uuid;
        }

        @Override
        protected Path getContainer() {
            return equipmentPath;
        }

        @Override
        protected String getSelfName() {
            return uuid.toString();
        }

        @Override
        public boolean exists() {
            return getExtension("equipped").toFile().exists();
        }

        @Override
        public Void delete() {
            setEquippedAvatars(new BackendAvatar[0]);
            ReceptionistServerBackend.this.delete(getExtension("equipped"));
            ReceptionistServerBackend.this.delete(getExtension("time"));
            ReceptionistServerBackend.this.delete(getExtension("sbadges"));
            ReceptionistServerBackend.this.delete(getExtension("pbadges"));
            return null;
        }
    }

    abstract class CleanableBackendObject<COLLATERAL> {
        abstract protected Path getContainer();

        abstract protected String getSelfName();

        abstract public boolean exists();

        abstract public COLLATERAL delete();

        public Date getUpkeep() {
            checkExists();
            return Objects.requireNonNull(
                    read(getExtension("time"), i -> new Date(new DataInputStream(i).readLong()))
            );
        }

        public void upkeep() {
            checkExists();
            write(getExtension("time"), o -> o.writeLong(System.currentTimeMillis()));
        }

        protected void checkExists() {
            if (!exists()) {
                throw new NoSuchElementException();
            }
        }

        Path getExtension(String name) {
            Path container = getContainer();

            return container.resolve(getSelfName()
                    .replace("\\", "")
                    .replace("/", "")
                    .replace(container.getFileSystem().getSeparator(), "") + "." + name);
        }
    }
}
