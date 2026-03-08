package net.herecraft.client;

import net.herecraft.client.resources.ClasspathResourceManager;
import net.herecraft.client.resources.GrassColorReloadListener;
import net.herecraft.client.renderer.texture.TextureManager;
import net.herecraft.graphics.Shader;
import net.herecraft.world.BlockType;
import net.herecraft.world.ColorizerGrass;
import net.herecraft.world.World;
import net.herecraft.world.WorldMesh;
import org.lwjgl.BufferUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glReadPixels;

public class Game {
    private Window window;
    private Shader shader;

    private TextureManager grassTexture;
    private TextureManager cobbleTexture;

    private final String vertexShaderSource = """
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in float aTexIndex;
layout (location = 3) in float aLightLevel;

uniform mat4 mvp;

out vec2 TexCoord;
flat out int TexIndex;
out float LightLevel;
out vec3 WorldPos;

void main() {
    gl_Position = mvp * vec4(aPos, 1.0);
    TexCoord = aTexCoord;
    TexIndex = int(aTexIndex + 0.5);
    LightLevel = aLightLevel;
    WorldPos = aPos;
}
""";

    private final String fragmentShaderSource = """
#version 330 core
in vec2 TexCoord;
flat in int TexIndex;
in float LightLevel;
in vec3 WorldPos;
out vec4 FragColor;

uniform sampler2D texGrass;
uniform sampler2D texCobble;
uniform vec3 grassTint;
uniform vec3 cameraPos;

void main() {
    vec4 base;
    if (TexIndex == 0) {
        vec4 c = texture(texGrass, TexCoord);
        base = vec4(c.rgb * grassTint, c.a);
    } else {
        base = texture(texCobble, TexCoord);
    }

    vec3 lit = base.rgb * LightLevel;
    if (LightLevel < 0.999) {
        float dist = distance(cameraPos, WorldPos);
        float fog = smoothstep(240.0, 1100.0, dist);
        lit = mix(lit, vec3(0.0), fog);
    }

    FragColor = vec4(lit, base.a);
}
""";

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private final Matrix4f mvp = new Matrix4f();
    private final Matrix4f view = new Matrix4f();

    private World world;
    private WorldMesh worldMesh;

    private final Vector3f cameraPos = new Vector3f();
    private final Vector3f cameraFront = new Vector3f(0f, 0f, -1f);
    private final Vector3f cameraUp = new Vector3f(0f, 1f, 0f);
    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraTarget = new Vector3f();
    private final Vector3f moveDir = new Vector3f();
    private final Vector3f grassTintColor = new Vector3f(1.0f, 1.0f, 1.0f);

    private float yaw = 90.0f;
    private float pitch = -20.0f;

    private static final float MOVE_SPEED = 0.5f;
    private static final float MOUSE_SENSITIVITY = 0.12f;

    private float velocityY = 0.0f;
    private boolean onGround = false;
    private boolean respawnKeyDownLastFrame = false;
    private boolean screenshotKeyDownLastFrame = false;
    private boolean screenshotRequested = false;

    private static final float GRAVITY = 0.06f;
    private static final float TERMINAL_VELOCITY = -3.0f;
    private static final float JUMP_VELOCITY = 1.2f;
    private static final float BLOCK_SIZE = 16.0f;

    private static final float PLAYER_RADIUS = 4.0f;
    private static final float PLAYER_HEIGHT = 28.0f;
    private static final float PLAYER_EYE_OFFSET = 24.0f;
    private static final float VOID_Y = -320.0f;

