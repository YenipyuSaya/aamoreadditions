package com.aamoreadditions.digger;

/** Duck interface mixed into AA's TileEntityVerticalDigger. */
public interface DiggerState {
    /** false = Strict Radius (default, AA behaviour), true = Strict Chunk (whole 16x16 chunks touching the radius square). */
    boolean aamoreadditions$strictChunk();
    void aamoreadditions$setStrictChunk(boolean strictChunk);
    boolean aamoreadditions$highlight();
    void aamoreadditions$setHighlight(boolean highlight);
    /** Effective range (Phantom Boosters), synced for the work-area highlight. */
    int aamoreadditions$range();
    DiggerFillers aamoreadditions$fillers();
    DiggerWork aamoreadditions$work();
}
