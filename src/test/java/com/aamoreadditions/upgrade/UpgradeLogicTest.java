package com.aamoreadditions.upgrade;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class UpgradeLogicTest {
    static UpgradeLogic.Plugin s(int n){return new UpgradeLogic.Plugin("speed",n);}
    static UpgradeLogic.Plugin e(int n){return new UpgradeLogic.Plugin("efficiency",n);}
    @Test void legacyUsesOnlyHighestPerLineEvenWithUnorderedSlots(){
        var input=List.of(s(1),s(3),e(1),s(2),e(2));
        assertEquals(List.of(s(3),e(2)),UpgradeLogic.active(input,true,UpgradeLogic.Plugin::line,UpgradeLogic.Plugin::tier));
    }
    @Test void independentUsesEveryRepeatedGrade(){
        var input=List.of(s(3),s(3),e(1),e(1));
        assertEquals(input,UpgradeLogic.active(input,false,UpgradeLogic.Plugin::line,UpgradeLogic.Plugin::tier));
    }
    @Test void emptyIsLegalInBothModes(){assertTrue(UpgradeLogic.valid(List.of(),true));assertTrue(UpgradeLogic.valid(List.of(),false));}
    @Test void dependencyNeedsEveryLowerTier(){assertFalse(UpgradeLogic.valid(List.of(s(3)),true));assertFalse(UpgradeLogic.valid(List.of(s(1),s(3)),true));assertTrue(UpgradeLogic.valid(List.of(s(3),s(1),s(2)),true));}
    @Test void differentLinesDoNotProvidePrerequisites(){assertFalse(UpgradeLogic.valid(List.of(s(1),e(2)),true));}
    @Test void dependencyRejectsDuplicates(){assertFalse(UpgradeLogic.valid(List.of(s(1),s(1)),true));}
    @Test void independentAllowsTenRepeatedHighestTiers(){assertTrue(UpgradeLogic.valid(Collections.nCopies(10,s(3)),false));}
    @Test void modeSwitchRetainsItemsButMarksInvalid(){var items=new ArrayList<>(Collections.nCopies(5,s(3)));assertTrue(UpgradeLogic.valid(items,false));assertFalse(UpgradeLogic.valid(items,true));assertEquals(5,items.size());}
    @Test void manualRepairBecomesLegalOnlyAfterAllProblemsFixed(){assertFalse(UpgradeLogic.valid(List.of(s(3),s(3)),true));assertFalse(UpgradeLogic.valid(List.of(s(3)),true));assertFalse(UpgradeLogic.valid(List.of(s(3),s(1)),true));assertTrue(UpgradeLogic.valid(List.of(s(3),s(1),s(2)),true));}
    @Test void shrinkingPreservesExactSlotIndicesAndQueuesEveryExcessItem(){var result=UpgradeLogic.resize(List.of("a","b","c","d","e"),2,"");assertEquals(List.of("a","b"),result.kept());assertEquals(List.of("c","d","e"),result.overflow());}
    @Test void zeroSlotsQueuesAllItems(){var result=UpgradeLogic.resize(List.of("a","b"),0,"");assertEquals(List.of(),result.kept());assertEquals(List.of("a","b"),result.overflow());}
    @Test void increasingToTenNeverCreatesPlugins(){var result=UpgradeLogic.resize(List.of("a","b"),10,"");assertEquals(10,result.kept().size());assertEquals(8,Collections.frequency(result.kept(),""));assertTrue(result.overflow().isEmpty());}
    @Test void savedMigratedSlotsAreNotQueuedAgain(){var once=UpgradeLogic.resize(List.of("a","b","c"),1,"");var twice=UpgradeLogic.resize(once.kept(),1,"");assertEquals(List.of("b","c"),once.overflow());assertTrue(twice.overflow().isEmpty());}
    @Test void allSizesConserveEveryItem(){for(int before=0;before<=10;before++)for(int after=0;after<=10;after++){var old=new ArrayList<String>();for(int i=0;i<before;i++)old.add("slot"+i);var r=UpgradeLogic.resize(old,after,"");var all=new ArrayList<>(r.kept());all.addAll(r.overflow());all.removeIf(String::isEmpty);assertEquals(old,all);}}
}
