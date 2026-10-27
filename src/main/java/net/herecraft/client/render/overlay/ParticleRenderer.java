package net.herecraft.client.render.overlay;

import net.herecraft.client.render.buffer.GpuBuffer;
import net.herecraft.client.render.sync.SyncObjects;
import net.herecraft.client.world.BlockParticle;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class ParticleRenderer {
    private static final int MAX_PARTICLES = 2048;
    private static final int FLOATS_PER_VERTEX = 10;
    private static final int VERTICES_PER_PARTICLE = 36;
    private static final float MAX_PARTICLE_PIXELS = 16.0f;
    private static final float FOV_RADIANS = (float)Math.toRadians(70.0);

    private final GpuBuffer[] buffers;
    private final int[] vertexCounts;

    public ParticleRenderer(VkDevice device, VkPhysicalDevice physicalDevice) {
        int frames = SyncObjects.MAX_FRAMES_IN_FLIGHT;
        buffers = new GpuBuffer[frames];
        vertexCounts = new int[frames];
        long bufferBytes = (long)MAX_PARTICLES * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX * Float.BYTES;

        for(int  i = 0; i < frames; i++) {
            buffers[i] = new GpuBuffer(device);
            buffers[i].create(physicalDevice, bufferBytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }
    }

    public void update(List<BlockParticle> particles, int frameIndex, Vector3f cameraPosition, Vector3f cameraRight, Vector3f cameraUp, Vector3f cameraForward, float viewportHeight) {
        List<Float> data = new ArrayList<>();

        int count = Math.min(particles.size(), MAX_PARTICLES);
        for(int i = 0; i < count; i++) {
            BlockParticle particle = particles.get(i);
            Vector3f position = particle.getPosition();

            float depth = new Vector3f(position).sub(cameraPosition).dot(cameraForward);

            if(depth <= 0.1f) {
                continue;
            }

            float halfSize = MAX_PARTICLE_PIXELS * depth * (float)Math.tan(FOV_RADIANS * 0.5f) / viewportHeight;

            addParticle(data, particle, cameraRight, cameraUp, halfSize);
        }

        float[] vertices = new float[data.size()];
        for(int i = 0; i < data.size(); i++) {
            vertices[i] = data.get(i);
        }

        buffers[frameIndex].uploadData(vertices);
        vertexCounts[frameIndex] = vertices.length / FLOATS_PER_VERTEX;
    }

    private void addParticle(List<Float> out, BlockParticle particle, Vector3f cameraRight, Vector3f cameraUp, float halfSize) {
        Vector3f center = particle.getPosition();

        Vector3f right = new  Vector3f(cameraRight).mul(halfSize);
        Vector3f up = new  Vector3f(cameraUp).mul(halfSize);

        Vector3f bottomLeft = new Vector3f(center).sub(right).sub(up);
        Vector3f bottomRight = new Vector3f(center).add(right).sub(up);
        Vector3f topRight = new Vector3f(center).add(right).add(up);
        Vector3f topLeft = new Vector3f(center).sub(right).add(up);

        float[][] corners = {{bottomLeft.x, bottomLeft.y, bottomLeft.z}, {bottomRight.x, bottomRight.y, bottomRight.z}, {topRight.x, topRight.y, topRight.z}, {topLeft.x, topLeft.y, topLeft.z}};

        addQuad(out, corners, particle.getTextureLayer(), 1.0f, particle.getTextureU(), particle.getTextureV());
    }

    private void addQuad(List<Float> out, float[][] corners, int textureLayer, float shade, float textureU, float textureV) {
        int[] order = {0, 1, 2, 2, 3, 0};

        for(int index : order) {
            out.add(corners[index][0]);
            out.add(corners[index][1]);
            out.add(corners[index][2]);

            out.add(textureU);
            out.add(textureV);

            out.add(shade);
            out.add((float)textureLayer);

            out.add(1.0f);
            out.add(1.0f);
            out.add(1.0f);
        }
    }

    public GpuBuffer getBuffer(int frameIndex) {
        return buffers[frameIndex];
    }

    public int getVertexCount(int frameIndex) {
        return vertexCounts[frameIndex];
    }

    public void destroy() {
        for(GpuBuffer buffer : buffers) {
            buffer.destroy();
        }
    }
}
