package net.herecraft.client.render;

import net.herecraft.client.world.Chunk;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ChunkRenderer {
    private final int vao;
    private final int vbo;
    private final int vertexCount;
    private final int chunkX;
    private final int chunkZ;
    private final ShaderProgram shader;
    private final TextureArray textures;

    public ChunkRenderer(Chunk chunk) {
        this.chunkX = chunk.getChunkX();
        this.chunkZ = chunk.getChunkZ();

        float vertices[] = chunk.buildMesh();
        vertexCount = vertices.length / 7;

        shader = new ShaderProgram(vertexShaderSource(), fragmentShaderSource());

        textures = new TextureArray(new String[] {
                "/assets/herecraft/textures/block/grass_block_top.png",
                "/assets/herecraft/textures/block/dirt.png",
                "/assets/herecraft/textures/block/stone.png"
        });

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = 7 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(3);
    }

    public void render(Camera camera, float aspectRatio) {
        FloatBuffer mvp = BufferUtils.createFloatBuffer(16);
        new Matrix4f()
                .perspective((float)Math.toRadians(70.0f), aspectRatio, 0.1f, 100.0f)
                .mul(camera.getMatrix())
                .translate(chunkX * Chunk.SIZE, 0, chunkZ * Chunk.SIZE)
                .get(mvp);

        shader.use();
        shader.setMatrix("uMvp", mvp);
        shader.setInt("uTextures", 0);

        glActiveTexture(GL_TEXTURE0);
        textures.bind();

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    }

    public void destroy() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        textures.destroy();
        shader.destroy();
    }

    private String vertexShaderSource() {
        return """
                #version 330 core

                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec2 aTexCoord;
                layout (location = 2) in float aShade;
                layout (location = 3) in float aTextureLayer;

                uniform mat4 uMvp;

                out vec2 texCoord;
                out float shade;
                out float textureLayer;

                void main() {
                    gl_Position = uMvp * vec4(aPos, 1.0);
                    texCoord = aTexCoord;
                    shade = aShade;
                    textureLayer = aTextureLayer;
                }
                """;
    }

    private String fragmentShaderSource() {
        return """
                #version 330 core

                in vec2 texCoord;
                in float shade;
                in float textureLayer;

                uniform sampler2DArray uTextures;

                out vec4 FragColor;

                void main() {
                    vec4 texColor = texture(uTextures, vec3(texCoord, textureLayer));
                    FragColor = vec4(texColor.rgb * shade, texColor.a);
                }
                """;
    }
}