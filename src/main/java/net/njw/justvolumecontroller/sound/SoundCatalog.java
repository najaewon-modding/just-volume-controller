package net.njw.justvolumecontroller.sound;

import net.minecraft.client.Minecraft;

public final class SoundCatalog {
    private SoundCatalog() {
    }

    public static SoundTree capture() {
        return SoundTree.from(Minecraft.getInstance().getSoundManager().getAvailableSounds());
    }
}
