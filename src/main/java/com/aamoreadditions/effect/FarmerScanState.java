package com.aamoreadditions.effect;

/** Constant-space scan accounting. Only actual AA checkBehaviors calls advance it;
 * a round may start at any cursor offset, not just the top-left cell. */
public final class FarmerScanState {
    public static final int SLOW_TICKS=20;
    private boolean initialized, enabled=true, sleeping, found;
    private int side, facing, checked, expected=-1;
    private long position;

    public boolean sleeping(){return sleeping;}
    public int checked(){return checked;}
    public boolean configure(int side,int facing,long position,boolean enabled) {
        boolean changed=!initialized || this.side!=side || this.facing!=facing || this.position!=position || this.enabled!=enabled;
        if(changed) {
            reset(); this.side=side;this.facing=facing;this.position=position;this.enabled=enabled;initialized=true;
        }
        return changed;
    }
    public void reset(){sleeping=false;found=false;checked=0;expected=-1;}
    public void loaded(){reset();initialized=false;}
    /** Client presentation comes from AA's existing server NBT update, never client scans. */
    public void syncedSleeping(boolean sleeping){this.sleeping=sleeping;}
    /** Successful planting is work, even without a mature harvest. Begin a fresh
     * normal-speed round, retaining this success until that round has completed. */
    public void planted(int cell) {
        if(!enabled)return;
        reset();
        long cells=(long)side*side;
        if(!initialized || side<1 || cells>Integer.MAX_VALUE || cell<0 || cell>=cells)return;
        found=true;
        expected=(int)((cell+1L)%cells);
    }
    /** Convenience for a known mature, processable candidate. */
    public void observe(int cell,boolean matureOrUnknown) {observe(cell,matureOrUnknown,false,matureOrUnknown);}
    public void observe(int cell,boolean mature,boolean storageBlocked,boolean harvestFits) {
        observe(cell,mature,storageBlocked,harvestFits,false);
    }
    public void observe(int cell,boolean mature,boolean storageBlocked,boolean harvestFits,boolean unknownBehavior) {
        if(!enabled)return;
        long cells=(long)side*side;
        if(!initialized || side<1 || cells>Integer.MAX_VALUE || cell<0 || cell>=cells) {reset();return;}
        // A cursor jump is not a completed round: start a fresh traversal conservatively.
        if(expected!=-1 && cell!=expected)reset();
        // Wake only on an actual fit, or conservatively opt out for unknown extensions.
        // Exclude the wake-up cell: the subsequent normal round must visit every cell anew.
        if(sleeping && (harvestFits || unknownBehavior)){reset();return;}
        // No capacity result for a mature crop (e.g. insufficient operation FE) is
        // uncertain, not proof of blockage. Do not create a new sleep reason from it.
        if(harvestFits || unknownBehavior || (mature && !storageBlocked))found=true;
        // A rejected target alone never changes scan speed. Only the completed round can sleep.
        checked++;expected=(int)((cell+1L)%cells);
        if(checked==cells){if(!found)sleeping=true;checked=0;found=false;}
    }
}
