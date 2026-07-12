package net.herecraft.client.world;

import net.herecraft.client.block.Block;
import net.herecraft.client.render.Camera;
import net.herecraft.client.render.ChunkRenderer;

import java.util.*;

public class World {
    private static final int RENDER_DISTANCE = 6;
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final Map<ChunkPos, ChunkRenderer> renderers = new HashMap<>();

    public void update(float playerX, float playerZ) {
        int playerChunkX = Math.floorDiv((int)Math.floor(playerX), Chunk.SIZE);
        int playerChunkZ = Math.floorDiv((int)Math.floor(playerZ), Chunk.SIZE);

        Set<ChunkPos> shouldBeLoaded = new HashSet<>();
        for(int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for(int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                shouldBeLoaded.add(new ChunkPos(playerChunkX + dx, playerChunkZ + dz));
            }
        }

        for(ChunkPos pos : shouldBeLoaded) {
            if(!chunks.containsKey(pos)) {
                chunks.put(pos, new Chunk(pos.x, pos.z, this));
            }
        }

        for(ChunkPos pos : shouldBeLoaded) {
            if(!renderers.containsKey(pos)) {
                renderers.put(pos, new ChunkRenderer(chunks.get(pos)));
            }
        }

        Iterator<Map.Entry<ChunkPos, Chunk>> iterator = chunks.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<ChunkPos, Chunk> entry = iterator.next();
            if(!shouldBeLoaded.contains(entry.getKey())) {
                ChunkRenderer renderer = renderers.remove(entry.getKey());
                if(renderer != null) renderer.destroy();
                iterator.remove();
            }
        }
    }

    public void breakBlock(int worldX, int worldY, int worldZ) {
        setBlock(worldX, worldY, worldZ, Block.air());
    }

    public void placeBlock(int worldX, int worldY, int worldZ, Block block) {
        setBlock(worldX, worldY, worldZ, block);
    }

    private void setBlock(int worldX, int worldY, int worldZ, Block block) {
        if(worldY < 0 || worldY >= Chunk.SIZE) {
            return;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);
        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);

        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        Chunk chunk = chunks.get(pos);
        if(chunk == null) {
            return;
        }

        chunk.setBlockLocal(localX, worldY, localZ, block);
        rebuildChunk(pos);

        if(localX == 0) rebuildChunk(new ChunkPos(chunkX - 1, chunkZ));
        if(localX == Chunk.SIZE - 1) rebuildChunk(new ChunkPos(chunkX + 1, chunkZ));
        if(localZ == 0) rebuildChunk(new ChunkPos(chunkX, chunkZ - 1));
        if(localZ == Chunk.SIZE - 1) rebuildChunk(new ChunkPos(chunkX, chunkZ + 1));
    }

    private void rebuildChunk(ChunkPos pos) {
        Chunk chunk = chunks.get(pos);
        if(chunk == null) {
            return;
        }

        ChunkRenderer oldRenderer = renderers.remove(pos);
        if(oldRenderer != null) {
            oldRenderer.destroy();
        }

        renderers.put(pos, new ChunkRenderer(chunk));
    }

    public boolean isSolidBlock(int worldX, int worldY, int worldZ) {
        if(worldY < 0 || worldY >= Chunk.SIZE) return false;

        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);

        Chunk chunk = chunks.get(new ChunkPos(chunkX, chunkZ));
        if(chunk == null) return false;

        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);

        return chunk.isSolidBlockLocal(localX, worldY, localZ);
    }

    public void render(Camera camera, float aspectRatio) {
        for(ChunkRenderer renderer : renderers.values()) {
            renderer.render(camera, aspectRatio);
        }
    }

    public void destroy() {
        for(ChunkRenderer renderer : renderers.values()) {
            renderer.destroy();
        }
        renderers.clear();
        chunks.clear();
    }
}
