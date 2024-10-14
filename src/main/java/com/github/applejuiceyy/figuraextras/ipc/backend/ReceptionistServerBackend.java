package com.github.applejuiceyy.figuraextras.ipc.backend;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.CommonOps;
import com.github.applejuiceyy.figuraextras.fsstorage.DataId;
import com.github.applejuiceyy.figuraextras.fsstorage.storage.Storage;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.github.applejuiceyy.figuraextras.fsstorage.CommonOps.TIME;

public class ReceptionistServerBackend {
    private static final DataId.RW<UUID> UUID_RW = new DataId.RW<>() {
        @Override
        public UUID read(DataInputStream stream) throws IOException {
            return new UUID(stream.readLong(), stream.readLong());
        }

        @Override
        public void write(UUID data, DataOutputStream stream) throws IOException {
            stream.writeLong(data.getMostSignificantBits());
            stream.writeLong(data.getLeastSignificantBits());
        }
    };
    private static final DataId.RW<String> STRING_RW = new DataId.RW<>() {
        @Override
        public String read(DataInputStream stream) throws IOException {
            return stream.readUTF();
        }

        @Override
        public void write(String data, DataOutputStream stream) throws IOException {
            stream.writeUTF(data);
        }
    };
    private static final DataId.RW<BitSet> BIT_SET_RW = new DataId.RW<>() {
        @Override
        public BitSet read(DataInputStream stream) throws DataId.ParseException, IOException {
            return BitSet.valueOf(stream.readAllBytes());
        }

        @Override
        public void write(BitSet data, DataOutputStream stream) throws IOException {
            stream.write(data.toByteArray());
        }
    };

    private static final DataId<byte[]> AVATAR_DATA = DataId.of(DataId.PASS_THROUGH, "avatar", false);
    private static final DataId<byte[]> AVATAR_HASH = DataId.of(DataId.PASS_THROUGH, "hash");
    private static final DataId<List<UUID>> AVATAR_USE = DataId.of(DataId.repeating(UUID_RW), "use");

    private static final DataId<List<Tuple<UUID, String>>> USER_EQUIPPED = DataId.of(DataId.repeating(DataId.concat(UUID_RW, STRING_RW)), "equipped");
    private static final DataId<BitSet> USER_SPECIAL_BADGES = DataId.of(BIT_SET_RW, "special");
    private static final DataId<BitSet> USER_PRIDE_BADGES = DataId.of(BIT_SET_RW, "pride");

    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:Backend");
    private final Storage playerStorage;
    private final Storage avatarStorage;

    public ReceptionistServerBackend() {
        Path backendDirectory = FiguraExtras.getGlobalMinecraftDirectory().resolve("backend");
        Path playerPath = backendDirectory.resolve("players");
        Path avatarPath = backendDirectory.resolve("avatars");

        playerStorage = Storage.create(playerPath, Set.of(USER_EQUIPPED, USER_PRIDE_BADGES, USER_SPECIAL_BADGES, TIME), 1);
        avatarStorage = Storage.create(avatarPath, Set.of(AVATAR_DATA, AVATAR_HASH, AVATAR_USE, TIME), 2);

        CommonOps.pruneBucketsByTime(() -> Iterators.concat(playerStorage.iterator(), avatarStorage.iterator()), Duration.ofDays(10));
    }

    @Nullable
    public BackendAvatar getAvatar(UUID owner, String id) {
        Bucket bucket = avatarStorage.getBucket(owner.toString(), id);
        return bucket == null ? null : new BackendAvatar(bucket);
    }

    public BackendAvatar createAvatar(UUID owner, String id, byte[] bytes) {
        return new BackendAvatar(
                avatarStorage
                        .createBucket(owner.toString(), id)
                        .data(AVATAR_DATA, bytes)
                        .data(AVATAR_USE, List.of())
                        .data(TIME, Instant.now())
                        .data(AVATAR_HASH, Util.hashBytes(bytes))
                        .create()
        );
    }

    public BackendUser getOrCreateUser(UUID user) {
        BackendUser backendUser = getUser(user);
        if (backendUser == null) {
            return createUser(user);
        }
        return backendUser;
    }

    @Nullable
    public BackendUser getUser(UUID id) {
        Bucket bucket = playerStorage.getBucket(id.toString());
        return bucket == null ? null : new BackendUser(bucket);
    }

    public BackendUser createUser(UUID user) {
        return new BackendUser(
                playerStorage.createBucket(user.toString())
                        .data(TIME, Instant.now())
                        .data(USER_EQUIPPED, List.of())
                        .data(USER_PRIDE_BADGES, new BitSet())
                        .data(USER_SPECIAL_BADGES, new BitSet())
                        .create()
        );
    }

