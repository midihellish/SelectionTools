package dev.buildzones.network;

import dev.buildzones.replacement.ReplacementResult;
import net.minecraft.network.PacketByteBuf;

/** Resposta do servidor: sucesso ou o motivo da recusa. */
public record ReplaceBlockResult(ReplacementResult result) {

    public static ReplaceBlockResult read(PacketByteBuf buf) {
        return new ReplaceBlockResult(buf.readEnumConstant(ReplacementResult.class));
    }

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(result);
    }
}
