package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Reads the actual AA dependency and compiled addon classes; does not apply Mixins or launch Minecraft. */
class SpecialMachineBytecodeTest {
    static ClassNode type(String name) throws IOException {
        var node=new ClassNode();
        try(var in=SpecialMachineBytecodeTest.class.getClassLoader().getResourceAsStream(name+".class")) {
            assertNotNull(in,name); new ClassReader(in).accept(node,0);
        }
        return node;
    }
    static MethodNode method(String owner,String name) throws IOException {
        return type(owner).methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
    }
    static List<MethodInsnNode> calls(MethodNode method,String owner,String name) {
        var result=new ArrayList<MethodInsnNode>();
        for(var insn:method.instructions) if(insn instanceof MethodInsnNode c && c.owner.equals(owner) && c.name.equals(name))result.add(c);
        return result;
    }
    static int integer(AbstractInsnNode insn) {
        while(insn!=null && insn.getOpcode()<0)insn=insn.getPrevious();
        if(insn instanceof IntInsnNode n)return n.operand;
        if(insn instanceof LdcInsnNode n && n.cst instanceof Integer i)return i;
        if(insn!=null && insn.getOpcode()>=Opcodes.ICONST_M1 && insn.getOpcode()<=Opcodes.ICONST_5)return insn.getOpcode()-Opcodes.ICONST_0;
        throw new AssertionError("Expected integer constant before cost STORE/call");
    }
    static final String AA="de/ellpeck/actuallyadditions/mod/";
    static final String FARMER="de/ellpeck/actuallyadditions/api/internal/IFarmer";
    static final String MOD="com/aamoreadditions/";
    @Test void allTenNativeBehaviorCostStoresReallyAreTheExpectedFirstIntLocal() throws Exception {
        Map<String,int[]> defaults=Map.of("DefaultFarmerBehavior",new int[]{350,250},"CactusFarmerBehavior",new int[]{250,250},
                "ReedFarmerBehavior",new int[]{250,250},"NetherWartFarmerBehavior",new int[]{500,500},"MelonPumpkinFarmerBehavior",new int[]{350,500});
        for(var entry:defaults.entrySet())for(int i=0;i<2;i++) {
            String name=i==0?"tryPlantSeed":"tryHarvestPlant";
            var m=method(AA+"misc/apiimpl/farmer/"+entry.getKey(),name);
            VarInsnNode first=null;
            for(var insn:m.instructions)if(insn instanceof VarInsnNode v && v.getOpcode()==Opcodes.ISTORE){first=v;break;}
            assertNotNull(first); assertEquals(entry.getValue()[i],integer(first.getPrevious()),entry.getKey()+name);
            assertFalse(calls(m,FARMER,"getEnergy").isEmpty());
            assertTrue(m.desc.contains("L"+FARMER+";"));
        }
    }
    @Test void nativeFarmerCounterAndHarvestCapacityTargetsExistAtRealDescriptors() throws Exception {
        var tick=method(AA+"tile/TileEntityFarmer","serverTick");
        assertEquals("(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",tick.desc);
        long reads=0;
        for(var insn:tick.instructions)if(insn instanceof FieldInsnNode f && f.name.equals("waitTime") && f.getOpcode()==Opcodes.GETFIELD)reads++;
        assertTrue(reads>=2);
        assertEquals("(Ljava/util/List;)Z",method(AA+"tile/TileEntityFarmer","canAddToOutput").desc);
        assertEquals(1,calls(method(AA+"tile/TileEntityFarmer","checkBehaviors"),
                "de/ellpeck/actuallyadditions/api/farmer/IFarmerBehavior","tryHarvestPlant").size());
    }
    @Test void nativeDefaultHarvestLiteralAndAddonScalingAreEachSingleCalls() throws Exception {
        var nativeHelper=method(AA+"misc/apiimpl/farmer/DefaultFarmerBehavior","doFarmerStuff");
        var charges=calls(nativeHelper,FARMER,"extractEnergy");
        assertEquals(1,charges.size()); assertEquals(250,integer(charges.getFirst().getPrevious()));
        var adapter=method(MOD+"mixin/FarmerDefaultHarvestMixin","aamoreadditions$matchingHarvestCharge");
        assertEquals(1,calls(adapter,MOD+"effect/FarmerEffects","cost").size());
    }
    @Test void nativeEmpowererReallyDividesPerStandEnergyByRecipeTime() throws Exception {
        var tick=method(AA+"tile/TileEntityEmpowerer","serverTick");
        assertEquals(2,calls(tick,AA+"crafting/EmpowererRecipe","getEnergyPerStand").size());
        assertEquals(3,calls(tick,AA+"crafting/EmpowererRecipe","getTime").size());
        int divisions=0;for(var insn:tick.instructions)if(insn.getOpcode()==Opcodes.IDIV)divisions++;
        assertEquals(2,divisions);
        assertEquals(1,calls(tick,AA+"tile/CustomEnergyStorage","extractEnergyInternal").size());
    }
    @Test void compiledCrossRuntimeAndMenuUseSameDedicatedAdapterExactlyOnce() throws Exception {
        var common=method(MOD+"structure/CasingCrossData","speedMultiplier");
        assertEquals(1,calls(common,MOD+"effect/CrossEffects","speed").size());
        var tick=method(MOD+"structure/CasingCrossData$Entry","tick");
        assertEquals(1,calls(tick,MOD+"structure/CasingCrossData","speedMultiplier").size());
        assertEquals(1,calls(tick,MOD+"effect/UpgradeMath","time").size());
        assertEquals(1,calls(tick,MOD+"structure/CasingCrossData$Energy","extractInternal").size());
        assertEquals(1,calls(method(MOD+"menu/CasingCrossMenu","speedMultiplier"),MOD+"structure/CasingCrossData","speedMultiplier").size());
    }
    @Test void compiledFarmerMathAndAdaptersNeverReadEditableProcessorConfig() throws Exception {
        for(String owner:List.of("effect/FarmerEffects","effect/FarmerUpgradeMath","farmer/FarmerRipeningMath"))
            for(var method:type(MOD+owner).methods)for(var insn:method.instructions)
                if(insn instanceof MethodInsnNode c)assertNotEquals(MOD+"effect/UpgradeConfig",c.owner,owner+method.name);
        var info=method(MOD+"client/UpgradeInfo","lines");
        assertEquals(1,calls(info,MOD+"effect/FarmerEffects","evaluate").size());
        assertEquals(1,calls(info,MOD+"effect/FarmerScanRuntime","cooldown").size());
    }
    @Test void nativeEnergizerRemainsCappedWhileFarmerUsesLocalUnlimitedComponentAdapter() throws Exception {
        int receives=0;
        for(var m:type(AA+"tile/TileEntityEnergizer").methods)
            receives+=calls(m,"net/neoforged/neoforge/energy/IEnergyStorage","receiveEnergy").size();
        assertEquals(1,receives);
        var farmer=method(MOD+"farmer/FarmerRipeningRuntime","afterNative");
        assertEquals(0,calls(farmer,"net/neoforged/neoforge/energy/IEnergyStorage","receiveEnergy").size());
        assertEquals(1,calls(farmer,MOD+"farmer/FarmerRingCharging","charge").size());
        var adapter=method(MOD+"farmer/FarmerRingCharging","receive");
        assertEquals(1,calls(adapter,"net/neoforged/neoforge/energy/ComponentEnergyStorage","receiveEnergy").size());
        assertTrue(adapter.instructions.iterator().hasNext());
        var charge=method(MOD+"farmer/FarmerRingCharging","charge");
        assertEquals(1,calls(charge,"net/neoforged/neoforge/energy/IEnergyStorage","getMaxEnergyStored").size());
        assertTrue(Arrays.stream(charge.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f && f.owner.equals(AA+"components/ActuallyComponents") && f.name.equals("ENERGY_STORAGE")));
        assertTrue(type(AA+"items/ActuallyItems").methods.stream().anyMatch(m->!calls(m,"net/neoforged/neoforge/energy/ComponentEnergyStorage","<init>").isEmpty()));
        assertEquals(1,calls(farmer,AA+"tile/CustomEnergyStorage","extractEnergyInternal").size());
    }
    @Test void jadeUsesRealServerHandlerActivationNotAnotherCoefficientSource() throws Exception {
        var data=method(MOD+"compat/jade/UpgradeJadeProvider","appendServerData");
        assertEquals(1,calls(data,MOD+"upgrade/UpgradeSlots","target").size());
        assertEquals(1,calls(data,MOD+"upgrade/UpgradeSlots$Handler","activeUpgrades").size());
        assertEquals(1,calls(data,MOD+"upgrade/UpgradeSlots$Handler","legal").size());
    }
    @Test void nativeFarmerBaseStorageIsCapturedFromTheActualDependency() throws Exception {
        var ctor=method(AA+"tile/TileEntityFarmer","<init>");
        var storage=calls(ctor,AA+"tile/CustomEnergyStorage","<init>"); assertEquals(1,storage.size());
        var last=storage.getFirst().getPrevious();
        assertEquals(0,integer(last)); assertEquals(1000,integer(last.getPrevious()));
        assertEquals(100000,integer(last.getPrevious().getPrevious()));
    }
    @Test void compiledMenuReconstructsBothFullIntProgressValuesFromTwoWords() throws Exception {
        for(var name:List.of("progress","recipeTime")) {
            var method=method(MOD+"menu/CasingCrossMenu",name);
            var gets=calls(method,"net/minecraft/world/inventory/ContainerData","get");
            assertEquals(2,gets.size());
            assertEquals(name.equals("progress")?2:3,integer(gets.get(0).getPrevious()));
            assertEquals(name.equals("progress")?7:8,integer(gets.get(1).getPrevious()));
            assertTrue(calls(method,"java/lang/Math","min").isEmpty());
        }
    }
    @Test void successfulPlantingWrapperTargetsTheActualNativeCallAndReachesScanState() throws Exception {
        var calls=calls(method(AA+"tile/TileEntityFarmer","checkBehaviors"),"de/ellpeck/actuallyadditions/api/farmer/IFarmerBehavior","tryPlantSeed");
        assertEquals(1,calls.size());
        assertEquals("(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;L"+FARMER+";)Lde/ellpeck/actuallyadditions/api/farmer/FarmerResult;",calls.getFirst().desc);
        var wrapper=method(MOD+"mixin/TileEntityFarmerMixin","aamoreadditions$plantingWork");
        assertEquals(1,calls(wrapper,"com/llamalad7/mixinextras/injector/wrapoperation/Operation","call").size());
        assertTrue(Arrays.stream(wrapper.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f && f.name.equals("SUCCESS")));
        assertEquals(1,calls(method(MOD+"effect/FarmerScanRuntime","checked"),MOD+"effect/FarmerScanState","planted").size());
    }
    @Test void wrenchOnlyWrongLogicActionbarGetsRedNotSuccessNotifications() throws Exception {
        var m=method(MOD+"item/UpgradeWrenchItem","onRightClick");
        String message=null;boolean red=false;Map<String,Boolean> notifications=new HashMap<>();
        for(var i:m.instructions) {
            if(i instanceof LdcInsnNode l && l.cst instanceof String s && s.startsWith("message.aamoreadditions.")){message=s;red=false;}
            if(i instanceof FieldInsnNode f && f.owner.equals("net/minecraft/ChatFormatting") && f.name.equals("RED"))red=true;
            if(i instanceof MethodInsnNode c && c.name.equals("displayClientMessage")){notifications.put(message,red);message=null;red=false;}
        }
        assertEquals(Boolean.TRUE,notifications.get("message.aamoreadditions.upgrades.wrong_logic"));
        assertEquals(Boolean.FALSE,notifications.get("message.aamoreadditions.upgrade_wrench.recorded"));
        assertEquals(Boolean.FALSE,notifications.get("message.aamoreadditions.upgrade_wrench.installed"));
    }
}
