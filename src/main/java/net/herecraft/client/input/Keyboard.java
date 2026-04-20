package net.herecraft.client.input;

import static net.herecraft.Herecraft.window;
import static org.lwjgl.glfw.GLFW.*;

public class Keyboard {
    public static void input() {
        if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_TRUE) {
            glfwSetWindowShouldClose(window, true);
        }
    }
}
