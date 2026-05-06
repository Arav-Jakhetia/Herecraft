package net.herecraft.client.render;

import net.herecraft.client.math.Matrix4;

public class Camera {
    private final float aspect;
    private float x = 18.0f;
    private float y = 10.0f;
    private float z = 24.0f;

    private final float forwardX = -0.55f;
    private final float forwardY = -0.25f;
    private final float forwardZ = -0.80f;

    public Camera(float aspect) {
        this.aspect = aspect;
    }

    public void move(float dx, float dy, float dz) {
        x += dx;
        y += dy;
        z += dz;
    }

    public float forwardX() {
        return forwardX;
    }

    public float forwardZ() {
        return forwardZ;
    }

    public float rightX() {
        return -forwardZ;
    }

    public float rightZ() {
        return forwardX;
    }

    public float[] viewProjection() {
        float[] projection = Matrix4.perspective((float)Math.toRadians(60.0f), aspect, 0.1f, 100.0f);
        float[] view = Matrix4.lookAt(
                x, y, z,
                x + forwardX, y + forwardY, z + forwardZ,
                0.0f, 1.0f, 0.0f
        );

        return Matrix4.multiply(projection, view);
    }
}