    private static final int LOAD_RADIUS_CHUNKS = 5;
    private static final int UNLOAD_RADIUS_CHUNKS = 7;
    private static final int MAX_CHUNK_UPLOADS_PER_FRAME = 4;
    private static final int INITIAL_UPLOAD_BUDGET = 96;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window = new Window(1280, 720, "Herecraft 26.2.28");
        window.init();

        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);

        new GrassColorReloadListener().onResourceManagerReload(new ClasspathResourceManager());
        int grassColor = ColorizerGrass.getGrassColor(0.5f, 0.5f);
        grassTintColor.set(
                ((grassColor >> 16) & 0xFF) / 255.0f,
                ((grassColor >> 8) & 0xFF) / 255.0f,
                (grassColor & 0xFF) / 255.0f
        );

        shader = new Shader(vertexShaderSource, fragmentShaderSource);
        grassTexture = new TextureManager("herecraft/textures/block/grass_block_top.png");
        cobbleTexture = new TextureManager("herecraft/textures/block/cobblestone.png");

        world = new World();
        worldMesh = new WorldMesh(world);

        projection.setPerspective(
                (float) Math.toRadians(70.0f),
                (float) window.getWidth() / Math.max(window.getHeight(), 1),
                0.1f,
                5000.0f
        );

        updateCameraVectors();

        respawnAtWorldCenter();

        worldMesh.updateStreaming(
                0,
                0,
                LOAD_RADIUS_CHUNKS,
                UNLOAD_RADIUS_CHUNKS,
                INITIAL_UPLOAD_BUDGET
        );
    }

    private void loop() {
        while (!window.shouldClose()) {
            handleMouseLook();
            handleInput();
            applyGravity();

            projection.setPerspective(
                    (float) Math.toRadians(70.0f),
                    (float) window.getWidth() / Math.max(window.getHeight(), 1),
                    0.1f,
                    5000.0f
            );

            cameraTarget.set(cameraPos).add(cameraFront);
            view.identity().lookAt(cameraPos, cameraTarget, cameraUp);
            model.identity();
            projection.mul(view, mvp).mul(model);

            float playerBlockX = cameraPos.x / BLOCK_SIZE;
            float playerBlockZ = cameraPos.z / BLOCK_SIZE;
            worldMesh.updateStreaming(
                    playerBlockX,
                    playerBlockZ,
                    LOAD_RADIUS_CHUNKS,
                    UNLOAD_RADIUS_CHUNKS,
                    MAX_CHUNK_UPLOADS_PER_FRAME
            );

            window.clear();

            shader.use();
            grassTexture.bind(0);
            cobbleTexture.bind(1);
            shader.setUniform1i("texGrass", 0);
            shader.setUniform1i("texCobble", 1);
            shader.setUniform3f("grassTint", grassTintColor.x, grassTintColor.y, grassTintColor.z);
            shader.setUniform3f("cameraPos", cameraPos.x, cameraPos.y, cameraPos.z);
            shader.setUniformMat4("mvp", mvp.get(new float[16]));

            worldMesh.render(playerBlockX, playerBlockZ, cameraPos, cameraFront);
            if (screenshotRequested) {
                takeScreenshot();
                screenshotRequested = false;
            }
            window.update();
        }
    }

    private void handleMouseLook() {
        float deltaX = window.consumeMouseDeltaX();
        float deltaY = window.consumeMouseDeltaY();

        if (deltaX == 0.0f && deltaY == 0.0f) {
            return;
        }

        yaw += deltaX * MOUSE_SENSITIVITY;
        pitch -= deltaY * MOUSE_SENSITIVITY;

        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        updateCameraVectors();
    }

    private void updateCameraVectors() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        cameraFront.set(
                (float) (Math.cos(yawRad) * Math.cos(pitchRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.sin(yawRad) * Math.cos(pitchRad))
        ).normalize();

        cameraFront.cross(cameraUp, cameraRight).normalize();
        cameraRight.y = 0.0f;
        if (cameraRight.lengthSquared() > 0.0f) {
            cameraRight.normalize();
        }
    }

    private void handleInput() {
        moveDir.zero();

        float fx = cameraFront.x;
        float fz = cameraFront.z;
        float len = (float) Math.sqrt(fx * fx + fz * fz);

        if (len > 0.0f) {
            fx /= len;
            fz /= len;
        }

        if (window.isKeyDown(GLFW.GLFW_KEY_W)) {
            moveDir.x += fx;
            moveDir.z += fz;
        }
        if (window.isKeyDown(GLFW.GLFW_KEY_S)) {
            moveDir.x -= fx;
            moveDir.z -= fz;
        }
        if (window.isKeyDown(GLFW.GLFW_KEY_A)) {
            moveDir.sub(cameraRight);
        }
        if (window.isKeyDown(GLFW.GLFW_KEY_D)) {
            moveDir.add(cameraRight);
        }

        if (window.isKeyDown(GLFW.GLFW_KEY_SPACE) && onGround) {
            velocityY = JUMP_VELOCITY;
            onGround = false;
        }

        if (moveDir.lengthSquared() > 0.0f) {
            moveDir.normalize(MOVE_SPEED);
            moveWithCollision(moveDir.x, moveDir.z);
        }

        if (window.isKeyDown(GLFW.GLFW_KEY_ESCAPE)) {
            window.requestClose();
        }

        boolean respawnKeyDownNow = window.isKeyDown(GLFW.GLFW_KEY_R);
        if (respawnKeyDownNow && !respawnKeyDownLastFrame) {
            respawnAtWorldCenter();
        }
        respawnKeyDownLastFrame = respawnKeyDownNow;

        boolean screenshotKeyDownNow = window.isKeyDown(GLFW.GLFW_KEY_F2);
        if (screenshotKeyDownNow && !screenshotKeyDownLastFrame) {
            screenshotRequested = true;
        }
        screenshotKeyDownLastFrame = screenshotKeyDownNow;
    }

    private void applyGravity() {
        velocityY -= GRAVITY;
        if (velocityY < TERMINAL_VELOCITY) {
            velocityY = TERMINAL_VELOCITY;
        }

        float nextY = cameraPos.y + velocityY;
        if (!collidesAt(cameraPos.x, nextY, cameraPos.z)) {
            cameraPos.y = nextY;
            onGround = false;
        } else {
            if (velocityY < 0.0f) {
                onGround = true;
            }
            velocityY = 0.0f;
        }

        if (cameraPos.y < VOID_Y) {
            respawnAtWorldCenter();
        }
    }

    private boolean isSolidBlock(int bx, int by, int bz) {
        return world.get(bx, by, bz) != BlockType.AIR;
    }

    private boolean collidesAt(float testX, float testY, float testZ) {
        float feetY = testY - PLAYER_EYE_OFFSET;
        float headY = feetY + PLAYER_HEIGHT;

        float[] xs = {testX - PLAYER_RADIUS, testX + PLAYER_RADIUS};
        float[] zs = {testZ - PLAYER_RADIUS, testZ + PLAYER_RADIUS};
        float[] ys = {feetY + 0.1f, (feetY + headY) * 0.5f, headY - 0.1f};

        for (float x : xs) {
            for (float y : ys) {
                for (float z : zs) {
                    int bx = (int) Math.floor(x / BLOCK_SIZE);
                    int by = (int) Math.floor(y / BLOCK_SIZE);
                    int bz = (int) Math.floor(z / BLOCK_SIZE);
                    if (isSolidBlock(bx, by, bz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void moveWithCollision(float dx, float dz) {
        float nextX = clampToWorldX(cameraPos.x + dx);
        if (!collidesAt(nextX, cameraPos.y, cameraPos.z)) {
            cameraPos.x = nextX;
        }

        float nextZ = clampToWorldZ(cameraPos.z + dz);
        if (!collidesAt(cameraPos.x, cameraPos.y, nextZ)) {
            cameraPos.z = nextZ;
        }
    }

    private float clampToWorldX(float x) {
        float min = World.MIN_X * BLOCK_SIZE + PLAYER_RADIUS;
        float max = (World.MAX_X + 1) * BLOCK_SIZE - PLAYER_RADIUS;
        return Math.max(min, Math.min(max, x));
    }

    private float clampToWorldZ(float z) {
        float min = World.MIN_Z * BLOCK_SIZE + PLAYER_RADIUS;
        float max = (World.MAX_Z + 1) * BLOCK_SIZE - PLAYER_RADIUS;
        return Math.max(min, Math.min(max, z));
    }

    private void respawnAtWorldCenter() {
        int spawnX = 0;
        int spawnZ = 0;
        int topY = world.getTopSolidY(spawnX, spawnZ);
        float surfaceY = (topY + 1.0f) * BLOCK_SIZE;
        cameraPos.set(
                (spawnX + 0.5f) * BLOCK_SIZE,
                surfaceY + PLAYER_EYE_OFFSET + 0.1f,
                (spawnZ + 0.5f) * BLOCK_SIZE
        );
        velocityY = 0.0f;
        onGround = false;
    }

    private void takeScreenshot() {
        int width = window.getWidth();
        int height = window.getHeight();
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (x + y * width) * 4;
                int r = pixels.get(index) & 0xFF;
                int g = pixels.get(index + 1) & 0xFF;
                int b = pixels.get(index + 2) & 0xFF;
                int a = pixels.get(index + 3) & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, height - 1 - y, argb);
            }
        }

        try {
            Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
            Files.createDirectories(downloads);
            String fileName = "herecraft_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
            Path output = downloads.resolve(fileName);
            ImageIO.write(image, "png", output.toFile());
            System.out.println("Screenshot saved: " + output.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save screenshot: " + e.getMessage());
        }
    }

    private void cleanup() {
        if (worldMesh != null) {
            worldMesh.cleanup();
        }
        if (grassTexture != null) {
            grassTexture.cleanup();
        }
        if (cobbleTexture != null) {
            cobbleTexture.cleanup();
        }
        if (shader != null) {
            shader.cleanup();
        }
        if (window != null) {
            window.cleanup();
        }
    }
}
