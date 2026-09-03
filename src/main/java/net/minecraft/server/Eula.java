package net.minecraft.server;

import java.nio.file.Path;

// The EULA is a useless and unnecessary feature, so i removed it.
public class Eula {
    public Eula(Path file) {}
    public boolean hasAgreedToEULA() { return true; }
}