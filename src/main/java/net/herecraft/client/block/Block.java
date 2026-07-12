package net.herecraft.client.block;

public class Block {
    public static final int GRASS_BLOCK_TOP_TEXTURE = 0;
    public static final int DIRT_TEXTURE = 1;
    public static final int STONE_TEXTURE = 2;

    private final boolean solid;
    private final int textureLayer;

    public Block(boolean solid, int textureLayer) {
        this.solid = solid;
        this.textureLayer = textureLayer;
    }

    public boolean isSolid() {
        return solid;
    }

    public int getTextureLayer() {
        return textureLayer;
    }

    public static Block air() {
        return new Block(false, -1);
    }

    public static Block grass() {
        return new Block(true, GRASS_BLOCK_TOP_TEXTURE);
    }

    public static Block dirt() {
        return new Block(true, DIRT_TEXTURE);
    }

    public static Block stone() {
        return new Block(true, STONE_TEXTURE);
    }
}
