package net.herecraft.client.block;

public class Block {
    public static final int GRASS_BLOCK_TOP_TEXTURE = 0;
    public static final int DIRT_TEXTURE = 1;
    public static final int STONE_TEXTURE = 2;
    public static final int GRASS_BLOCK_SIDE_TEXTURE = 3;

    private final int topTextureLayer;
    private final int sideTextureLayer;
    private final int bottomTextureLayer;

    private final boolean solid;

    public Block(boolean solid, int textureLayer) {
        this(solid, textureLayer, textureLayer, textureLayer);
    }

    public Block(boolean solid, int topTextureLayer, int sideTextureLayer, int bottomTextureLayer) {
        this.solid = solid;
        this.topTextureLayer = topTextureLayer;
        this.sideTextureLayer = sideTextureLayer;
        this.bottomTextureLayer = bottomTextureLayer;
    }

    public static Block grass() {
        return new Block(true, GRASS_BLOCK_TOP_TEXTURE, GRASS_BLOCK_SIDE_TEXTURE, DIRT_TEXTURE);
    }

    public boolean isSolid() {
        return solid;
    }

    public static Block air() {
        return new Block(false, -1);
    }

    public static Block dirt() {
        return new Block(true, DIRT_TEXTURE);
    }

    public static Block stone() {
        return new Block(true, STONE_TEXTURE);
    }

    public int getTopTextureLayer() {
        return topTextureLayer;
    }

    public int getSideTextureLayer() {
        return sideTextureLayer;
    }

    public int getBottomTextureLayer() {
        return bottomTextureLayer;
    }
}
