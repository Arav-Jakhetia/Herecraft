package net.herecraft.client.world;

import net.herecraft.world.level.block.Block;
import net.herecraft.world.level.block.Blocks;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

public class ChunkStorage {
    private static final int MAGIC = 0x48435246;
    private static final int HEADER_BYTES = 8192;
    private static final int SECTOR_BYTES = 4096;

    private ChunkStorage() {}

    public static void save(File worldFolder, Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        int regionX = Math.floorDiv(chunkX, 32);
        int regionZ = Math.floorDiv(chunkZ, 32);
        int localX = Math.floorMod(chunkX, 32);
        int localZ = Math.floorMod(chunkZ, 32);
        int slot = localX + localZ * 32;

        File region = regionFile(worldFolder, regionX, regionZ);
        File parent = region.getParentFile();

        if(!parent.mkdirs() && !parent.isDirectory()) {
            throw new RuntimeException("Could not create region folder: " + parent);
        }

        try {
            Map<Integer, byte[]> records = readRegion(region);
            records.put(slot, compress(encodeChunk(chunk)));

            File temp = new File(parent, region.getName() + ".tmp");
            writeRegion(temp, records);

            try {
                Files.move(temp.toPath(), region.toPath(), StandardCopyOption.REPLACE_EXISTING,  StandardCopyOption.ATOMIC_MOVE);
            } catch(IOException exception) {
                Files.move(temp.toPath(), region.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException exception) {
            throw new RuntimeException("Could not save region " + region, exception);
        }
    }

    public static void load(File worldFolder, Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        int regionX = Math.floorDiv(chunkX, 32);
        int regionZ = Math.floorDiv(chunkZ, 32);
        int slot = Math.floorMod(chunkX, 32) + Math.floorMod(chunkZ,32) * 32;
        File region = regionFile(worldFolder, regionX, regionZ);

        if(!region.isFile()) return;

        try(RandomAccessFile input = new RandomAccessFile(region, "r")) {
            if(input.length() < HEADER_BYTES) return;

            input.seek((long)slot * 4);
            int location = input.readInt();
            int sectorOffset = location >>> 8;

            if(sectorOffset < 2) return;

            input.seek((long)sectorOffset * SECTOR_BYTES);
            int length = input.readInt();
            int compression = input.readUnsignedByte();

            if(length < 1 || compression != 2) {
                throw new IOException("Invalid Herecraft chunk record");
            }

            byte[] compressed = new byte[length - 1];
            input.readFully(compressed);

            byte[] raw;
            try(DataInputStream data = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(compressed)))) {
                raw = data.readAllBytes();
            }

            decodeChunk(raw, chunk);
        } catch(IOException exception) {
            throw new RuntimeException("Could not load chunk " + chunkX + ", " + chunkZ, exception);
        }
    }

    private static File regionFile(File worldFolder, int regionX, int regionZ) {
        return new File(worldFolder, "dimensions/herecraft/overworld/region/r." + regionX + "." + regionZ + ".mca");
    }

    private static byte[] encodeChunk(Chunk chunk) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try(DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(chunk.getChunkX());
            output.writeInt(chunk.getChunkZ());

            for(int y = 0; y < Chunk.SIZE; y++) {
                for(int x = 0; x < Chunk.SIZE; x++) {
                    for(int z = 0; z < Chunk.SIZE; z++) {
                        output.writeByte(blockId(chunk.getBlockLocal(x, y, z)));
                    }
                }
            }
        }

        return bytes.toByteArray();
    }

    private static void decodeChunk(byte[] raw, Chunk chunk)  throws IOException {
        try(DataInputStream input = new DataInputStream(new ByteArrayInputStream(raw))) {
            if(input.readInt() != MAGIC || input.readInt() != chunk.getChunkX() || input.readInt() != chunk.getChunkZ()) {
                throw new IOException("Invalid chunk data");
            }

            for(int y = 0; y < Chunk.SIZE; y++) {
                for(int x = 0; x < Chunk.SIZE; x++) {
                    for(int z = 0; z < Chunk.SIZE; z++) {
                        chunk.setBlockLocal(x, y, z, block(input.readUnsignedByte()));
                    }
                }
            }
        }
    }

    private static Map<Integer, byte[]> readRegion(File region) throws IOException {
        Map<Integer, byte[]> records = new HashMap<>();
        if(!region.isFile() || region.length() < HEADER_BYTES) return records;

        try(RandomAccessFile input = new RandomAccessFile(region, "r")) {
            for(int slot = 0; slot < 1024; slot++) {
                input.seek((long)slot * 4);
                int location = input.readInt();
                int sectorOffset = location >>> 8;

                if(sectorOffset < 2) continue;

                input.seek((long)sectorOffset * SECTOR_BYTES);
                int length = input.readInt();
                int compression = input.readUnsignedByte();

                if(length < 1 || compression != 2) continue;

                byte[] data = new byte[length - 1];
                input.readFully(data);
                records.put(slot, data);
            }
        }

        return records;
    }

    private static void writeRegion(File file, Map<Integer, byte[]> records) throws IOException {
        try(RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            output.setLength(HEADER_BYTES);
            int nextSector = 2;

            for(Map.Entry<Integer, byte[]> entry : records.entrySet()) {
                int slot = entry.getKey();
                byte[] compressed = entry.getValue();
                int recordLength = compressed.length + 1;
                int sectors = (recordLength + 4 + SECTOR_BYTES - 1) / SECTOR_BYTES;

                if(sectors > 255) {
                    throw new IOException("Chunk record exceeds MCA sector limit");
                }

                output.seek((long)slot * 4);
                output.writeByte((nextSector >>> 16) & 0xFF);
                output.writeByte((nextSector >>> 8) & 0xFF);
                output.writeByte(nextSector & 0xFF);
                output.writeByte(sectors);

                output.seek(4096L + (long)slot * 4);
                output.writeInt((int)(System.currentTimeMillis() / 1000L));

                output.seek((long)nextSector * SECTOR_BYTES);
                output.writeInt(recordLength);
                output.writeByte(2);
                output.write(compressed);

                nextSector += sectors;
                output.setLength((long)nextSector * SECTOR_BYTES);
            }
        }
    }

    private static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(DataOutputStream output = new DataOutputStream(bytes)) {
            output.write(data);
        }
        return bytes.toByteArray();
    }

    private static int blockId(Block block) {
        if(block == Blocks.AIR) return 0;
        if(block == Blocks.GRASS) return 1;
        if(block == Blocks.DIRT) return 2;
        if(block == Blocks.STONE) return 3;
        if(block == Blocks.COBBLESTONE) return 4;
        if(block == Blocks.OAK_PLANKS) return 5;
        if(block == Blocks.OAK_SAPLING) return 6;
        throw new IllegalArgumentException("Unregistered block: " + block);
    }

    private static Block block(int id) throws IOException {
        return switch(id) {
            case 0 -> Blocks.AIR;
            case 1 -> Blocks.GRASS;
            case 2 -> Blocks.DIRT;
            case 3 -> Blocks.STONE;
            case 4 -> Blocks.COBBLESTONE;
            case 5 -> Blocks.OAK_PLANKS;
            case 6 -> Blocks.OAK_SAPLING;
            default -> throw new IOException("Unknown block ID: " + id);
        };
    }
}
