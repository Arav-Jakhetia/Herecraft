package net.herecraft.client.input;

import static org.lwjgl.glfw.GLFW.*;

public class Keyboard {
    private static long window;
    private static boolean f3WasDown;

    public static void init(long windowHandler) {
        window = windowHandler;
    }

    public static boolean isDown(int keycode) {
        return glfwGetKey(window, keycode) == GLFW_PRESS;
    }

    public static void update() {
        if(isDown(GLFW_KEY_ESCAPE)) {
            glfwSetWindowShouldClose(window, true);
        }
    }

    public static boolean consumeF3Press() {
        boolean f3IsDown = isDown(GLFW_KEY_F3);
        boolean pressed = f3IsDown && !f3WasDown;
        f3WasDown = f3IsDown;
        return pressed;
    }
}