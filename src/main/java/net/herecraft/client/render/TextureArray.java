package net.herecraft.client.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL12.glTexSubImage3D;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.stb.STBImage.*;

public class TextureArray {
    private final int id;

    public TextureArray(String paths[]) {
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, 16, 16, paths.length, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);

        for(int i = 0; i < paths.length; i++) {
            ByteBuffer image = loadImage(paths[i]);

            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, 16, 16, 1, GL_RGBA, GL_UNSIGNED_BYTE, image);

            stbi_image_free(image);
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);
    }

    public void destroy() {
        glDeleteTextures(id);
    }

    private ByteBuffer loadImage(String path) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer fileBuffer = readResource(path);

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = stbi_load_from_memory(fileBuffer, width, height, channels, 4);

            if(image == null) {
                throw new RuntimeException("Failed to load texture: " + path);
            }

            if(width.get(0) != 16 || height.get(0) != 16) {
                throw new RuntimeException("Texture must be 16x16: " + path);
            }

            return image;
        }
    }

    private ByteBuffer readResource(String path) {
        try(InputStream stream = TextureArray.class.getResourceAsStream(path)) {
            if(stream == null) {
                throw new RuntimeException("Missing resource: " + path);
            }

            byte bytes[] = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch(IOException e) {
            throw new RuntimeException("Could not read resource: " + path, e);
        }
    }
}
