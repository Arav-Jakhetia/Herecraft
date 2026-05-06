package net.herecraft.client.render;

import net.herecraft.client.world.Chunk;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ChunkRenderer {
    private final int vao;
    private final int vbo;
    private final int vertexCount;
    private final ShaderProgram shader;

    public ChunkRenderer(Chunk chunk) {
        float[] vertices = chunk.buildMesh();
        vertexCount = vertices.length / 6;

        shader = new ShaderProgram(vertexShaderSource(), fragmentShaderSource());

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
    }

    public void render(float[] viewProjection) {
        shader.use();
        shader.setMatrix("uMvp", viewProjection);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    }

    public void destroy() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.destroy();
    }

    private String vertexShaderSource() {
        return """
                #version 330 core

                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec3 aColor;

                uniform mat4 uMvp;
                out vec3 blockColor;

                void main() {
                    gl_Position = uMvp * vec4(aPos, 1.0);
                    blockColor = aColor;
                }
                """;
    }

    private String fragmentShaderSource() {
        return """
                #version 330 core

                in vec3 blockColor;
                out vec4 FragColor;

                void main() {
                    FragColor = vec4(blockColor, 1.0);
                }
                """;
    }
}
