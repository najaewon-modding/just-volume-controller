package net.njw.volumedesk;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(VolumeDesk.MODID)
public class VolumeDesk {
    public static final String MODID = "njw_volume_desk";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VolumeDesk(IEventBus modEventBus, ModContainer modContainer) {
    }
}
