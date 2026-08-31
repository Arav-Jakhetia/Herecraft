package net.herecraft.client.render.overlay;


import net.herecraft.client.render.buffer.GpuBuffer;
import net.herecraft.client.world.Chunk;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.vulkan.VK10.*;

public class ChunkRenderer {
    private final GpuBuffer vertexBuffer;
    private final int vertexCount;
    private final int chunkX;
    private final int chunkZ;

    public ChunkRenderer(VkDevice device, VkPhysicalDevice physicalDevice, Chunk chunk) {
        this.chunkX = chunk.getChunkX();
        this.chunkZ = chunk.getChunkZ();

        float[] meshData = chunk.buildMesh();
        vertexCount = meshData.length / 7;

        vertexBuffer = new GpuBuffer(device);

        if(vertexCount > 0) {
            vertexBuffer.create(
                    physicalDevice,
                    (long)meshData.length * Float.BYTES,
                    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                    );
            vertexBuffer.uploadData(meshData);
        }
    }

    public GpuBuffer getVertexBuffer() {
        return vertexBuffer;
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

    public void destroy() {
        if(vertexCount > 0) {
            vertexBuffer.destroy();
        }
    }
}
