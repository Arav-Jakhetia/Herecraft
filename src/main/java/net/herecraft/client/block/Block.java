package net.herecraft.client.block;

public class Block {
    private final boolean solid;
    private final float red;
    private final float green;
    private final float blue;

    public Block(boolean solid, float red, float green, float blue) {
        this.solid = solid;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public boolean isSolid() {
        return solid;
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    public static Block air() {
        return new Block(false, 0.0f, 0.0f, 0.0f);
    }

    public static Block grass() {
        return new Block(true, 0.2f, 0.7f, 0.25f);
    }

    public static Block dirt() {
        return new Block(true, 0.45f, 0.28f, 0.12f);
    }

    public static Block stone() {
        return new Block(true, 0.45f, 0.45f, 0.48f);
    }
}
