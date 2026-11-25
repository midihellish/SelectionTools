package dev.buildzones.util;

import dev.buildzones.BuildZones;
import net.minecraft.util.Identifier;

/** Identificadores compartilhados por cliente e servidor (canais de rede). */
public final class ModIds {
    public static final Identifier REPLACE_REQUEST = id("replace_block_request");
    public static final Identifier REPLACE_RESULT = id("replace_block_result");
    public static final Identifier SYNC_REGION = id("sync_region");
    public static final Identifier AREA_OP_REQUEST = id("area_op_request");
    public static final Identifier AREA_PLACE_REQUEST = id("area_place_request");
    public static final Identifier AREA_RESULT = id("area_result");

    private ModIds() {
    }

    public static Identifier id(String path) {
        return new Identifier(BuildZones.MOD_ID, path);
    }
}
