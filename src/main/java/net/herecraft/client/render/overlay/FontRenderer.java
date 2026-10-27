package net.herecraft.client.render.overlay;

import net.herecraft.client.render.buffer.GpuBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class FontRenderer {
    private static final int MAX_CHARACTERS = 128;
    private static final float PIXEL_SIZE = 2.0f;
    private static final String[][] GLYPHS =  {
            {"01110","10001","10001","11111","10001","10001","10001"}, // A
            {"11110","10001","10001","11110","10001","10001","11110"}, // B
            {"01111","10000","10000","10000","10000","10000","01111"}, // C
            {"11110","10001","10001","10001","10001","10001","11110"}, // D
            {"11111","10000","10000","11110","10000","10000","11111"}, // E
            {"11111","10000","10000","11110","10000","10000","10000"}, // F
            {"01111","10000","10000","10111","10001","10001","01111"}, // G
            {"10001","10001","10001","11111","10001","10001","10001"}, // H
            {"11111","00100","00100","00100","00100","00100","11111"}, // I
            {"00111","00010","00010","00010","10010","10010","01100"}, // J
            {"10001","10010","10100","11000","10100","10010","10001"}, // K
            {"10000","10000","10000","10000","10000","10000","11111"}, // L
            {"10001","11011","10101","10101","10001","10001","10001"}, // M
            {"10001","11001","10101","10011","10001","10001","10001"}, // N
            {"01110","10001","10001","10001","10001","10001","01110"}, // O
            {"11110","10001","10001","11110","10000","10000","10000"}, // P
            {"01110","10001","10001","10001","10101","10010","01101"}, // Q
            {"11110","10001","10001","11110","10100","10010","10001"}, // R
            {"01111","10000","10000","01110","00001","00001","11110"}, // S
            {"11111","00100","00100","00100","00100","00100","00100"}, // T
            {"10001","10001","10001","10001","10001","10001","01110"}, // U
            {"10001","10001","10001","10001","10001","01010","00100"}, // V
            {"10001","10001","10001","10101","10101","10101","01010"}, // W
            {"10001","10001","01010","00100","01010","10001","10001"}, // X
            {"10001","10001","01010","00100","00100","00100","00100"}, // Y
            {"11111","00001","00010","00100","01000","10000","11111"}, // Z
            {"01110","10001","10011","10101","11001","10001","01110"}, // 0
            {"00100","01100","00100","00100","00100","00100","01110"}, // 1
            {"01110","10001","00001","00010","00100","01000","11111"}, // 2
            {"11110","00001","00001","01110","00001","00001","11110"}, // 3
            {"00010","00110","01010","10010","11111","00010","00010"}, // 4
            {"11111","10000","10000","11110","00001","00001","11110"}, // 5
            {"01110","10000","10000","11110","10001","10001","01110"}, // 6
            {"11111","00001","00010","00100","01000","01000","01000"}, // 7
            {"01110","10001","10001","01110","10001","10001","01110"}, // 8
            {"01110","10001","10001","01111","00001","00001","01110"}, // 9
            {"00000","00000","00000","00000","00000","00110","00110"}, // .
            {"00000","00100","00100","00000","00100","00100","00000"}, // :
            {"00000","00000","00000","11111","00000","00000","00000"}  // -
    };
    private final GpuBuffer[] buffers = new GpuBuffer[2];
    private final int[] vertexCounts = new int[2];
    private final String[] cachedText = new String[2];

    public FontRenderer(VkDevice device, VkPhysicalDevice physicalDevice) {
        int maxFloats = MAX_CHARACTERS * 35 * 2 * 5;

        for(int i = 0; i < buffers.length; i++) {
            buffers[i] = new GpuBuffer(device);
            buffers[i].create(physicalDevice, (long)maxFloats * Float.BYTES, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }
    }

    public void update(String text, int frame, int width, int height) {
        if(text.equals(cachedText[frame])) return;
        cachedText[frame] = text;

        List<Float> vertices = new ArrayList<>();
        int x = 8;
        int y = 8;
        int startX = x;

        for(char original : text.toUpperCase().toCharArray()) {
            if(original == '\n') {
                x = startX;
                y += 18;
                continue;
            }

            String[] glyph = getGlyph(original);
            for(int row = 0; row < 7; row++) {
                for(int col = 0; col < 5; col++) {
                    if(glyph[row].charAt(col) == '1') {
                        addPixelLine(vertices, x + col * PIXEL_SIZE, y + row * PIXEL_SIZE, width, height);
                    }
                }
            }

            x += 12;
            if(x > width - 12) break;
        }

        float[] data = new float[vertices.size()];
        for(int i = 0; i < data.length; i++) data[i] = vertices.get(i);

        buffers[frame].uploadData(data);
        vertexCounts[frame] = data.length / 5;
    }

    private void addPixelLine(List<Float> vertices, float x, float y, int width, int height) {
        float x1 = -1.0f + 2.0f * x / width;
        float y1 = -1.0f + 2.0f * y / height;
        float x2 = -1.0f + 2.0f * (x + PIXEL_SIZE) / width;

        vertices.add(x1);
        vertices.add(y1);
        vertices.add(1.0f);
        vertices.add(1.0f);
        vertices.add(1.0f);
        vertices.add(x2);
        vertices.add(y1);
        vertices.add(1.0f);
        vertices.add(1.0f);
        vertices.add(1.0f);
    }

    private String[] getGlyph(char c) {
        if(c >= 'A' && c <= 'Z') return GLYPHS[c - 'A'];
        if(c >= '0' && c <= '9') return GLYPHS[26 + c - '0'];
        if(c == '.') return GLYPHS[36];
        if(c == ':') return GLYPHS[37];
        if(c == '-') return GLYPHS[38];
        return new String[] {"00000","00000","00000","00000","00000","00000","00000"};
    }

    public GpuBuffer getBuffer(int frame) {
        return buffers[frame];
    }

    public int getVertexCount(int frame) {
        return vertexCounts[frame];
    }

    public void destroy() {
        for(GpuBuffer buffer : buffers) buffer.destroy();
    }
}
