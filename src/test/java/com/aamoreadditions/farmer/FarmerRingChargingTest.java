package com.aamoreadditions.farmer;

import net.minecraft.core.component.*;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Actual NeoForge component energy storage; one-component in-memory holder,
 * not a bootstrapped ItemStack, NBT or game integration test. */
class FarmerRingChargingTest {
    private static final class Holder implements MutableDataComponentHolder {
        int energy;
        Holder(int energy){this.energy=energy;}
        // The fake holder has exactly one component; null is only a test key.
        @Override public DataComponentMap getComponents(){throw new UnsupportedOperationException();}
        @Override @SuppressWarnings("unchecked") public <T> T get(DataComponentType<? extends T> type){return (T)Integer.valueOf(energy);}
        @Override @SuppressWarnings("unchecked") public <T> T getOrDefault(DataComponentType<? extends T> type,T fallback){return (T)Integer.valueOf(energy);}
        @Override @SuppressWarnings("unchecked") public <T> T set(DataComponentType<? super T> type,T value){int old=energy;energy=(Integer)value;return (T)Integer.valueOf(old);}
        @Override public <T> T remove(DataComponentType<? extends T> type){throw new UnsupportedOperationException();}
        @Override public void applyComponents(DataComponentPatch patch){throw new UnsupportedOperationException();}
        @Override public void applyComponents(DataComponentMap components){throw new UnsupportedOperationException();}
    }
    @Test void farmerAcceptsFarMoreThanTwoThousandInOneCall() {
        var ring=new Holder(0);assertEquals(50000,FarmerRingCharging.receive(ring,null,1000000,50000));assertEquals(50000,ring.energy);
    }
    @Test void remainingRingCapacityIsTheOnlyReceiverLimit() {
        var ring=new Holder(999995);assertEquals(5,FarmerRingCharging.receive(ring,null,1000000,50000));assertEquals(1000000,ring.energy);
    }
    @Test void zeroMachineEnergyTransfersNothingEvenToEmptyRing() {
        var ring=new Holder(0);assertEquals(0,FarmerRingCharging.receive(ring,null,1000000,0));assertEquals(0,ring.energy);
    }
    @Test void fullRingDoesNotChargeMachine() {
        var ring=new Holder(1000000);assertEquals(0,FarmerRingCharging.receive(ring,null,1000000,50000));
    }
    @Test void originalCapabilityRemainsCappedButReadsTheSameUpdatedComponent() {
        var ring=new Holder(100);var original=new ComponentEnergyStorage(ring,null,1000000,2000);
        assertEquals(2000,original.receiveEnergy(50000,false));
        assertEquals(50000,FarmerRingCharging.receive(ring,null,1000000,50000));assertEquals(52100,original.getEnergyStored());
        assertEquals(2000,original.receiveEnergy(50000,false));
    }
    @Test void actualTransferredAmountConservesMachineAndRingEnergy() {
        var ring=new Holder(999500);int machine=100000;
        int received=FarmerRingCharging.receive(ring,null,1000000,machine);machine-=received;
        assertEquals(500,received);assertEquals(999500+100000,ring.energy+machine);
    }
    @Test void integerMaximumDoesNotOverflowCapacityOrCreateEnergy() {
        var ring=new Holder(Integer.MAX_VALUE-100);
        assertEquals(100,FarmerRingCharging.receive(ring,null,Integer.MAX_VALUE,Integer.MAX_VALUE));assertEquals(Integer.MAX_VALUE,ring.energy);
    }
}
