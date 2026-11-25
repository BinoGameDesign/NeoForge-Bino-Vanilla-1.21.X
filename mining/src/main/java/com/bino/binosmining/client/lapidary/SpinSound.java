package com.bino.binosmining.client.lapidary;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.function.DoubleSupplier;
import com.bino.binosmining.sound.MiningSounds;

// Zumbido do disco: som em loop que acompanha a velocidade do disco (volume e tom sobem e descem com ela).
public class SpinSound extends AbstractTickableSoundInstance {
    private final DoubleSupplier speed;
    private int age;

    private SpinSound(BlockPos pos, DoubleSupplier speed) {
        super(MiningSounds.LAPIDARY_SPIN.get(), SoundSource.BLOCKS, RandomSource.create());
        this.speed = speed;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.pitch = 0.6f;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.8;
        this.z = pos.getZ() + 0.5;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    public static SpinSound start(BlockPos pos, DoubleSupplier speed) {
        SpinSound s = new SpinSound(pos, speed);
        Minecraft.getInstance().getSoundManager().play(s);
        return s;
    }

    @Override
    public void tick() {
        age++;
        double v = speed.getAsDouble();
        this.volume = (float) Math.max(0.01, v * 0.55);
        this.pitch = (float) (0.55 + 0.6 * v);
        if (age > 20 && v < 0.01) stop();   // o disco parou
        if (age > 20 * 12) stop();
    }

    public void stopNow() { stop(); }

    @Override
    public boolean canStartSilent() { return true; }
}
