package net.herecraft;

import net.herecraft.client.input.Keyboard;
import net.herecraft.client.player.Player;
import net.herecraft.client.render.ChunkRenderer;
import net.herecraft.client.render.Camera;
import net.herecraft.client.world.Chunk;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Herecraft {
    public static long window;
    private static ChunkRenderer chunkRenderer;
    private static Camera camera;
    private static Player player;
    private static float lastTime;

    public static void main(String args[]) {
        init();
        loop();
        chunkRenderer.destroy();
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

        glViewport(0, 0, vidMode.width(), vidMode.height());
        glEnable(GL_DEPTH_TEST);

        Chunk chunk = new Chunk();
        chunkRenderer = new ChunkRenderer(chunk);
        camera = new Camera(16.0f / 9.0f);
        player = new Player(camera);
        lastTime = (float)glfwGetTime();
    }

    public static void loop() {
        glClearColor(0.55f, 0.75f, 0.95f, 1.0f);
        while(!glfwWindowShouldClose(window)) {
            float currentTime = (float)glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            player.update(deltaTime);
            chunkRenderer.render(camera.viewProjection());

            Keyboard.input();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
}
