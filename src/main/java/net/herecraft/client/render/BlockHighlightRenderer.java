package net.herecraft.client.render;

import net.herecraft.client.world.BlockHit;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BlockHighlightRenderer {
    private final int vao;
    private final int vbo;
    private final ShaderProgram shader;

    public BlockHighlightRenderer() {
        float s = 0.002f;

        float[] vertices = {
                -s, -s, -s,  1 + s, -s, -s,
                1 + s, -s, -s,  1 + s, 1 + s, -s,
                1 + s, 1 + s, -s,  -s, 1 + s, -s,
                -s, 1 + s, -s,  -s, -s, -s,

                -s, -s, 1 + s,  1 + s, -s, 1 + s,
                1 + s, -s, 1 + s,  1 + s, 1 + s, 1 + s,
                1 + s, 1 + s, 1 + s,  -s, 1 + s, 1 + s,
                -s, 1 + s, 1 + s,  -s, -s, 1 + s,

                -s, -s, -s,  -s, -s, 1 + s,
                1 + s, -s, -s,  1 + s, -s, 1 + s,
                1 + s, 1 + s, -s,  1 + s, 1 + s, 1 + s,
                -s, 1 + s, -s,  -s, 1 + s, 1 + s
        };

        shader = new ShaderProgram(vertexShaderSource(), fragmentShaderSource());

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
    }

    public void render(BlockHit hit, Camera camera, float aspectRatio) {
        if(hit == null) {
            return;
        }

        FloatBuffer mvp = BufferUtils.createFloatBuffer(16);

        Matrix4f projection = new Matrix4f()
                .perspective((float)Math.toRadians(70.0f), aspectRatio, 0.1f, 100.0f);

        Matrix4f view = camera.getMatrix();

        Matrix4f model = new Matrix4f()
                .translate(hit.x, hit.y, hit.z);

        projection.mul(view).mul(model).get(mvp);

        shader.use();
        shader.setMatrix("uMvp", mvp);

        glDisable(GL_CULL_FACE);
        glLineWidth(2.0f);

        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, 24);
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

                uniform mat4 uMvp;

                void main() {
                    gl_Position = uMvp * vec4(aPos, 1.0);
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