package net.herecraft.client.input;

import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;

public class Mouse {
    private static long window;
    public static double mouseX;
    public static double mouseY;
    private static double lastMouseX;
    private static double lastMouseY;
    private static boolean initialized;

    private static boolean leftWasDown;
    private static boolean rightWasDown;

    public static void init(long windowHandler) {
        window = windowHandler;
    }

    public static void update() {
        DoubleBuffer xPos = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yPos = BufferUtils.createDoubleBuffer(1);

        glfwGetCursorPos(window, xPos, yPos);
        double currentMouseX = xPos.get(0);
        double currentMouseY = yPos.get(0);

        if(!initialized) {
            lastMouseX = currentMouseX;
            lastMouseY = currentMouseY;
            initialized = true;
            return;
        }

        mouseX += currentMouseX - lastMouseX;
        mouseY += currentMouseY - lastMouseY;

        lastMouseX = currentMouseX;
        lastMouseY = currentMouseY;
    }

    public static boolean isButtonDown(int button) {
        return glfwGetMouseButton(window, button) == GLFW_PRESS;
    }

    public static boolean consumeLeftClick() {
        boolean isDown = isButtonDown(GLFW_MOUSE_BUTTON_LEFT);
        boolean clicked = isDown && !leftWasDown;
        leftWasDown = isDown;
        return clicked;
    }

    public static boolean consumeRightClick() {
        boolean isDown = isButtonDown(GLFW_MOUSE_BUTTON_RIGHT);
        boolean clicked = isDown && !rightWasDown;
        rightWasDown = isDown;
        return clicked;
    }
}