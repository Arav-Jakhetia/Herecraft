package net.herecraft.util.math;

public class Matrix4f {
    public final float[] elements = new float[16];

    public Matrix4f() {
        identity();
    }

    public Matrix4f identity() {
        for (int i = 0; i < 16; i++) {
            elements[i] = 0.0f;
        }

        elements[0] = 1.0f;
        elements[5] = 1.0f;
        elements[10] = 1.0f;
        elements[15] = 1.0f;
        return this;
    }

    public Matrix4f identify() {
        return identity();
    }

    public Matrix4f perspective(float fov, float aspect, float near, float far) {
        identity();
        float tanHalfFov = (float) Math.tan(fov / 2.0f);

        elements[0] = 1.0f / (aspect * tanHalfFov);
        elements[5] = 1.0f / tanHalfFov;
        elements[10] = -(far + near) / (far - near);
        elements[11] = -1.0f;
        elements[14] = -(2.0f * far * near) / (far - near);
        elements[15] = 0.0f;
        return this;
    }

    public Matrix4f translate(float x, float y, float z) {
        elements[12] += x;
        elements[13] += y;
        elements[14] += z;
        return this;
    }

    public Matrix4f multiply(Matrix4f other) {
        Matrix4f result = new Matrix4f();

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                result.elements[col + row * 4] =
                        elements[row * 4] * other.elements[col] +
                                elements[row * 4 + 1] * other.elements[col + 4] +
                                elements[row * 4 + 2] * other.elements[col + 8] +
                                elements[row * 4 + 3] * other.elements[col + 12];
            }
        }

        return result;
    }

    public float[] get() {
        return elements;
    }
}
