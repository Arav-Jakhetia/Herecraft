package net.herecraft.client.render;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);

        if(glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteProgram(programId);
            throw new ShaderException("Shader link failed:\n" + log);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setMatrix(String name, float matrix[]) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        buffer.put(matrix).flip();
        glUniformMatrix4fv(glGetUniformLocation(programId, name), false, buffer);
    }

    public void setMatrix(String name, FloatBuffer matrix) {
        glUniformMatrix4fv(glGetUniformLocation(programId, name), false, matrix);
    }

    public void destroy() {
        glDeleteProgram(programId);
    }

    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if(glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new ShaderException("Shader compile failed:\n" + log);
        }

        return shader;
    }

    public void setInt(String name, int value) {
        glUniform1i(glGetUniformLocation(programId, name), value);
    }
}
