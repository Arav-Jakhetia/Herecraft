package net.herecraft.client.player;

import net.herecraft.client.input.Keyboard;
import net.herecraft.client.render.Camera;
import net.herecraft.client.world.World;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Player {
    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.8f;
    private static final float EYE_HEIGHT = 1.62f;

    private static final float MOVE_SPEED = 5.0f;
    private static final float JUMP_SPEED = 8.0f;
    private static final float GRAVITY = 22.0f;
    private static final float MAX_FALL_SPEED = 40.0f;

    private final Camera camera;
    private final Vector3f position; // feet position, center of the box on x/z
    private final Vector3f velocity;
    private boolean onGround;

    public Player(Camera camera, Vector3f startPosition) {
        this.camera = camera;
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f(0, 0, 0);
        updateCameraPosition();
    }

    public void update(float deltaTime, World world) {
        handleInput();

        velocity.y -= GRAVITY * deltaTime;
        if(velocity.y < -MAX_FALL_SPEED) {
            velocity.y = -MAX_FALL_SPEED;
        }

        moveAxis(world, 0, velocity.x * deltaTime);
        moveAxis(world, 1, velocity.y * deltaTime);
        moveAxis(world, 2, velocity.z * deltaTime);

        updateCameraPosition();
    }

    private void handleInput() {
        float forwardInput = 0.0f;
        float rightInput = 0.0f;

        if(Keyboard.isDown(GLFW_KEY_W)) forwardInput += 1.0f;
        if(Keyboard.isDown(GLFW_KEY_S)) forwardInput -= 1.0f;
        if(Keyboard.isDown(GLFW_KEY_D)) rightInput += 1.0f;
        if(Keyboard.isDown(GLFW_KEY_A)) rightInput -= 1.0f;

        Vector3f forward = camera.getForward();
        forward.y = 0.0f;
        if(forward.lengthSquared() > 0.0001f) {
            forward.normalize();
        }

        Vector3f right = camera.getRight();
        right.y = 0.0f;
        if(right.lengthSquared() > 0.0001f) {
            right.normalize();
        }

        float wishX = forward.x * forwardInput + right.x * rightInput;
        float wishZ = forward.z * forwardInput + right.z * rightInput;

        float length = (float)Math.sqrt(wishX * wishX + wishZ * wishZ);
        if(length > 0.0001f) {
            wishX /= length;
            wishZ /= length;
        }

        velocity.x = wishX * MOVE_SPEED;
        velocity.z = wishZ * MOVE_SPEED;

        if(Keyboard.isDown(GLFW_KEY_SPACE) && onGround) {
            velocity.y = JUMP_SPEED;
            onGround = false;
        }
    }

    private void moveAxis(World world, int axis, float amount) {
        if(amount == 0.0f) {
            return;
        }

        if(axis == 0) position.x += amount;
        else if(axis == 1) position.y += amount;
        else position.z += amount;

        if(collides(world)) {
            // step back out of the block on just this axis
            if(axis == 0) position.x -= amount;
            else if(axis == 1) position.y -= amount;
            else position.z -= amount;

            if(axis == 1) {
                onGround = amount < 0.0f;
                velocity.y = 0.0f;
            } else if(axis == 0) {
                velocity.x = 0.0f;
            } else {
                velocity.z = 0.0f;
            }
        } else if(axis == 1) {
            onGround = false;
        }
    }

    private boolean collides(World world) {
        float minX = position.x - WIDTH / 2.0f;
        float maxX = position.x + WIDTH / 2.0f;
        float minY = position.y;
        float maxY = position.y + HEIGHT;
        float minZ = position.z - WIDTH / 2.0f;
        float maxZ = position.z + WIDTH / 2.0f;

        int startX = (int)Math.floor(minX);
        int endX = (int)Math.floor(maxX - 0.0001f);
        int startY = (int)Math.floor(minY);
        int endY = (int)Math.floor(maxY - 0.0001f);
        int startZ = (int)Math.floor(minZ);
        int endZ = (int)Math.floor(maxZ - 0.0001f);

        for(int x = startX; x <= endX; x++) {
            for(int y = startY; y <= endY; y++) {
                for(int z = startZ; z <= endZ; z++) {
                    if(world.isSolidBlock(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void updateCameraPosition() {
        camera.setPosition(new Vector3f(position.x, position.y + EYE_HEIGHT, position.z));
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }
}