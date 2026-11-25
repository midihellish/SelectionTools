package dev.buildzones.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildzones.client.selection.SelectionRegion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SelectionCodecTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @TempDir
    Path tempDir;

    private static SelectionRegion region(String name, BlockPos a, BlockPos b) {
        return new SelectionRegion(UUID.randomUUID(), a, b, OVERWORLD, 0x53DDF2, name);
    }

    @Test
    void roundTripKeepsEveryField() {
        SelectionRegion r1 = region("Área 01", new BlockPos(100, 64, 100), new BlockPos(120, 80, 115));
        SelectionRegion r2 = region("Área 02", new BlockPos(-5, -60, 7), new BlockPos(3, 10, 9));
        SelectionStorage storage = new SelectionStorage(tempDir);

        assertTrue(storage.save("sp-abc", "Mundo", OVERWORLD, List.of(r1, r2), 1L));
        List<SelectionRegion> loaded = storage.load("sp-abc", OVERWORLD);

        assertEquals(2, loaded.size());
        assertEquals(r1.id(), loaded.get(0).id());
        assertEquals(r1.anchor(), loaded.get(0).anchor());
        assertEquals(r1.opposite(), loaded.get(0).opposite());
        assertEquals(0x53DDF2, loaded.get(0).color());
        assertEquals("Área 01", loaded.get(0).name());
        assertEquals(new BlockPos(-5, -60, 7), loaded.get(1).anchor());
        assertEquals(OVERWORLD, loaded.get(1).dimension());
    }

    @Test
    void shapeSurvivesRoundTripAndDefaultsToFill() {
        SelectionRegion hollow = new SelectionRegion(UUID.randomUUID(), new BlockPos(0, 0, 0), new BlockPos(4, 4, 4),
                OVERWORLD, 0x53DDF2, "Oca", true, dev.buildzones.client.area.AreaShape.HOLLOW);
        SelectionStorage storage = new SelectionStorage(tempDir);
        storage.save("sp-shape", "Mundo", OVERWORLD, List.of(hollow), 1L);
        assertEquals(dev.buildzones.client.area.AreaShape.HOLLOW, storage.load("sp-shape", OVERWORLD).get(0).shape());
        assertEquals(dev.buildzones.client.area.AreaShape.FILL, region("x", new BlockPos(0, 0, 0), new BlockPos(1, 1, 1)).shape());
    }

    @Test
    void missingFileGivesEmptyList() {
        SelectionStorage storage = new SelectionStorage(tempDir);
        assertTrue(storage.load("nope", OVERWORLD).isEmpty());
    }

    @Test
    void corruptFileDoesNotThrow() throws Exception {
        SelectionStorage storage = new SelectionStorage(tempDir);
        Path file = storage.fileFor("sp-bad", OVERWORLD);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ isto não é json");
        assertTrue(storage.load("sp-bad", OVERWORLD).isEmpty());
    }

    @Test
    void invalidEntriesAreSkippedAndOthersKept() throws Exception {
        String json = """
                {
                  "schemaVersion": 1,
                  "dimension": "minecraft:overworld",
                  "regions": [
                    { "id": "not-a-uuid", "anchor": [1,2,3], "opposite": [4,5,6], "color": "#FFFFFF" },
                    { "id": "7c2a1f8e-8b2a-4c6e-9fd8-000000000001", "anchor": [1,2], "opposite": [4,5,6] },
                    { "id": "7c2a1f8e-8b2a-4c6e-9fd8-000000000002", "anchor": [100,64,100], "opposite": [120,80,115], "color": "#53DDF2" }
                  ]
                }
                """;
        SelectionStorage storage = new SelectionStorage(tempDir);
        Path file = storage.fileFor("sp-mixed", OVERWORLD);
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);

        List<SelectionRegion> loaded = storage.load("sp-mixed", OVERWORLD);
        assertEquals(1, loaded.size());
        assertEquals(new BlockPos(100, 64, 100), loaded.get(0).anchor());
    }

    @Test
    void unknownSchemaVersionIsIgnored() throws Exception {
        String json = "{ \"schemaVersion\": 99, \"regions\": [] }";
        SelectionStorage storage = new SelectionStorage(tempDir);
        Path file = storage.fileFor("sp-v99", OVERWORLD);
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
        assertFalse(storage.load("sp-v99", OVERWORLD).size() > 0);
    }

    @Test
    void hexColorsRoundTrip() {
        assertEquals("#53DDF2", SelectionCodec.toHex(0x53DDF2));
        assertEquals(0x8FFFF0, SelectionCodec.parseHex("#8FFFF0", 0));
        assertEquals(0x111111, SelectionCodec.parseHex("cor-ruim", 0x111111));
    }
}
