package net.herecraft.client.render;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

    public class CompassRenderer {
        private static final float VIEW_RANGE_DEGREES = 90.0f;
        private static final float BAR_HALF_WIDTH = 0.3f;
        private static final float BAR_Y = 0.88f;
        private static final float TICK_HEIGHT = 0.02f;

        private final int vao;
        private final int vbo;
        private final ShaderProgram shader;

        public CompassRenderer() {
            shader = new ShaderProgram(vertexShaderSource(), fragmentShaderSource());

            vao = glGenVertexArrays();
            vbo = glGenBuffers();

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, 12L * 5 * Float.BYTES, GL_DYNAMIC_DRAW);

            int stride = 5 * Float.BYTES;
            glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 2L * Float.BYTES);
            glEnableVertexAttribArray(1);
        }

        public void render(float yaw) {
            float normalizedYaw = normalizeAngle(yaw);

            FloatBuffer data = BufferUtils.createFloatBuffer(12 * 5);

            addVertex(data, -BAR_HALF_WIDTH, BAR_Y, 1, 1, 1);
            addVertex(data, BAR_HALF_WIDTH, BAR_Y, 1, 1, 1);
            int vertexCount = 2;

            vertexCount += addCardinalTick(data, 0.0f, normalizedYaw, 1.0f, 0.2f, 0.2f);
            vertexCount += addCardinalTick(data, 90.0f, normalizedYaw, 0.3f, 1.0f, 0.3f);
            vertexCount += addCardinalTick(data, 180.0f, normalizedYaw, 0.4f, 0.6f, 1.0f);
            vertexCount += addCardinalTick(data, 270.0f, normalizedYaw, 1.0f, 1.0f, 0.3f);

            addVertex(data, 0.0f, BAR_Y - TICK_HEIGHT * 1.5f, 1, 1, 1);
            addVertex(data, 0.0f, BAR_Y + TICK_HEIGHT * 1.5f, 1, 1, 1);
            vertexCount += 2;

            data.flip();

            shader.use();

            glDisable(GL_DEPTH_TEST);
            glLineWidth(2.0f);

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, data);

            glDrawArrays(GL_LINES, 0, vertexCount);

            glEnable(GL_DEPTH_TEST);
        }

        private int addCardinalTick(FloatBuffer data, float cardinalAngle, float yaw, float r, float g, float b) {
            float diff = angleDifference(cardinalAngle, yaw);
            float halfRange = VIEW_RANGE_DEGREES / 2.0f;

            if(Math.abs(diff) > halfRange) {
                return 0;
            }

            float t = diff / halfRange;
            float x = t * BAR_HALF_WIDTH;

            addVertex(data, x, BAR_Y - TICK_HEIGHT, r, g, b);
            addVertex(data, x, BAR_Y + TICK_HEIGHT, r, g, b);
            return 2;
        }

        private void addVertex(FloatBuffer data, float x, float y, float r, float g, float b) {
            data.put(x).put(y).put(r).put(g).put(b);
        }

        private float normalizeAngle(float angle) {
            float result = angle % 360.0f;
            if(result < 0) {
                result += 360.0f;
            }
            return result;
        }

        private float angleDifference(float cardinalAngle, float yaw) {
            float diff = (cardinalAngle - yaw) % 360.0f;
            if(diff > 180.0f) diff -= 360.0f;
            if(diff < -180.0f) diff += 360.0f;
            return diff;
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
                layout (location = 1) in vec3 aColor;

                out vec3 vColor;

                void main() {
                    gl_Position = vec4(aPos, 0.0, 1.0);
                    vColor = aColor;
                }
                """;
        }

        private String fragmentShaderSource() {
            return """
                #version 330 core

                in vec3 vColor;
                out vec4 FragColor;

                void main() {
                    FragColor = vec4(vColor, 1.0);
                }
                """;
        }
    }
