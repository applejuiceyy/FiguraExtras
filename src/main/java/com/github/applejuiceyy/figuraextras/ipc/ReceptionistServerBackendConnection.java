package com.github.applejuiceyy.figuraextras.ipc;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ipc.backend.ReceptionistServerBackend;
import com.github.applejuiceyy.figuraextras.ipc.protocol.C2CServer;
import com.github.applejuiceyy.figuraextras.ipc.protocol.ClientInformation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.figuramc.figura.FiguraMod;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ReceptionistServerBackendConnection implements C2CServer.C2CServerBackend {
    private final ReceptionistServer.ServerClientInterface client;
    private final ReceptionistServer owner;

    private final Set<UUID> subscriptions = new HashSet<>();

    public ReceptionistServerBackendConnection(ReceptionistServer.ServerClientInterface client, ReceptionistServer owner) {
        this.client = client;
        this.owner = owner;
    }

    private UUID getStoringId() {
        return getStoringId(Objects.requireNonNull(client.information));
    }

    private UUID getStoringId(ClientInformation information) {
        assert information != null;
        String id = information.gameProfileId();
        return UUID.fromString(id == null ? information.instanceId() : id);
    }

    private UUID convertToStoringId(UUID id) {
        for (ReceptionistServer.ServerClientInterface client : owner.allClients) {
            if (client.information != null) {
                if (UUID.fromString(client.information.offlinePlayerId()).equals(id)) {
                    return getStoringId(client.information);
                }
            }
        }
        return id;
    }

    private Iterable<ReceptionistServer.ServerClientInterface> interestedClients() {
        return interestedClients(getStoringId());
    }

    private Iterable<ReceptionistServer.ServerClientInterface> interestedClients(UUID id) {
        return () -> owner.allClients
                .stream()
                .filter(client -> client.receptionistServerBackendConnection.subscriptions.contains(id)).iterator();
    }


    @Override
    public CompletableFuture<Void> ping(byte[] content) {
        ByteArrayInputStream stream = new ByteArrayInputStream(content);
        DataInputStream dis = new DataInputStream(stream);
        try {
            dis.readByte();  // event id, not needed
            int id = dis.readInt();  // ping id
            boolean sync = dis.readBoolean();  // sync

            byte[] bytes = dis.readAllBytes();

            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 1 + 8 + 8 + 4 + 1);
            UUID uuid = getStoringId();

            buffer.put((byte) 1);  // event id, ping
            buffer.putLong(uuid.getMostSignificantBits());  // uuid
            buffer.putLong(uuid.getLeastSignificantBits());  // uuid
            buffer.putInt(id);  // ping name
            buffer.put((byte) (sync ? 1 : 0));  // sync


            buffer.put(bytes);  // data

            byte[] array = buffer.array();
            owner.allClients
                    .stream()
                    .filter(client -> (client != this.client || sync) && (client.receptionistServerBackendConnection.subscriptions.contains(uuid)))
                    .forEach(client -> client.client.getBackend().websocketEvent(array));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> subscribe(String id) {
        subscriptions.add(convertToStoringId(UUID.fromString(id)));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(String id) {
        subscriptions.remove(UUID.fromString(id));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<String> getApi() {
        return CompletableFuture.completedFuture("");
    }

    @Override
    public CompletableFuture<String> getMotd() {
        return CompletableFuture.completedFuture("local backend at instance id " + FiguraExtras.getInstanceUUID().toString() + " or " + FabricLoader.getInstance().getGameDir().toString() + "\n\nHi!");
    }

    @Override
    public CompletableFuture<String> getVersion() {
        return CompletableFuture.completedFuture(FiguraMod.VERSION.toString());
    }

    @Override
    public CompletableFuture<String> getLimits() {
        return CompletableFuture.completedFuture("{\"rate\":{\"upload\":100,\"download\":100},\"limits\":{\"maxAvatarSize\":" + 100 * 1000 + "}}");
    }

    @Override
    public CompletableFuture<String> deleteAvatar(HashMap<String, String> args) {
        String avatarId = args.get("id");
        ReceptionistServerBackend.BackendAvatar avatar = owner.backend.getAvatar(getStoringId(), avatarId);

        if (avatar == null) {
            return CompletableFuture.completedFuture("");
        }

        ReceptionistServerBackend.Collateral collateral = avatar.delete();

        ByteBuffer buffer = ByteBuffer.allocate(1 + 8 + 8);

        for (ReceptionistServerBackend.BackendUser changedUser : collateral.changedUsers()) {
            UUID uuid = changedUser.getUuid();
            buffer.clear();
            buffer.put((byte) 2);
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());
            byte[] array = buffer.array();

            for (ReceptionistServer.ServerClientInterface clientInterface : owner.allClients) {
                ReceptionistServerBackendConnection connection = clientInterface.receptionistServerBackendConnection;
                if (connection.subscriptions.contains(uuid)) {
                    clientInterface.client.getBackend().websocketEvent(array);
                }
            }
        }

            /*Files.delete(resolveAvatarContentPath(getPlayerId(), avatarId));
            for (String file : Objects.requireNonNull(equipmentPath.toFile().list())) {
                String uuid = file.substring(0, file.length() - 2);
                boolean willRewrite = false;

                JsonArray arr;
                File fileObject = equipmentPath.resolve(file).toFile();
                try(FileInputStream fp = new FileInputStream(fileObject)) {
                    arr = JsonParser.parseString(Arrays.toString(fp.readAllBytes())).getAsJsonArray();
                }

                for (Iterator<JsonElement> iterator = arr.iterator(); iterator.hasNext(); ) {
                    JsonElement jsonElement = iterator.next();
                    JsonObject object = jsonElement.getAsJsonObject();
                    JsonElement owner = object.get("owner");
                    JsonElement id = object.get("id");

                    if (owner.getAsString().equals(uuid) && uuid.equals(id.getAsString())) {
                        iterator.remove();
                        willRewrite = true;
                    }
                }

                if(willRewrite) {
                    if(arr.isEmpty()) {
                        Files.delete(fileObject.toPath());
                    }
                    else {
                        try(FileOutputStream fp = new FileOutputStream(fileObject)) {
                            fp.write(arr.toString().getBytes());
                        }
                    }

                    UUID actualUUID = UUID.fromString(uuid);
                    ByteBuffer buffer = ByteBuffer.allocate(1 + 8 + 8);
                    buffer.put((byte) 2);
                    buffer.putLong(actualUUID.getMostSignificantBits());
                    buffer.putLong(actualUUID.getLeastSignificantBits());
                    byte[] array = buffer.array();
                    for (ReceptionistServer.ServerClientInterface serverClientInterface : interestedClients(actualUUID)) {
                        if(uuid.equals(getPlayerId().toString()) && serverClientInterface == client) continue;
                        serverClientInterface.client.getBackend().websocketEvent(array);
                    }
                }
            }
            e*/
        return CompletableFuture.completedFuture("");
    }

    @Override
    public CompletableFuture<String> putAvatar(HashMap<String, String> args) {
        try {
            ReceptionistServerBackend.BackendAvatar avatar = owner.backend.getAvatar(getStoringId(), args.get("id"));
            if (avatar != null) {
                avatar.setContent(Hex.decodeHex(args.get("body")));
            } else {
                owner.backend.createAvatar(getStoringId(), args.get("id"), Hex.decodeHex(args.get("body")));
            }
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture("");
    }

    @Override
    public CompletableFuture<String> setEquip(HashMap<String, String> args) {
        JsonArray body;
        try {
            body = JsonParser.parseString(new String(Hex.decodeHex(args.get("body")))).getAsJsonArray();
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
        List<ReceptionistServerBackend.BackendAvatar> avatars = new ArrayList<>();
        for (JsonElement jsonElement : body) {
            JsonObject object = jsonElement.getAsJsonObject();
            String uuid = object.get("owner").getAsString();
            String id = object.get("id").getAsString();
            avatars.add(owner.backend.getAvatar(UUID.fromString(uuid), id));
        }
        owner.backend.getOrCreateUser(getStoringId()).setEquippedAvatars(avatars.toArray(new ReceptionistServerBackend.BackendAvatar[0]));
        UUID uuid = getStoringId();
        ByteBuffer buffer = ByteBuffer.allocate(1 + 8 + 8);
        buffer.put((byte) 2);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        byte[] array = buffer.array();
        for (ReceptionistServer.ServerClientInterface serverClientInterface : interestedClients()) {
            if (serverClientInterface != client) {
                serverClientInterface.client.getBackend().websocketEvent(array);
            }
        }
        return CompletableFuture.completedFuture("");
    }

    @Override
    public CompletableFuture<String> getUser(HashMap<String, String> args) {
        ReceptionistServerBackend.BackendUser user = owner.backend.getUser(convertToStoringId(UUID.fromString(args.get("id"))));
        if (user == null) {
            return CompletableFuture.completedFuture("{\"equipped\":[],\"equippedBadges\":{\"pride\":[],\"special\":[]}}");
        }

        user.upkeep();

        JsonObject json = new JsonObject();
        JsonArray equipped = new JsonArray();

        ReceptionistServerBackend.BackendAvatar[] avatars = user.getEquippedAvatars();
        for (ReceptionistServerBackend.BackendAvatar element : avatars) {
            JsonObject object = new JsonObject();
            object.addProperty("owner", element.getOwner().toString());
            object.addProperty("id", element.getId());
            Formatter formatter = new Formatter();
            for (byte b : element.getHash()) {
                formatter.format("%02x", b);
            }
            object.addProperty("hash", formatter.toString());

            equipped.add(object);
        }

        json.add("equipped", equipped);

        JsonObject badgesRoot = new JsonObject();
        json.add("equippedBadges", badgesRoot);

        JsonArray prideBadges = new JsonArray();
        JsonArray specialBadges = new JsonArray();


        BitSet prideBitSet = user.getPrideBadges();
        for (int i = 0; i < prideBitSet.length(); i++) {
            prideBadges.add(prideBitSet.get(i) ? 1 : 0);
        }
        BitSet specialBitSet = user.getSpecialBadges();
        for (int i = 0; i < specialBitSet.length(); i++) {
            specialBadges.add(specialBitSet.get(i) ? 1 : 0);
        }


        badgesRoot.add("pride", prideBadges);
        badgesRoot.add("special", specialBadges);
        return CompletableFuture.completedFuture(json.toString());
    }

    @Override
    public CompletableFuture<String> getAvatar(HashMap<String, String> args) {
        ReceptionistServerBackend.BackendAvatar avatar = owner.backend.getAvatar(UUID.fromString(args.get("owner")), args.get("id"));
        if (avatar == null) {
            return CompletableFuture.failedFuture(new ResponseErrorException(new ResponseError(ResponseErrorCode.InvalidParams, "No avatar", null)));
        }
        avatar.upkeep();
        return CompletableFuture.completedFuture(Hex.encodeHexString(avatar.getContent()));
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.completedFuture(null);
    }


    @Override
    public CompletableFuture<?> request(String method, Object parameter) {
        return new CompletableFuture<>();
    }

    @Override
    public void notify(String method, Object parameter) {

    }
}
