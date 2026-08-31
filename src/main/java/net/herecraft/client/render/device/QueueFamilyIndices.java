package net.herecraft.client.render.device;

public class QueueFamilyIndices {
    public Integer graphicsFamily;
    public Integer presentFamily;

    public boolean isComplete() {
        return graphicsFamily != null && presentFamily != null;
    }
}
