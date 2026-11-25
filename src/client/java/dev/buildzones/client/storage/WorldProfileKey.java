package dev.buildzones.client.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.WorldSavePath;

/**
 * Identifica o mundo atual sem guardar caminhos ou endereços em texto puro.
 * Singleplayer usa a pasta do save; multiplayer usa o endereço do servidor.
 */
public final class WorldProfileKey {

    public record Profile(String key, String label) {
    }

    private WorldProfileKey() {
    }

    public static Optional<Profile> resolve(MinecraftClient client) {
        IntegratedServer server = client.getServer();
        if (server != null) {
            Path savePath = server.getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
            Path folder = savePath.getFileName();
            String label = folder == null ? "singleplayer" : folder.toString();
            return Optional.of(new Profile("sp-" + hash(savePath.toString()), label));
        }
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null) {
            String address = info.address.toLowerCase(Locale.ROOT);
            return Optional.of(new Profile("mp-" + hash(address), info.address));
        }
        return Optional.empty();
    }

    /** SHA-256 truncado: usado só como nome de pasta local. */
    static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format(Locale.ROOT, "%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
