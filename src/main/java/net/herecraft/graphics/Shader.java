package net.herecraft.graphics;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    public Shader(String vertexSource, String fragmentSource) {
        int vertexShaderId = compile(GL_VERTEX_SHADER, vertexSource);
        int fragmentShaderId = compile(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            throw new RuntimeException("Shader link failed: " + log);
        }

        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    private int compile(int type, String source) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderId);
            throw new RuntimeException("Shader compile failed: " + log);
        }

        return shaderId;
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setUniformMat4(String name, float[] matrix) {
        int location = getUniformLocation(name);
        if (location < 0) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            buffer.put(matrix).flip();
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    public void setUniform1i(String name, int value) {
        int location = getUniformLocation(name);
        if (location < 0) {
            return;
        }
        glUniform1i(location, value);
    }

    public void setUniform3f(String name, float x, float y, float z) {
        int location = getUniformLocation(name);
        if (location < 0) {
            return;
        }
        glUniform3f(location, x, y, z);
    }

    private int getUniformLocation(String name) {
        Integer cached = uniformLocations.get(name);
        if (cached != null) {
            return cached;
        }
        int location = glGetUniformLocation(programId, name);
        uniformLocations.put(name, location);
        return location;
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }
}
