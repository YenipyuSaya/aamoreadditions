package com.aamoreadditions.mixin;

import com.aamoreadditions.digger.*;
import com.aamoreadditions.fake.MachineFakePlayers;
import com.aamoreadditions.upgrade.UpgradeRuntime;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vertical Digger: AAMA state (mode, highlight, fillers, range) and the AAMA server tick.
 * The upgrade guard lives here instead of UpgradeMachineTickMixin, so only one HEAD-cancel exists for this tile.
 */
@Mixin(value = TileEntityVerticalDigger.class, remap = false)
public abstract class TileEntityVerticalDiggerMixin implements DiggerState {
    @Unique private static final String AAMORE_MODE = "aamoreadditions_digger_strict_chunk", AAMORE_HIGHLIGHT = "aamoreadditions_digger_highlight",
            AAMORE_RANGE = "aamoreadditions_digger_range", AAMORE_SCAN_RANGE = "aamoreadditions_digger_scan_range",
            AAMORE_SCAN_CHUNK = "aamoreadditions_digger_scan_strict_chunk",
            AAMORE_SCAN_SPAN = "aamoreadditions_digger_scan_y_span";
    @Unique private boolean aamoreadditions$strictChunk, aamoreadditions$highlight;
    @Unique private DiggerFillers aamoreadditions$fillers;
    @Unique private final DiggerWork aamoreadditions$work = new DiggerWork();

    @Override public boolean aamoreadditions$strictChunk() { return aamoreadditions$strictChunk; }
    @Override public void aamoreadditions$setStrictChunk(boolean v) { aamoreadditions$strictChunk = v; }
    @Override public boolean aamoreadditions$highlight() { return aamoreadditions$highlight; }
    @Override public void aamoreadditions$setHighlight(boolean v) { aamoreadditions$highlight = v; }
    @Override public int aamoreadditions$range() { return aamoreadditions$work.range; }
    @Override public DiggerWork aamoreadditions$work() { return aamoreadditions$work; }
    @Override public DiggerFillers aamoreadditions$fillers() {
        if (aamoreadditions$fillers == null) aamoreadditions$fillers = new DiggerFillers((TileEntityVerticalDigger) (Object) this);
        return aamoreadditions$fillers;
    }

    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private static void aamoreadditions$tick(Level level, BlockPos pos, BlockState state, BlockEntity be, CallbackInfo ci) {
        if (!(be instanceof TileEntityVerticalDigger tile) || !(level instanceof ServerLevel server)) return;
        ci.cancel();
        if (UpgradeRuntime.tick(be)) return; // illegal plugins: work stopped, nothing mined
        DiggerRuntime.serverTick(tile, server, pos);
    }

    @Inject(method = "onButtonPressed", at = @At("TAIL"))
    private void aamoreadditions$reset(int buttonID, Player player, CallbackInfo ci) {
        if (buttonID == 1) DiggerRuntime.reset((TileEntityVerticalDigger) (Object) this);
    }

    /** AA restarts the scan on every load; resume a saved cursor instead (the range check runs on the next work tick). */
    @Inject(method = "onLoad", at = @At("TAIL"))
    private void aamoreadditions$resume(CallbackInfo ci) {
        var self = (TileEntityVerticalDigger) (Object) this;
        var saved = aamoreadditions$work.savedCursor;
        aamoreadditions$work.savedCursor = null;
        if (saved == null || self.getLevel() == null || self.getLevel().isClientSide) return;
        var pos = self.getBlockPos();
        if (DiggerScan.resumable(saved, pos.getY(), self.getLevel().getMinBuildHeight(),
                pos.getX(), pos.getZ(), aamoreadditions$work.scanRange, aamoreadditions$work.scanStrictChunk)) {
            self.checkX = saved.x(); self.checkY = saved.y(); self.checkZ = saved.z();
        }
    }

    @Inject(method = "writeSyncableNBT", at = @At("TAIL"))
    private void aamoreadditions$write(CompoundTag tag, HolderLookup.Provider provider, TileEntityBase.NBTType type, CallbackInfo ci) {
        if (type == TileEntityBase.NBTType.SAVE_BLOCK) return;
        tag.putBoolean(AAMORE_MODE, aamoreadditions$strictChunk);
        tag.putBoolean(AAMORE_HIGHLIGHT, aamoreadditions$highlight);
        tag.putInt(AAMORE_RANGE, aamoreadditions$work.range);
        if (type == TileEntityBase.NBTType.SAVE_TILE) {
            tag.putInt(AAMORE_SCAN_RANGE, aamoreadditions$work.scanRange);
            tag.putBoolean(AAMORE_SCAN_CHUNK, aamoreadditions$work.scanStrictChunk);
            tag.putInt(AAMORE_SCAN_SPAN, aamoreadditions$work.scanVerticalSpan);
            tag.put(DiggerFillers.NBT, aamoreadditions$fillers().serializeNBT(provider));
        }
    }

    @Inject(method = "readSyncableNBT", at = @At("TAIL"))
    private void aamoreadditions$read(CompoundTag tag, HolderLookup.Provider provider, TileEntityBase.NBTType type, CallbackInfo ci) {
        if (type == TileEntityBase.NBTType.SAVE_BLOCK) return;
        var self = (TileEntityVerticalDigger) (Object) this;
        aamoreadditions$strictChunk = tag.getBoolean(AAMORE_MODE);
        aamoreadditions$highlight = tag.getBoolean(AAMORE_HIGHLIGHT);
        if (tag.contains(AAMORE_RANGE)) aamoreadditions$work.range = tag.getInt(AAMORE_RANGE);
        if (type == TileEntityBase.NBTType.SAVE_TILE) {
            aamoreadditions$work.scanRange = tag.getInt(AAMORE_SCAN_RANGE);
            aamoreadditions$work.scanStrictChunk = tag.getBoolean(AAMORE_SCAN_CHUNK);
            aamoreadditions$work.scanVerticalSpan = tag.getInt(AAMORE_SCAN_SPAN);
            aamoreadditions$work.savedCursor = new DiggerScan.Cursor(self.checkX, self.checkY, self.checkZ);
            if (tag.contains(DiggerFillers.NBT)) aamoreadditions$fillers().deserializeNBT(provider, tag.getCompound(DiggerFillers.NBT));
        } else if (type == TileEntityBase.NBTType.SYNC) { // AA reads SYNC tags on the client only
            DiggerHighlights.track(self);
        }
    }

    @Redirect(method = "mine", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/util/FakePlayerFactory;getMinecraft(Lnet/minecraft/server/level/ServerLevel;)Lnet/neoforged/neoforge/common/util/FakePlayer;"))
    private FakePlayer aamoreadditions$clonePlayer(net.minecraft.server.level.ServerLevel level) {
        return MachineFakePlayers.of((TileEntityVerticalDigger) (Object) this);
    }
}
