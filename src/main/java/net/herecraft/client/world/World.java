package net.herecraft.client.world;

import net.herecraft.client.block.Block;
import net.herecraft.client.render.overlay.ChunkRenderer;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.*;

public class World {
    private static final int RENDER_DISTANCE = 6;
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final Map<ChunkPos, ChunkRenderer> renderers = new HashMap<>();
    private VkDevice device;
    private VkPhysicalDevice physicalDevice;
    private final GrassColorMap grassColorMap = new  GrassColorMap();
    private static final float GRASS_TICK_INTERVAL = 1.0f;
    private float grassTickTimer;
    private final Random random = new Random();
    private final List<BlockParticle> particles = new ArrayList<>();

    public void initVulkan(VkDevice device, VkPhysicalDevice physicalDevice) {
        this.device = device;
        this.physicalDevice = physicalDevice;
    }

    public void update(float playerX, float playerZ, float deltaTime) {
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
                renderers.put(pos, new ChunkRenderer(device, physicalDevice, chunks.get(pos)));
            }
        }

        Iterator<Map.Entry<ChunkPos, Chunk>> iterator = chunks.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<ChunkPos, Chunk> entry = iterator.next();
            if(!shouldBeLoaded.contains(entry.getKey())) {
                ChunkRenderer renderer = renderers.remove(entry.getKey());
                if(renderer != null) {
                    renderer.destroy();
                }
                iterator.remove();
            }
        }

        Iterator<BlockParticle> particleIterator = particles.iterator();
        while(particleIterator.hasNext()) {
            BlockParticle particle = particleIterator.next();
            particle.update(deltaTime, this);

            if(particle.isExpired()) {
                particleIterator.remove();
            }
        }

        grassTickTimer += deltaTime;

        if(grassTickTimer >= GRASS_TICK_INTERVAL) {
            grassTickTimer -=  GRASS_TICK_INTERVAL;
            randomTickGrass();
        }
    }

    private void randomTickGrass() {
        if(chunks.isEmpty()) {
            return;
        }

        List<Chunk> loadedChunks = new ArrayList<>(chunks.values());

        for(int i = 0; i < 3; i++) {
            Chunk chunk = loadedChunks.get(random.nextInt(loadedChunks.size()));
            int localX = random.nextInt(Chunk.SIZE);
            int localY = random.nextInt(Chunk.SIZE);
            int localZ = random.nextInt(Chunk.SIZE);

            Block block = chunk.getBlockLocal(localX, localY, localZ);

            int worldX = chunk.getChunkX() * Chunk.SIZE + localX;
            int worldZ = chunk.getChunkZ() * Chunk.SIZE + localZ;

            boolean covered = isSolidBlock(worldX, localY + 1, worldZ);

            if(block.isGrass()) {
                if(covered) {
                    setBlock(worldX, localY, worldZ, Block.dirt());
                }
                continue;
            }

            if(!block.isDirt() || covered) {
                continue;
            }

            if(isSolidBlock(worldX, localY + 1, worldZ)) {
                continue;
            }

            boolean nearGrass = isGrassBlock(worldX - 1, localY, worldZ) ||isGrassBlock(worldX + 1, localY, worldZ) ||isGrassBlock(worldX, localY, worldZ - 1) ||isGrassBlock(worldX, localY, worldZ + 1);

            if(nearGrass) {
                setBlock(worldX, localY, worldZ, Block.grass());
            }
        }
    }

    private boolean isGrassBlock(int worldX, int worldY, int worldZ) {
        if(worldY < 0 || worldY >= Chunk.SIZE) {
            return false;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);

        Chunk chunk = chunks.get(new ChunkPos(chunkX, chunkZ));
        if(chunk == null) {
            return false;
        }

        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);

        return chunk.getBlockLocal(localX, worldY, localZ).isGrass();
    }

    public void breakBlock(int worldX, int worldY, int worldZ) {
        if(worldY < 0 || worldY >= Chunk.SIZE) {
            return;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);
        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);

        Chunk chunk = chunks.get(new ChunkPos(chunkX, chunkZ));
        if(chunk == null) {
            return;
        }

        Block brokenBlock = chunk.getBlockLocal(localX, worldY, localZ);
        if(!brokenBlock.isSolid()) {
            return;
        }

        for(int i = 0; i < 64; i++) {
            Vector3f position = new Vector3f(worldX + 0.15f + random.nextFloat() * 0.7f, worldY + 0.15f + random.nextFloat() * 0.7f, worldZ + 0.15f + random.nextFloat() * 0.7f);
            Vector3f velocity = new Vector3f((random.nextFloat() - 0.5f) * 4.0f, 1.0f + random.nextFloat() * 4.0f, (random.nextFloat() - 0.5f) * 4.0f);

            particles.add(new BlockParticle(brokenBlock, position, velocity, 0.12f + random.nextFloat() * 0.12f));
        }

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

        renderers.put(pos, new ChunkRenderer(device, physicalDevice, chunk));
    }

    public int getGroundHeight(int worldX, int worldZ) {
        for(int y = Chunk.SIZE - 1; y >= 0; y--) {
            if(isSolidBlock(worldX, y, worldZ)) {
                return y + 1;
            }
        }
        return 0;
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

    public Collection<ChunkRenderer> getChunkRenderers() {
        return renderers.values();
    }

    public Vector3f getGrassTint(int worldX, int worldZ) {
        return grassColorMap.sample(0.8f, 0.4f);
    }

    public List<BlockParticle> getParticles() {
        return Collections.unmodifiableList(particles);
    }

    public void destroy() {
        for(ChunkRenderer renderer : renderers.values()) {
            renderer.destroy();
        }
        renderers.clear();
        chunks.clear();
    }
}
