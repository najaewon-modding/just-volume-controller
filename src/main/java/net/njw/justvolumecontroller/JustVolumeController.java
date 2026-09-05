package net.njw.justvolumecontroller;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.njw.justvolumecontroller.client.gui.JustVolumeControllerConfigScreen;
import net.njw.justvolumecontroller.config.SoundVolumeConfig;

@Mod(value = JustVolumeController.MOD_ID, dist = Dist.CLIENT)
public final class JustVolumeController {
    public static final String MOD_ID = "njw_just_volume_controller";

    private static final SoundVolumeConfig SOUND_VOLUMES = new SoundVolumeConfig(
            FMLPaths.CONFIGDIR.get().resolve(MOD_ID + ".json")
    );

    public JustVolumeController(ModContainer modContainer) {
        SOUND_VOLUMES.load();
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new JustVolumeControllerConfigScreen(parent)
        );
    }

    public static SoundVolumeConfig soundVolumes() {
        return SOUND_VOLUMES;
    }
}
