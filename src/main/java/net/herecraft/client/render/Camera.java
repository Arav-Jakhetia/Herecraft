package net.herecraft.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private Vector3f position;
    private Vector3f orientation;

    private float yaw = 0;
    private float pitch = 0;

    public Camera(float x, float y, float z) {
        position = new Vector3f(x, y, z);
        orientation = new Vector3f(0, 1, 0);
    }

    public void translate(float x, float y, float z) {
        Vector3f offset = new Vector3f(x, y, z);
        offset.rotateY((float)Math.toRadians(yaw), offset);

        position.x += offset.x;
        position.y += offset.y;
        position.z += offset.z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setPosition(Vector3f newPosition) {
        position.set(newPosition);
    }

    public void setlookDir(float x, float y) {
        yaw = x;
        pitch = Math.max(-89.0f, Math.min(89.0f, y));
    }

    public Matrix4f getMatrix() {
        Vector3f lookPoint = new Vector3f(position).add(getForward());

        Matrix4f matrix = new Matrix4f();
        matrix.lookAt(position, lookPoint, orientation, matrix);

        return matrix;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getForward() {
        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);

        Vector3f forward = new Vector3f();

        forward.x = (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        forward.y = (float)Math.sin(pitchRad);
        forward.z = (float)(-Math.cos(yawRad) * Math.cos(pitchRad));

        forward.normalize();
        return forward;
    }

    public void moveForward(float amount) {
        Vector3f forward = getForward();
        forward.y = 0.0f;
        forward.normalize();

        position.add(forward.mul(amount));
    }

    public void moveRight(float amount) {
        Vector3f right = getRight();
        right.y = 0.0f;
        right.normalize();

        position.add(right.mul(amount));
    }

    public void moveUp(float amount) {
        position.y += amount;
    }

    public Vector3f getRight() {
        Vector3f right = getForward().cross(new Vector3f(0, 1, 0));
        right.normalize();
        return right;
    }
}
