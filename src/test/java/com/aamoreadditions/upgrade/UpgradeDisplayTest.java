package com.aamoreadditions.upgrade;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure grouping/selection tests; not a Minecraft or Jade renderer integration test. */
class UpgradeDisplayTest {
    record P(String id, String line, int tier, String label) {}
    static final P S1 = new P("speed_upgrade_1", "speed", 1, "Speed I");
    static final P S2 = new P("speed_upgrade_2", "speed", 2, "Speed II");
    static final P S3 = new P("speed_upgrade_3", "speed", 3, "Speed III");
    static final P E1 = new P("efficiency_upgrade_1", "efficiency", 1, "Efficiency I");
    static final P E2 = new P("efficiency_upgrade_2", "efficiency", 2, "Efficiency II");

    static List<UpgradeDisplay.Group<P>> selected(List<P> installed, boolean dependency) {
        var plugins = installed.stream().map(p -> new UpgradeLogic.Plugin(p.line(), p.tier())).toList();
        boolean legal = UpgradeLogic.valid(plugins, dependency);
        var active = UpgradeLogic.active(installed, dependency, P::line, P::tier);
        return UpgradeDisplay.group(UpgradeDisplay.visible(installed, dependency, legal, active::contains), P::id);
    }

    @Test void independentAggregatesTenSlotsByExactGrade() {
        var groups = selected(List.of(S1,S2,S1,E2,S3,E1,E2,S2,S3,S1), false);
        assertEquals(List.of(S1,S2,E2,S3,E1), groups.stream().map(UpgradeDisplay.Group::representative).toList());
        assertEquals(List.of(3,2,2,2,1), groups.stream().map(UpgradeDisplay.Group::count).toList());
        assertEquals(10, groups.stream().mapToInt(UpgradeDisplay.Group::count).sum());
    }
    @Test void sameItemIdGroupsDespiteNamesAndComponents() {
        var renamed = new P(S1.id(), S1.line(), S1.tier(), "Renamed custom-component item");
        var groups = selected(List.of(S1, renamed), false);
        assertEquals(1,groups.size()); assertEquals(2,groups.getFirst().count());
        assertSame(S1,groups.getFirst().representative());
    }
    @Test void translatedNameAndLineDoNotMergeDifferentItemIds() {
        var sameName = new P(S2.id(), S2.line(), S2.tier(), S1.label());
        assertEquals(2, selected(List.of(S1,sameName),false).size());
    }
    @Test void dependencyUsesHighestOnBothLinesNotGlobalHighest() {
        var groups = selected(List.of(S3,E1,S1,E2,S2), true);
        assertEquals(List.of(S3,E2),groups.stream().map(UpgradeDisplay.Group::representative).toList());
        assertEquals(List.of(1,1),groups.stream().map(UpgradeDisplay.Group::count).toList());
    }
    @Test void efficiencyOnlyAndSpeedOnlyHandlersRetainTheirHighestGrade() {
        assertEquals(E2, selected(List.of(E1,E2),true).getFirst().representative());
        assertEquals(S3, selected(List.of(S1,S3,S2),true).getFirst().representative());
    }
    @Test void illegalMissingPrerequisiteNeverAdvertisesActivation() {
        assertTrue(selected(List.of(S3),true).isEmpty());
        assertTrue(selected(List.of(E2),true).isEmpty());
    }
    @Test void illegalDuplicateNeverAdvertisesEvenOtherwiseValidHighest() {
        assertTrue(selected(List.of(S1,S2,S3,S1,E1,E2),true).isEmpty());
    }
    @Test void zeroSlotsAndLastRemovalYieldEmptyFreshPayloadGroups() {
        assertEquals(1,selected(List.of(S1),false).size());
        assertTrue(selected(List.of(),false).isEmpty());
        assertTrue(selected(List.of(),true).isEmpty());
    }
    @Test void independentDoesNotConsultHighestTierPredicate() {
        assertEquals(List.of(S1,S2,S2), UpgradeDisplay.visible(List.of(S1,S2,S2),false,true,p -> {
            fail("Independent mode must not filter to highest tiers"); return false;
        }));
    }
    @Test void tenIdenticalIconsBecomeOneTimesTen() {
        var groups = selected(Collections.nCopies(10,E2),false);
        assertEquals(1,groups.size());assertEquals(10,groups.getFirst().count());
    }
    @Test void rowsWrapAllFiveKnownAndTenExternallyTaggedIds() {
        assertEquals(List.of(3,2),UpgradeDisplay.rows(selected(List.of(S1,S2,S3,E1,E2),false)).stream().map(List::size).toList());
        var ids = java.util.stream.IntStream.range(0,10).mapToObj(i -> "external:"+i).toList();
        var groups = UpgradeDisplay.group(ids,id->id);
        assertEquals(List.of(3,3,3,1),UpgradeDisplay.rows(groups).stream().map(List::size).toList());
        assertTrue(UpgradeDisplay.rows(List.of()).isEmpty());
    }
    @Test void presentationDoesNotMutateOrAliasInventoryLists() {
        var installed = new ArrayList<>(List.of(S1,S1,E1));
        var groups = selected(installed,false);
        assertEquals(3,installed.size()); installed.clear();
        assertEquals(2,groups.getFirst().count());assertEquals(2,groups.size());
        assertThrows(UnsupportedOperationException.class,()->groups.clear());
    }
}
