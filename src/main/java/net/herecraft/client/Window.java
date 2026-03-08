package net.herecraft.client;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Window {
    private int width;
    private int height;
    private final String title;
    private long window;

    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;

    private boolean fullscreen;
    private boolean suppressMaximizedCallback;

    private double mouseDeltaX;
    private double mouseDeltaY;
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouseEvent = false;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;

        this.windowedWidth = width;
        this.windowedHeight = height;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode mode = monitor != 0 ? GLFW.glfwGetVideoMode(monitor) : null;
        if (mode != null) {
            width = mode.width();
            height = mode.height();
            windowedWidth = width;
            windowedHeight = height;
            fullscreen = false;
        }

        window = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        if (mode != null) {
            GLFW.glfwSetWindowPos(window, 0, 0);
        }

        setupCallbacks();

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        if (GLFW.glfwRawMouseMotionSupported()) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
        }

        GLFW.glfwShowWindow(window);
        GLFW.glfwMaximizeWindow(window);
        GLFW.glfwFocusWindow(window);
    }

    private void setupCallbacks() {
        GLFW.glfwSetFramebufferSizeCallback(window, (w, newWidth, newHeight) -> {
            this.width = newWidth;
            this.height = newHeight;
            GL11.glViewport(0, 0, newWidth, newHeight);
        });

        GLFW.glfwSetWindowPosCallback(window, (w, x, y) -> {
            if (!fullscreen) {
                windowedX = x;
                windowedY = y;
            }
        });

        GLFW.glfwSetWindowSizeCallback(window, (w, newWidth, newHeight) -> {
            if (!fullscreen) {
                windowedWidth = newWidth;
                windowedHeight = newHeight;
            }
        });

        GLFW.glfwSetWindowMaximizeCallback(window, (w, maximized) -> {
        });

        GLFW.glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (firstMouseEvent) {
                lastMouseX = x;
                lastMouseY = y;
                firstMouseEvent = false;
                return;
            }

            mouseDeltaX += (x - lastMouseX);
            mouseDeltaY += (y - lastMouseY);

            lastMouseX = x;
            lastMouseY = y;
        });
    }

    private void enterFullscreen() {
        long monitor = GLFW.glfwGetPrimaryMonitor();
        if (monitor == 0) {
            return;
        }

        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        if (mode == null) {
            return;
        }

        int[] x = new int[1];
        int[] y = new int[1];
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowPos(window, x, y);
        GLFW.glfwGetWindowSize(window, w, h);

        windowedX = x[0];
        windowedY = y[0];
        windowedWidth = w[0];
        windowedHeight = h[0];

        suppressMaximizedCallback = true;
        fullscreen = true;
        GLFW.glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
        suppressMaximizedCallback = false;
    }

    private void exitFullscreen() {
        suppressMaximizedCallback = true;
        fullscreen = false;
        GLFW.glfwSetWindowMonitor(window, 0, windowedX, windowedY, windowedWidth, windowedHeight, 0);
        suppressMaximizedCallback = false;
    }

    public float consumeMouseDeltaX() {
        float delta = (float) mouseDeltaX;
        mouseDeltaX = 0.0;
        return delta;
    }

    public float consumeMouseDeltaY() {
        float delta = (float) mouseDeltaY;
        mouseDeltaY = 0.0;
        return delta;
    }

    public void update() {
        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public void requestClose() {
        GLFW.glfwSetWindowShouldClose(window, true);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public long getHandle() {
        return window;
    }

    public void clear() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void cleanup() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
