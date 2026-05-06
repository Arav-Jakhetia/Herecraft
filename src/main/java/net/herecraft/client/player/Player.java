package net.herecraft.client.player;

import net.herecraft.client.input.Keyboard;
import net.herecraft.client.render.Camera;

import static org.lwjgl.glfw.GLFW.*;

public class Player {
    private static final float SPEED = 8.0f;

    private final Camera camera;

    public Player(Camera camera) {
        this.camera = camera;
    }

    public void update(float deltaTime) {
        float moveSpeed = SPEED * deltaTime;
        float dx = 0.0f;
        float dy = 0.0f;
        float dz = 0.0f;

        if(Keyboard.isKeyDown(GLFW_KEY_W)) {
            dx += camera.forwardX() * moveSpeed;
            dz += camera.forwardZ() * moveSpeed;
        }
        if(Keyboard.isKeyDown(GLFW_KEY_S)) {
            dx -= camera.forwardX() * moveSpeed;
            dz -= camera.forwardZ() * moveSpeed;
        }
        if(Keyboard.isKeyDown(GLFW_KEY_A)) {
            dx -= camera.rightX() * moveSpeed;
            dz -= camera.rightZ() * moveSpeed;
        }
        if(Keyboard.isKeyDown(GLFW_KEY_D)) {
            dx += camera.rightX() * moveSpeed;
            dz += camera.rightZ() * moveSpeed;
        }
        if(Keyboard.isKeyDown(GLFW_KEY_SPACE)) {
            dy += moveSpeed;
        }
        if(Keyboard.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            dy -= moveSpeed;
        }

        camera.move(dx, dy, dz);
    }
}
