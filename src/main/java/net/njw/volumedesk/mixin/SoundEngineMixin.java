package net.njw.volumedesk.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.njw.volumedesk.VolumeDesk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @ModifyExpressionValue(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"
            )
    )
    private float volumeDesk$applyInitialVolume(float originalVolume, SoundInstance soundInstance) {
        return volumeDesk$applyVolume(originalVolume, soundInstance);
    }

    @ModifyReturnValue(
            method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN")
    )
    private float volumeDesk$applyUpdatedVolume(float originalVolume, SoundInstance soundInstance) {
        return volumeDesk$applyVolume(originalVolume, soundInstance);
    }

    @Unique
    private static float volumeDesk$applyVolume(float originalVolume, SoundInstance soundInstance) {
        return originalVolume * VolumeDesk.soundVolumes().getVolumeMultiplier(soundInstance.getIdentifier());
    }
}
