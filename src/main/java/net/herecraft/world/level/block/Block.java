package net.herecraft.world.level.block;

import net.herecraft.world.level.block.state.BlockBehaviour;

public class Block extends BlockBehaviour {
    private final String id;
    private final int topTextureLayer;
    private final int sideTextureLayer;
    private final int bottomTextureLayer;

    public Block(String id, BlockBehaviour.Properties properties) {
        this(id, properties, 0);
    }

    public Block(String id, Properties properties, int textureLayer) {
        this(id, properties, textureLayer, textureLayer, textureLayer);
    }

    public Block(String id, Properties properties, int topTextureLayer, int sideTextureLayer, int bottomTextureLayer) {
        super(properties);
        this.id = id;
        this.topTextureLayer = topTextureLayer;
        this.sideTextureLayer = sideTextureLayer;
        this.bottomTextureLayer = bottomTextureLayer;
    }

    public boolean isGrass() {
        return id.equals("grass");
    }

    public boolean isDirt() {
        return id.equals("dirt");
    }

    public boolean isSapling() {
        return id.endsWith("_sapling");
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
