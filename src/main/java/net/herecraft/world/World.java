package net.herecraft.world;

import net.herecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class World {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 64;
    public static final int DEPTH = 256;

    public static final int MIN_X = -WIDTH / 2;
    public static final int MAX_X = MIN_X + WIDTH - 1;
    public static final int MIN_Z = -DEPTH / 2;
    public static final int MAX_Z = MIN_Z + DEPTH - 1;

    public static final int CHUNK_SIZE = Chunk.SIZE;
    public static final int CHUNKS_X = (WIDTH + CHUNK_SIZE - 1) / CHUNK_SIZE;
    public static final int CHUNKS_Z = (DEPTH + CHUNK_SIZE - 1) / CHUNK_SIZE;

    private final Chunk[][] chunks = new Chunk[CHUNKS_X][CHUNKS_Z];
    private final List<Chunk> chunkList = new ArrayList<>();

    public World() {
        generate();
    }

    private void generate() {
        chunkList.clear();

        int firstChunkX = Math.floorDiv(MIN_X, CHUNK_SIZE);
        int firstChunkZ = Math.floorDiv(MIN_Z, CHUNK_SIZE);

        for (int cx = 0; cx < CHUNKS_X; cx++) {
            for (int cz = 0; cz < CHUNKS_Z; cz++) {
                int worldChunkX = firstChunkX + cx;
                int worldChunkZ = firstChunkZ + cz;

                Chunk chunk = new Chunk(worldChunkX, worldChunkZ);
                chunk.generate();
                chunks[cx][cz] = chunk;
                chunkList.add(chunk);
            }
        }
    }

    public BlockType get(int x, int y, int z) {
        if (x < MIN_X || x > MAX_X || y < 0 || y >= HEIGHT || z < MIN_Z || z > MAX_Z) {
            return BlockType.AIR;
        }

        int cx = (x - MIN_X) / CHUNK_SIZE;
        int cz = (z - MIN_Z) / CHUNK_SIZE;
        int lx = (x - MIN_X) % CHUNK_SIZE;
        int lz = (z - MIN_Z) % CHUNK_SIZE;

        Chunk chunk = chunks[cx][cz];
        if (chunk == null) {
            return BlockType.AIR;
        }

        return chunk.getLocal(lx, y, lz);
    }

    public int getTopSolidY(int x, int z) {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            if (get(x, y, z) != BlockType.AIR) {
                return y;
            }
        }
        return -1;
    }

    public List<Chunk> getChunks() {
        return Collections.unmodifiableList(chunkList);
    }
}
