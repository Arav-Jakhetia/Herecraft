package net.herecraft.client.input;

import static org.lwjgl.glfw.GLFW.*;

public class Keyboard {
    private static long window;

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
}