package net.njw.justvolumecontroller.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.njw.justvolumecontroller.JustVolumeController;
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
    private float justVolumeController$applyInitialVolume(float originalVolume, SoundInstance soundInstance) {
        return justVolumeController$applyVolume(originalVolume, soundInstance);
    }

    @ModifyReturnValue(
            method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN")
    )
    private float justVolumeController$applyUpdatedVolume(float originalVolume, SoundInstance soundInstance) {
        return justVolumeController$applyVolume(originalVolume, soundInstance);
    }

    @Unique
    private static float justVolumeController$applyVolume(float originalVolume, SoundInstance soundInstance) {
        return originalVolume * JustVolumeController.soundVolumes().getVolumeMultiplier(soundInstance.getIdentifier());
    }
}
