package net.herecraft.client.math;

public class Matrix4 {
    public static float[] perspective(float fov, float aspect, float near, float far) {
        float tanHalfFov = (float)Math.tan(fov / 2.0f);
        float result[] = new float[16];

        result[0] = 1.0f / (aspect * tanHalfFov);
        result[5] = 1.0f / tanHalfFov;
        result[10] = -(far + near) / (far - near);
        result[11] = -1.0f;
        result[14] = -(2.0f * far * near) / (far - near);

        return result;
    }

    public static float[] lookAt(
            float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ,
            float upX, float upY, float upZ
    ) {
        float forward[] = normalize(centerX - eyeX, centerY - eyeY, centerZ - eyeZ);
        float side[] = normalize(cross(forward[0], forward[1], forward[2], upX, upY, upZ));
        float up[] = cross(side[0], side[1], side[2], forward[0], forward[1], forward[2]);

        float result[] = identity();
        result[0] = side[0];
        result[4] = side[1];
        result[8] = side[2];
        result[1] = up[0];
        result[5] = up[1];
        result[9] = up[2];
        result[2] = -forward[0];
        result[6] = -forward[1];
        result[10] = -forward[2];
        result[12] = -dot(side, eyeX, eyeY, eyeZ);
        result[13] = -dot(up, eyeX, eyeY, eyeZ);
        result[14] = dot(forward, eyeX, eyeY, eyeZ);

        return result;
    }

    public static float[] multiply(float left[], float right[]) {
        float result[] = new float[16];

        for(int column = 0; column < 4; column++) {
            for(int row = 0; row < 4; row++) {
                result[column * 4 + row] =
                        left[0 * 4 + row] * right[column * 4 + 0] +
                        left[1 * 4 + row] * right[column * 4 + 1] +
                        left[2 * 4 + row] * right[column * 4 + 2] +
                        left[3 * 4 + row] * right[column * 4 + 3];
            }
        }

        return result;
    }

    private static float[] identity() {
        float result[] = new float[16];
        result[0] = 1.0f;
        result[5] = 1.0f;
        result[10] = 1.0f;
        result[15] = 1.0f;
        return result;
    }

    private static float[] normalize(float x, float y, float z) {
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        return new float[] {x / length, y / length, z / length};
    }

    private static float[] normalize(float vector[]) {
        return normalize(vector[0], vector[1], vector[2]);
    }

    private static float[] cross(float ax, float ay, float az, float bx, float by, float bz) {
        return new float[] {
                ay * bz - az * by,
                az * bx - ax * bz,
                ax * by - ay * bx
        };
    }

    private static float dot(float vector[], float x, float y, float z) {
        return vector[0] * x + vector[1] * y + vector[2] * z;
    }
}
