package net.herecraft.world.chunk;

import net.herecraft.world.BlockType;
import net.herecraft.world.World;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class ChunkMesh {
    public static final float BLOCK_SIZE = 16.0f;

    private final int chunkX;
    private final int chunkZ;

    private int vertexCount;
    private float[] vertexData = new float[0];

    private int vaoId;
    private int vboId;
    private boolean uploaded;

    public ChunkMesh(World world, Chunk chunk) {
        this.chunkX = chunk.getChunkX();
        this.chunkZ = chunk.getChunkZ();
        build(world, chunk);
    }

    private void build(World world, Chunk chunk) {
        List<Float> vertices = new ArrayList<>();

        int startX = chunk.getChunkX() * Chunk.SIZE;
        int startZ = chunk.getChunkZ() * Chunk.SIZE;

        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int y = 0; y < World.HEIGHT; y++) {
                for (int lz = 0; lz < Chunk.SIZE; lz++) {
                    BlockType block = chunk.getLocal(lx, y, lz);
                    if (block == BlockType.AIR) {
                        continue;
                    }

                    float texIndex = (block == BlockType.GRASS_BLOCK) ? 0.0f : 1.0f;

                    int wx = startX + lx;
                    int wz = startZ + lz;
                    float lightLevel = isSunlit(world, wx, y, wz) ? 1.0f : 0.55f;

                    if (world.get(wx, y, wz + 1) == BlockType.AIR) addFront(vertices, wx, y, wz, texIndex, lightLevel);
                    if (world.get(wx, y, wz - 1) == BlockType.AIR) addBack(vertices, wx, y, wz, texIndex, lightLevel);
                    if (world.get(wx - 1, y, wz) == BlockType.AIR) addLeft(vertices, wx, y, wz, texIndex, lightLevel);
                    if (world.get(wx + 1, y, wz) == BlockType.AIR) addRight(vertices, wx, y, wz, texIndex, lightLevel);
                    if (world.get(wx, y + 1, wz) == BlockType.AIR) addTop(vertices, wx, y, wz, texIndex, lightLevel);
                    if (world.get(wx, y - 1, wz) == BlockType.AIR) addBottom(vertices, wx, y, wz, texIndex, lightLevel);
                }
            }
        }

        vertexCount = vertices.size() / 7;
        vertexData = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexData[i] = vertices.get(i);
        }
    }

    public void upload() {
        if (uploaded || vertexData.length == 0) {
            uploaded = true;
            return;
        }

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexData.length);
        buffer.put(vertexData).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        uploaded = true;
    }

    public void render() {
        if (!uploaded || vertexCount <= 0 || vaoId == 0) {
            return;
        }

        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public float distanceSqTo(float playerBlockX, float playerBlockZ) {
        float centerX = chunkX * Chunk.SIZE + (Chunk.SIZE * 0.5f);
        float centerZ = chunkZ * Chunk.SIZE + (Chunk.SIZE * 0.5f);
        float dx = centerX - playerBlockX;
        float dz = centerZ - playerBlockZ;
        return dx * dx + dz * dz;
    }

    public float[] getVertexData() {
        return vertexData;
    }

    public void cleanup() {
        if (vboId != 0) {
            glDeleteBuffers(vboId);
            vboId = 0;
        }
        if (vaoId != 0) {
            glDeleteVertexArrays(vaoId);
            vaoId = 0;
        }
        uploaded = false;
    }

    private void put(List<Float> v, float x, float y, float z, float u, float t, float texIndex, float lightLevel) {
        v.add(x);
        v.add(y);
        v.add(z);
        v.add(u);
        v.add(t);
        v.add(texIndex);
        v.add(lightLevel);
    }

    private void face(List<Float> v,
                      float x0, float y0, float z0,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float texIndex,
                      float lightLevel) {
        put(v, x0, y0, z0, 0f, 0f, texIndex, lightLevel);
        put(v, x1, y1, z1, 1f, 0f, texIndex, lightLevel);
        put(v, x2, y2, z2, 1f, 1f, texIndex, lightLevel);
        put(v, x2, y2, z2, 1f, 1f, texIndex, lightLevel);
        put(v, x3, y3, z3, 0f, 1f, texIndex, lightLevel);
        put(v, x0, y0, z0, 0f, 0f, texIndex, lightLevel);
    }

    private void addFront(List<Float> v, int wx, int y, int wz, float texIndex, float lightLevel) {
        float x = wx * BLOCK_SIZE, yy = y * BLOCK_SIZE, z = wz * BLOCK_SIZE, s = BLOCK_SIZE;
        face(v, x, yy, z + s, x + s, yy, z + s, x + s, yy + s, z + s, x, yy + s, z + s, texIndex, lightLevel);
    }

    private void addBack(List<Float> v, int wx, int y, int wz, float texIndex, float lightLevel) {
        float x = wx * BLOCK_SIZE, yy = y * BLOCK_SIZE, z = wz * BLOCK_SIZE, s = BLOCK_SIZE;
        face(v, x + s, yy, z, x, yy, z, x, yy + s, z, x + s, yy + s, z, texIndex, lightLevel);
    }

    private void addLeft(List<Float> v, int wx, int y, int wz, float texIndex, float lightLevel) {
        float x = wx * BLOCK_SIZE, yy = y * BLOCK_SIZE, z = wz * BLOCK_SIZE, s = BLOCK_SIZE;
        face(v, x, yy, z, x, yy, z + s, x, yy + s, z + s, x, yy + s, z, texIndex, lightLevel);
    }

    private void addRight(List<Float> v, int wx, int y, int wz, float texIndex, float lightLevel) {
        float x = wx * BLOCK_SIZE, yy = y * BLOCK_SIZE, z = wz * BLOCK_SIZE, s = BLOCK_SIZE;
        face(v, x + s, yy, z + s, x + s, yy, z, x + s, yy + s, z, x + s, yy + s, z + s, texIndex, lightLevel);
    }

    private void addTop(List<Float> v, int wx, int y, int wz, float texIndex, float lightLevel) {
        float x = wx * BLOCK_SIZE, yy = y * BLOCK_SIZE, z = wz * BLOCK_SIZE, s = BLOCK_SIZE;
        face(v, x, yy + s, z + s, x + s, yy + s, z + s, x + s, yy + s, z, x, yy + s, z, texIndex, lightLevel);
    }

    private void addBottom(List<Float> v, int wx, int y, int wz, float texIndex, float lightLevel) {
        float x = wx * BLOCK_SIZE, yy = y * BLOCK_SIZE, z = wz * BLOCK_SIZE, s = BLOCK_SIZE;
        face(v, x, yy, z, x + s, yy, z, x + s, yy, z + s, x, yy, z + s, texIndex, lightLevel);
    }

    private boolean isSunlit(World world, int wx, int wy, int wz) {
        for (int y = wy + 1; y < World.HEIGHT; y++) {
            if (world.get(wx, y, wz) != BlockType.AIR) {
                return false;
            }
        }
        return true;
    }
}
