package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.entity.EntityMeteor;
import com.xcompwiz.mystcraft.registry.MystSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Modern equivalent of the legacy non-attenuated looping MovingSoundMeteor. */
final class MovingSoundMeteor extends AbstractTickableSoundInstance {
    private final EntityMeteor meteor;

    MovingSoundMeteor(EntityMeteor meteor) {
        super(MystSounds.METEOR_ROAR.get(), SoundSource.HOSTILE, RandomSource.create(meteor.getId()));
        this.meteor = meteor;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = true;
        this.delay = 0;
        this.volume = 2.0F;
        this.pitch = 1.0F;
        this.relative = false;
        updatePosition();
    }

    @Override
    public void tick() {
        if (meteor.isRemoved()) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        this.x = meteor.getX();
        this.y = meteor.getY();
        this.z = meteor.getZ();
    }
}
