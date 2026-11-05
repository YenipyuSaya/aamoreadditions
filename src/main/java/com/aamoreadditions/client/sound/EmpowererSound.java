package com.aamoreadditions.client.sound;

import com.aamoreadditions.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

/** Looping working noise of the Empowerer structure (at the anchor), driven by server packets; 1s linear fade-out like the Alloy Furnace. */
public final class EmpowererSound extends AbstractTickableSoundInstance {
    private static final int FADE_TICKS = 20, TIMEOUT_TICKS = 30; // refresh comes every 10 ticks
    private static final Map<BlockPos, EmpowererSound> PLAYING = new HashMap<>();

    private final BlockPos pos;
    private final ClientLevel level;
    private int fade = -1, sinceRefresh;
    private boolean started;

    private EmpowererSound(ClientLevel level, BlockPos pos) {
        super(ModSounds.EMPOWERER_STRUCTURE_WORKING.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.level = level;
        this.pos = pos.immutable();
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    public static void onPacket(BlockPos pos, boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        PLAYING.values().removeIf(s -> s.isStopped() || !mc.getSoundManager().isActive(s) && s.started);
        EmpowererSound cur = PLAYING.get(pos);
        if (!on) { if (cur != null && cur.fade < 0) cur.fade = FADE_TICKS; return; }
        if (cur != null && !cur.isStopped() && cur.fade < 0 && cur.level == mc.level) { cur.sinceRefresh = 0; return; }
        if (cur != null) cur.halt(); // fading or stale: restart fresh
        EmpowererSound s = new EmpowererSound(mc.level, pos);
        PLAYING.put(s.pos, s);
        mc.getSoundManager().play(s);
    }

    private void halt() {
        stop();
        Minecraft.getInstance().getSoundManager().stop(this);
        PLAYING.remove(pos, this);
    }

    @Override
    public void tick() {
        started = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level || mc.player == null) { halt(); return; }
        if (fade < 0) {
            if (++sinceRefresh > TIMEOUT_TICKS) fade = FADE_TICKS;
        } else {
            fade--;
            volume = Math.max(0F, fade / (float) FADE_TICKS);
            if (fade <= 0) halt();
        }
    }
}