    public Iterable<BackendAvatar> getAvatars() {
        return () -> new Iterator<>() {
            final Iterator<Bucket> bucketIterator = avatarStorage.iterator();

            @Override
            public boolean hasNext() {
                return bucketIterator.hasNext();
            }

            @Override
            public BackendAvatar next() {
                return new BackendAvatar(bucketIterator.next());
            }
        };
    }

    public Iterable<BackendUser> getUsers() {
        return () -> new Iterator<>() {
            final Iterator<Bucket> bucketIterator = playerStorage.iterator();

            @Override
            public boolean hasNext() {
                return bucketIterator.hasNext();
            }

            @Override
            public BackendUser next() {
                return new BackendUser(bucketIterator.next());
            }
        };
    }

    public record Collateral(List<BackendUser> changedUsers) {
    }

    public class BackendAvatar extends UpkeptObject {
        public BackendAvatar(Bucket next) {
            super(next);
        }

        public byte[] getContent() {
            return in.get(AVATAR_DATA);
        }

        public void setContent(byte[] bytes) {
            in.set(AVATAR_DATA, bytes);
            in.set(AVATAR_HASH, Util.hashBytes(bytes));
        }

        public byte[] getHash() {
            return in.get(AVATAR_HASH);
        }

        public Collateral delete() {
            Collateral collateral = new Collateral(new ArrayList<>());
            for (BackendUser user : getUsers()) {
                user.setEquippedAvatars(
                        Arrays.stream(user.getEquippedAvatars())
                                .filter(avatar -> !(avatar.getId().equals(this.getId()) && avatar.getOwner().equals(this.getOwner())))
                                .toArray(BackendAvatar[]::new),
                        collateral
                );
            }
            in.delete();
            return collateral;
        }

        public BackendUser[] getUsers() {
            return in.get(AVATAR_USE)
                    .stream()
                    .map(p -> playerStorage.getBucket(p.toString()))
                    .filter(Objects::nonNull)
                    .map(BackendUser::new)
                    .toArray(BackendUser[]::new);
        }

        public String getId() {
            return in.getBuckets()[1];
        }

        public UUID getOwner() {
            return UUID.fromString(in.getBuckets()[0]);
        }

        public void addUser(UUID user) {
            List<UUID> uuids = in.get(AVATAR_USE);
            uuids.add(user);
            in.set(AVATAR_USE, uuids);
        }

        public void removeUser(UUID user) {
            List<UUID> uuids = in.get(AVATAR_USE);
            uuids.remove(user);
            in.set(AVATAR_USE, uuids);
        }
    }


    public class BackendUser extends UpkeptObject {

        public BackendUser(Bucket next) {
            super(next);
        }

        public Iterable<BackendAvatar> getUploadedAvatars() {
            return () -> Streams.stream(avatarStorage.iterate(getUuid().toString()))
                    .map(BackendAvatar::new)
                    .iterator();
        }

        public UUID getUuid() {
            return UUID.fromString(in.getBuckets()[0]);
        }

        public Collateral setEquippedAvatars(BackendAvatar[] newAvatars) {
            Collateral collateral = new Collateral(new ArrayList<>());
            setEquippedAvatars(newAvatars, collateral);
            return collateral;
        }

        protected void setEquippedAvatars(BackendAvatar[] newAvatars, Collateral collateral) {
            BackendAvatar[] oldAvatars = getEquippedAvatars();
            in.set(USER_EQUIPPED, Arrays.stream(newAvatars).map(a -> new Tuple<>(a.getOwner(), a.getId())).toList());

            for (BackendAvatar newAvatar : newAvatars) {
                newAvatar.addUser(getUuid());
            }
            for (BackendAvatar avatar : oldAvatars) {
                avatar.removeUser(getUuid());
            }

            collateral.changedUsers().add(this);
        }

        public BackendAvatar[] getEquippedAvatars() {
            return in.get(USER_EQUIPPED)
                    .stream()
                    .map(p -> avatarStorage.getBucket(p.getA().toString(), p.getB()))
                    .filter(Objects::nonNull)
                    .map(BackendAvatar::new)
                    .toArray(BackendAvatar[]::new);
        }

        public BitSet getPrideBadges() {
            return in.get(USER_PRIDE_BADGES);
        }

        public void setPrideBadges(BitSet badges) {
            in.set(USER_PRIDE_BADGES, badges);
        }

        public BitSet getSpecialBadges() {
            return in.get(USER_SPECIAL_BADGES);
        }

        public void setSpecialBadges(BitSet badges) {
            in.set(USER_SPECIAL_BADGES, badges);
        }

        public void delete() {
            for (BackendAvatar user : getEquippedAvatars()) {
                user.removeUser(getUuid());
            }

            in.delete();
        }
    }

    protected class UpkeptObject {
        protected final Bucket in;

        public UpkeptObject(Bucket next) {
            in = next;
        }

        public void upkeep() {
            in.set(TIME, Instant.now());
        }

        public Instant getUpkeep() {
            return in.get(TIME);
        }
    }
}
