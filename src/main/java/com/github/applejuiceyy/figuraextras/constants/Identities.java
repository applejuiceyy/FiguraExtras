package com.github.applejuiceyy.figuraextras.constants;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

public class Identities {
    public static final UUID instanceUUID;
    public static final SymmetricSigner avatarSigner;
    public static Logger logger = LogUtils.getLogger();

    static {
        Path file = Paths.figuraExtrasDirectory.resolve("id");
        try {
            UUID i = null;

            if (Files.exists(file)) {
                try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file))) {
                    char[] uuid = new char[36];
                    if (reader.read(uuid) == 36 && reader.read() == -1) {
                        i = UUID.fromString(String.valueOf(uuid));
                    }
                }
            }
            if (i == null) {
                i = UUID.randomUUID();
                Files.write(file, i.toString().getBytes());
            }

            instanceUUID = i;


            SecretKey key = null;

            file = Paths.globalMinecraftDirectory.resolve("avatarkey");
            Mac mac = Mac.getInstance("HmacSHA256");
            reading:
            if (Files.exists(file)) {
                logger.info("Sourcing generated secret key");
                SecretKeySpec hmacSHA256 = new SecretKeySpec(Files.readAllBytes(file), "HmacSHA256");

                try {
                    mac.init(hmacSHA256);
                } catch (InvalidKeyException e) {
                    logger.warn("Deleting key file because apparently it's invalid");
                    Files.delete(file);
                    break reading;
                }

                key = hmacSHA256;
            }

            if (key == null) {
                logger.info("Generating a new secret key");
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA256");
                generator.init(1024 * 8);
                key = generator.generateKey();
                try {
                    mac.init(key);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
                Files.write(file, key.getEncoded());
            }
            avatarSigner = new SymmetricSigner() {
                @Override
                public byte[] sign(byte[] bytes) {
                    return mac.doFinal(bytes);
                }

                @Override
                public boolean verify(byte[] message, byte[] signature) {
                    return Arrays.equals(sign(message), signature);
                }
            };
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public interface SymmetricSigner {
        byte[] sign(byte[] bytes);

        boolean verify(byte[] message, byte[] signature);
    }
}
