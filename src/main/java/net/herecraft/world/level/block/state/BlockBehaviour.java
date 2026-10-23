package net.herecraft.world.level.block.state;

public abstract class BlockBehaviour {
    protected final boolean hasCollision;

    public BlockBehaviour(final Properties properties) {
        this.hasCollision = properties.hasCollision;
    }

    public boolean hasCollision() {
        return hasCollision;
    }

    public static class Properties {
        private boolean hasCollision = true;
        private boolean isAir;
        private boolean replaceable;

        public Properties() {}

        public static Properties of() {
            return new Properties();
        }

        public Properties noCollision() {
            this.hasCollision = false;
            return this;
        }

        public Properties air() {
            this.isAir = true;
            return this;
        }

        public Properties replaceable() {
            this.replaceable = true;
            return this;
        }
    }
}