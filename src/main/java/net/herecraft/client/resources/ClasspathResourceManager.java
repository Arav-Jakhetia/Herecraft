package net.herecraft.client.resources;

import net.herecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public class ClasspathResourceManager implements IResourceManager {
    private final ClassLoader classLoader;

    public ClasspathResourceManager() {
        this.classLoader = ClasspathResourceManager.class.getClassLoader();
    }

    @Override
    public InputStream getResource(ResourceLocation location) throws IOException {
        InputStream in = classLoader.getResourceAsStream(location.getPath());
        if (in == null) {
            throw new IOException("Resource not found: " + location);
        }
        return in;
    }
}
