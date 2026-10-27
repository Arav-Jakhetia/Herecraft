package net.herecraft.client.main;

import net.herecraft.world.level.block.Block;
import net.herecraft.client.input.Keyboard;
import net.herecraft.client.input.Mouse;
import net.herecraft.client.player.Player;
import net.herecraft.client.render.Camera;
import net.herecraft.client.render.RenderContext;
import net.herecraft.client.ui.MainMenu;
import net.herecraft.client.ui.WorldSelectMenu;
import net.herecraft.client.world.BlockHit;
import net.herecraft.client.world.Raycast;
import net.herecraft.client.world.World;
import net.herecraft.world.level.block.Blocks;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

import static net.herecraft.client.input.Mouse.mouseX;
import static net.herecraft.client.input.Mouse.mouseY;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;

public class Main {
    public static long window;
    private static World world;
    private static Camera camera;
    private static Player player;
    private static float lastTime;
    private static float aspectRatio;
    private static RenderContext renderContext;
    private static Block selectedBlock = Blocks.DIRT;
    private static boolean debugVisible;
    private static int fps;
    private static int frames;
    private static double fpsTimer;
    private static final String GAME_VERSION = "0.0.2a";
    private final java.io.File worldFolder;

    public static void main(String args[]) {
        if(!MainMenu.show()) {
            return;
        }

        java.io.File worldFolder = WorldSelectMenu.show();
        if(worldFolder != null) {
            new Main(worldFolder).run();
        }
    }

    public Main(java.io.File worldFolder) {
        this.worldFolder = worldFolder;
    }

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

        world = new World(worldFolder);
        world.initVulkan(renderContext.getDevice(), renderContext.getPhysicalDevice());
        world.update(8, 24, 0.0f);

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
            if(Keyboard.isDown(GLFW_KEY_F3)) {
                debugVisible = !debugVisible;
            }

            frames++;
            fpsTimer += deltaTime;

            if(fpsTimer >= 1.0) {
                fps = frames;
                frames = 0;
                fpsTimer -= 1.0;
            }

            if(Keyboard.isDown(GLFW_KEY_1)) {
                selectedBlock = Blocks.DIRT;
            }
            if(Keyboard.isDown(GLFW_KEY_2)) {
                selectedBlock = Blocks.STONE;
            }
            if(Keyboard.isDown(GLFW_KEY_3)) {
                selectedBlock = Blocks.COBBLESTONE;
            }
            if(Keyboard.isDown(GLFW_KEY_4)) {
                selectedBlock = Blocks.OAK_PLANKS;
            }
            if(Keyboard.isDown(GLFW_KEY_5)) {
                selectedBlock = Blocks.OAK_SAPLING;
            }

            player.update(deltaTime, world);

            Vector3f position = camera.getPosition();
            world.update(position.x, position.z, deltaTime);

            BlockHit hit = Raycast.cast(world, position, camera.getForward(), 6.0f);

            if(hit != null) {
                if(Mouse.consumeLeftClick()) {
                    world.breakBlock(hit.x, hit.y, hit.z);
                }
                if(Mouse.consumeRightClick()) {
                    if(!player.intersectsBlock(hit.faceX,  hit.faceY, hit.faceZ)) {
                        world.placeBlock(hit.faceX, hit.faceY, hit.faceZ, selectedBlock);
                    }
                }
            }

            Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70.0f), aspectRatio, 0.1f, 100.0f, true);
            projection.m11(projection.m11() * -1);

            Matrix4f viewProjection = new Matrix4f(projection).mul(camera.getMatrix());

            float[] mvpArray = new float[16];
            viewProjection.get(mvpArray);

            float[] highlightMvpArray = null;
            if(hit != null) {
                Matrix4f highlightMvp = new Matrix4f(viewProjection).translate(hit.x, hit.y, hit.z);

                highlightMvpArray = new  float[16];
                highlightMvp.get(highlightMvpArray);
            }

            Vector3f cameraRight = camera.getRight();
            Vector3f cameraForward = camera.getForward();
            Vector3f cameraUp = new Vector3f(cameraRight).cross(cameraForward).normalize();

            renderContext.drawFrame(debugVisible
                    ? "HERECRAFT 0.0.2a\nFPS: " + fps + "\nCHUNK UPDATES: " + world.getChunkUpdateCount()
                    : "", mvpArray, highlightMvpArray, camera.getPosition(), cameraRight, cameraUp, cameraForward);

            java.io.File preview = new java.io.File(worldFolder, "icon.png");
            if(!preview.exists()) {
                try {
                    java.awt.Rectangle screen = new java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
                    BufferedImage screenshot = new Robot().createScreenCapture(screen);
                    ImageIO.write(screenshot, "png", preview);
                } catch(Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public void cleanup() {
        if(world != null) {
            world.save();
        }
        glfwDestroyWindow(window);
        glfwSetErrorCallback(null).free();
    }
}