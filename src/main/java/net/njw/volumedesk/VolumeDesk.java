package net.njw.volumedesk;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.njw.volumedesk.config.SoundVolumeConfig;

@Mod(value = VolumeDesk.MOD_ID, dist = Dist.CLIENT)
public final class VolumeDesk {
    public static final String MOD_ID = "njw_volume_desk";

    private static final SoundVolumeConfig SOUND_VOLUMES = new SoundVolumeConfig(
            FMLPaths.CONFIGDIR.get().resolve(MOD_ID + ".json")
    );

    public VolumeDesk() {
        SOUND_VOLUMES.load();
    }

    public static SoundVolumeConfig soundVolumes() {
        return SOUND_VOLUMES;
    }
}
