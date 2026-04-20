package net.herecraft;

import net.herecraft.client.input.Keyboard;
import net.herecraft.client.block.Block;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class Herecraft {
    public static long window;
    static int shaderProgram;
    static int vao;
    static int vbo;

    public static void main(String args[]) {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwSetErrorCallback(null).free();
    }

    public static void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit()) {
            throw new RuntimeException("Unable to Initialize GLFW");
        }

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = glfwGetVideoMode(monitor);

        window = glfwCreateWindow(vidMode.width(), vidMode.height(), "Herecraft", 0, 0);
        if(window == 0) {
            throw new RuntimeException("Unable to create Window");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);
        glfwShowWindow(window);

        glEnable(GL_DEPTH_TEST);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        float vertices[] = {
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,

                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, -0.5f, 0.5f,

                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,

                0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,

                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f,

                -0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f
        };
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        String vertexShaderSource = """
                #version 330 core
                
                layout (location = 0) in vec3 aPos;
                
                void main() {
                    gl_Position = vec4(aPos, 1.0);
                }
                """;

        String fragmentShaderSource = """
                #version 330 core
                
                out vec4 FragColor;
                
                void main() {
                    FragColor = vec4(0.0, 0.0, 1.0, 1.0);
                }
                """;

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public static void loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        while(!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glUseProgram(shaderProgram);
            glBindVertexArray(vao);

            glDrawArrays(GL_TRIANGLES, 0, 36);

            Keyboard.input();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
}
