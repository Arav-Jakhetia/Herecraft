package net.herecraft.client.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class CrosshairRenderer {
    private final int vao;
    private final int vbo;
    private final ShaderProgram shader;

    public CrosshairRenderer() {
        float size = 0.015f;
        float gap = 0.004f;

        float[] vertices = {
                -size, 0.0f,
                -gap, 0.0f,

                gap, 0.0f,
                size, 0.0f,

                0.0f, -size,
                0.0f, -gap,

                0.0f, gap,
                0.0f, size
        };

        shader = new ShaderProgram(vertexShaderSource(), fragmentShaderSource());

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
    }

    public void render() {
        shader.use();

        glDisable(GL_DEPTH_TEST);
        glLineWidth(2.0f);

        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, 8);

        glEnable(GL_DEPTH_TEST);
    }

    public void destroy() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.destroy();
    }

    private String vertexShaderSource() {
        return """
                #version 330 core

                layout (location = 0) in vec2 aPos;

                void main() {
                    gl_Position = vec4(aPos, 0.0, 1.0);
                }
                """;
    }

    private String fragmentShaderSource() {
        return """
                #version 330 core

                out vec4 FragColor;

                void main() {
                    FragColor = vec4(0.0, 0.0, 0.0, 1.0);
                }
                """;
    }
}