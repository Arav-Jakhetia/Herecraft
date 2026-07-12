package net.herecraft;

import net.herecraft.client.block.Block;
import net.herecraft.client.input.Keyboard;
import net.herecraft.client.input.Mouse;
import net.herecraft.client.player.Player;
import net.herecraft.client.render.BlockHighlightRenderer;
import net.herecraft.client.render.Camera;
import net.herecraft.client.render.CompassRenderer;
import net.herecraft.client.render.CrosshairRenderer;
import net.herecraft.client.world.BlockHit;
import net.herecraft.client.world.Raycast;
import net.herecraft.client.world.World;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static net.herecraft.client.input.Mouse.mouseX;
import static net.herecraft.client.input.Mouse.mouseY;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class Herecraft {
    public static long window;
    private static World world;
    private static Camera camera;
    private static Player player;
    private static float lastTime;
    private static float aspectRatio;
    private static BlockHighlightRenderer blockHighlightRenderer;
    private static CrosshairRenderer crosshairRenderer;
    private static CompassRenderer compassRenderer;

    public void run() {
        init();
        loop();
        world.destroy();
        blockHighlightRenderer.destroy();
        crosshairRenderer.destroy();
        compassRenderer.destroy();
        cleanup();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit()) {
            throw new RuntimeException("Unable to Initialize GLFW");
        }

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = glfwGetVideoMode(monitor);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);

        window = glfwCreateWindow(vidMode.width(), vidMode.height(), "Herecraft", 0, 0);
        if(window == 0) {
            throw new RuntimeException("Unable to create Window");
        }

        Keyboard.init(window);
        Mouse.init(window);

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);
        glfwShowWindow(window);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glViewport(0, 0, vidMode.width(), vidMode.height());
        glEnable(GL_DEPTH_TEST);
        aspectRatio = (float)vidMode.width() / (float)vidMode.height();

        blockHighlightRenderer = new BlockHighlightRenderer();
        crosshairRenderer = new CrosshairRenderer();
        compassRenderer = new CompassRenderer();
        camera = new Camera(8, 15, 24);

        world = new World();
        world.update(8, 24);

        player = new Player(camera, new Vector3f(8, 15, 24));

        lastTime = (float)glfwGetTime();
    }

    public void loop() {
        glClearColor(0.55f, 0.75f, 0.95f, 1.0f);
        while(!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glfwPollEvents();
            Mouse.update();
            camera.setlookDir((float)(mouseX) * 0.1f, (float)(mouseY) * -0.1f);

            float currentTime = (float)glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            Keyboard.update();
            player.update(deltaTime, world);

            Vector3f position = camera.getPosition();
            world.update(position.x, position.z);

            BlockHit hit = Raycast.cast(world, position, camera.getForward(), 6.0f);

            if(hit != null) {
                if(Mouse.consumeLeftClick()) {
                    world.breakBlock(hit.x, hit.y, hit.z);
                }
                if(Mouse.consumeRightClick()) {
                    world.placeBlock(hit.faceX, hit.faceY, hit.faceZ, Block.stone());
                }
            }

            world.render(camera, aspectRatio);
            blockHighlightRenderer.render(hit, camera, aspectRatio);

            crosshairRenderer.render();
            compassRenderer.render(camera.getYaw());

            glfwSwapBuffers(window);
        }
    }

    public void cleanup() {
        glfwDestroyWindow(window);
        glfwSetErrorCallback(null).free();
    }
}