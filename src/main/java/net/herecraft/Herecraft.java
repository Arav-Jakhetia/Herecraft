package net.herecraft;

import net.herecraft.client.block.Block;
import net.herecraft.client.input.Keyboard;
import net.herecraft.client.input.Mouse;
import net.herecraft.client.player.Player;
import net.herecraft.client.render.*;
import net.herecraft.client.render.RenderContext;
import net.herecraft.client.render.overlay.BlockHighlightRenderer;
import net.herecraft.client.render.overlay.CrosshairRenderer;
import net.herecraft.client.world.BlockHit;
import net.herecraft.client.world.Raycast;
import net.herecraft.client.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;

import static net.herecraft.client.input.Mouse.mouseX;
import static net.herecraft.client.input.Mouse.mouseY;
import static org.lwjgl.glfw.GLFW.*;

public class Herecraft {
    public static long window;
    private static World world;
    private static Camera camera;
    private static Player player;
    private static float lastTime;
    private static float aspectRatio;
    private static BlockHighlightRenderer blockHighlightRenderer;
    private static CrosshairRenderer crosshairRenderer;

    private static RenderContext renderContext;

    public void run() {
        init();
        loop();
        cleanup();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit()) {
            throw new RuntimeException("Unable to Initialize GLFW");
        }

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = glfwGetVideoMode(monitor);

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);

        window = glfwCreateWindow(vidMode.width(), vidMode.height(), "Herecraft", 0, 0);
        if(window == 0) {
            throw new RuntimeException("Unable to create Window");
        }

        Keyboard.init(window);
        Mouse.init(window);

        glfwShowWindow(window);

        renderContext = new RenderContext();
        renderContext.init(window, vidMode.width(), vidMode.height());

        world = new World();
        world.initVulkan(renderContext.getDevice(), renderContext.getPhysicalDevice());
        world.update(8, 24);

        int spawnY = world.getGroundHeight(8, 24) + 1;
        camera = new Camera(8, spawnY, 24);
        renderContext.setWorld(world);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        aspectRatio = (float)vidMode.width() / (float)vidMode.height();

        player = new Player(camera, new Vector3f(8, spawnY, 24));

        lastTime = (float)glfwGetTime();
    }

    public void loop() {
        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            Mouse.update();
            camera.setlookDir((float)(mouseX) * 0.1f, (float)(mouseY) * -0.1f);

            float currentTime = (float)glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            if(deltaTime > 0.1f) {
                deltaTime = 0.1f;
            }

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

            Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70.0f), aspectRatio, 0.1f, 100.0f, true);
            projection.m11(projection.m11() * -1);

            Matrix4f mvp = projection.mul(camera.getMatrix());

            float[] mvpArray = new float[16];
            mvp.get(mvpArray);

            float[] highlightMvpArray = null;
            if(hit != null) {
                Matrix4f highlightMvp = new Matrix4f(projection)
                        .mul(camera.getMatrix())
                        .translate(hit.x, hit.y, hit.z);
                highlightMvpArray = new float[16];
                highlightMvp.get(highlightMvpArray);
            }

            renderContext.drawFrame(mvpArray, highlightMvpArray);
        }
    }

    public void cleanup() {
        glfwDestroyWindow(window);
        glfwSetErrorCallback(null).free();
    }
}