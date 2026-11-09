package com.aamoreadditions.farmer;

/** Independent of native harvest cursor and smart sleep. Only runnable ticks advance. */
public final class FarmerRipeningClock {
    private boolean initialized;
    private int period,side,facing,remaining;
    private long position;
    public void reset(){initialized=false;remaining=0;}
    public boolean step(int period,int side,int facing,long position,boolean enabled,boolean runnable) {
        if(period<1)throw new IllegalArgumentException("Invalid period");
        if(!enabled){reset();return false;}
        if(!initialized || this.period!=period || this.side!=side || this.facing!=facing || this.position!=position) {
            this.period=period;this.side=side;this.facing=facing;this.position=position;remaining=period;initialized=true;
        }
        if(!runnable)return false;
        if(--remaining>0)return false;
        remaining=period;return true;
    }
}
