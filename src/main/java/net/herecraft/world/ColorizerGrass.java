package net.herecraft.world;

public final class ColorizerGrass {
    private static int[] grassColorMap;
    private static int width;
    private static int height;

    private ColorizerGrass() {
    }

    public static void setGrassBiomeColorizer(int[] pixels, int mapWidth, int mapHeight) {
        grassColorMap = pixels;
        width = mapWidth;
        height = mapHeight;
    }

    public static boolean hasGrassColorizer() {
        return grassColorMap != null && width > 0 && height > 0;
    }

    public static int getGrassColor(float u, float v) {
        if (!hasGrassColorizer()) {
            return 0xFFFFFF;
        }

        float clampedU = Math.max(0.0f, Math.min(1.0f, u));
        float clampedV = Math.max(0.0f, Math.min(1.0f, v));
        int x = (int) (clampedU * (width - 1));
        int y = (int) (clampedV * (height - 1));
        return grassColorMap[y * width + x];
    }
}
