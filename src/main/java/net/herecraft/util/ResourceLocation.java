package net.herecraft.util;

import com.google.gson.*;
import java.util.Locale;

public class ResourceLocation {
    protected final String path;

    public ResourceLocation(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be empty");
        }
        this.path = path;
    }

    protected ResourceLocation(int unused, String ... name) {
        this.path = name[1].toLowerCase(Locale.ROOT);
    }



    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }
}
