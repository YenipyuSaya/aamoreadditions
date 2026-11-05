package com.aamoreadditions.client.sound;

import com.aamoreadditions.block.AlloyFurnaceBlock;
import com.aamoreadditions.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Map;

/** Client-only looping working noise for the Alloy Furnace, with a 1s linear fade-out. */
public final class AlloyFurnaceSound extends AbstractTickableSoundInstance {
    private static final int FADE_TICKS = 20;
    private static final Map<BlockPos, AlloyFurnaceSound> PLAYING = new HashMap<>();

    private final BlockPos pos;
    private final ClientLevel level;
    private int fade = -1; // -1 = not fading

    private AlloyFurnaceSound(ClientLevel level, BlockPos pos) {
        super(ModSounds.ALLOY_FURNACE_WORKING.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
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

    static boolean isWorking(ClientLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof AlloyFurnaceBlock && s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
    }

    /** Called from AlloyFurnaceBlock#animateTick (client only) while lit. */
    public static void ensurePlaying(ClientLevel level, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        PLAYING.values().removeIf(s -> s.isStopped() || !mc.getSoundManager().isActive(s) && s.started);
        AlloyFurnaceSound cur = PLAYING.get(pos);
        if (cur != null && !cur.isStopped() && cur.fade < 0 && cur.level == level) return;
        if (cur != null) cur.halt(); // fading or stale: restart fresh from the beginning
        AlloyFurnaceSound s = new AlloyFurnaceSound(level, pos);
        PLAYING.put(s.pos, s);
        mc.getSoundManager().play(s);
    }

    private boolean started;

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
            if (!isWorking(level, pos)) fade = FADE_TICKS;
        } else {
            fade--;
            volume = Math.max(0F, fade / (float) FADE_TICKS);
            if (fade <= 0) halt();
        }
    }
}
