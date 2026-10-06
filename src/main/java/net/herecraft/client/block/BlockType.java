package net.herecraft.client.block;

public enum BlockType {
    AIR(false, -1),
    GRASS(true, 0, 1, 2),
    DIRT(true, 2),
    STONE(true, 3),
    COBBLESTONE(true, 4),
    OAK_PLANKS(true, 5);

    private final boolean solid;
    private final int topTextureLayer;
    private final int sideTextureLayer;
    private final int bottomTextureLayer;

    BlockType(boolean solid, int textureLayer) {
        this(solid, textureLayer, textureLayer, textureLayer);
    }

    BlockType(boolean solid, int topTextureLayer, int sideTextureLayer, int bottomTextureLayer) {
        this.solid = solid;
        this.topTextureLayer = topTextureLayer;
        this.sideTextureLayer = sideTextureLayer;
        this.bottomTextureLayer = bottomTextureLayer;
    }

    public boolean isSolid() {
        return solid;
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
