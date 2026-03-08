package net.herecraft.client.resources;

import net.herecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public interface IResourceManager {
    InputStream getResource(ResourceLocation location) throws IOException;
}
