package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EfficientGroupingTest {
    record R(String id, EfficientGrouping.Shape shape) {}
    static EfficientGrouping.Shape sh(EfficientKind k, int n, int c, int t) { return new EfficientGrouping.Shape(k, n, c, t); }

    @Test void oneEntryPerShapeNotPerMetal() {
        List<R> rs = new java.util.ArrayList<>();
        for (String m : List.of("copper", "iron", "tin", "osmium")) {
            rs.add(new R(m + "/dust", sh(EfficientKind.DUST, 1, 1, 80)));
            rs.add(new R(m + "/raw_block", sh(EfficientKind.RAW_BLOCK, 1, 16, 640)));
            rs.add(new R(m + "/ore", sh(EfficientKind.ORE, 1, 2, 80)));
            rs.add(new R(m + "/raw_x3", sh(EfficientKind.RAW_ORE, 3, 5, 80)));
            rs.add(new R(m + "/raw_x2", sh(EfficientKind.RAW_ORE, 2, 3, 80)));
        }
        rs.add(new R("kubejs:fast_tin_ore", sh(EfficientKind.ORE, 1, 2, 40)));   // custom time -> own entry
        rs.add(new R("kubejs:weird", sh(EfficientKind.OTHER, 2, 1, 80)));        // custom shape -> own entry
        var g = EfficientGrouping.group(rs, R::shape);
        assertEquals(List.of(sh(EfficientKind.RAW_ORE, 2, 3, 80), sh(EfficientKind.RAW_ORE, 3, 5, 80),
                sh(EfficientKind.ORE, 1, 2, 40), sh(EfficientKind.ORE, 1, 2, 80),
                sh(EfficientKind.RAW_BLOCK, 1, 16, 640), sh(EfficientKind.DUST, 1, 1, 80), sh(EfficientKind.OTHER, 2, 1, 80)),
                g.stream().map(Map.Entry::getKey).toList());
        assertEquals(List.of("copper/raw_x2", "iron/raw_x2", "tin/raw_x2", "osmium/raw_x2"), g.get(0).getValue().stream().map(R::id).toList());
    }

    @Test void rowsStayInSyncPerMetal() {
        // tin: 2 raw variants, 1 ingot; iron: 1 raw, 2 ingot variants; lead: no ingot -> skipped
        var tin = List.of(List.of("rawtinA", "rawtinB"), List.of("rawtinA", "rawtinB"), List.of("tinIngot"));
        var iron = List.of(List.of("rawiron"), List.of("rawiron"), List.of("ironA", "ironB"));
        List<List<String>> lead = List.of(List.of("rawlead"), List.of("rawlead"), List.of());
        var rows = EfficientGrouping.align(List.of(tin, iron, lead));
        assertEquals(List.of(
                List.of("rawtinA", "rawtinA", "tinIngot"), List.of("rawtinB", "rawtinB", "tinIngot"),
                List.of("rawiron", "rawiron", "ironA"), List.of("rawiron", "rawiron", "ironB")), rows);
        assertEquals(List.of("tinIngot", "tinIngot", "ironA", "ironB"), EfficientGrouping.column(rows, 2));
    }

    @Test void focusShowsOnlyThatMetal() {
        var rows = EfficientGrouping.align(List.of(
                List.of(List.of("rawtin"), List.of("tin")), List.of(List.of("rawiron"), List.of("iron"))));
        assertEquals(List.of(List.of("rawtin", "tin")), EfficientGrouping.focus(rows, "rawtin", false, String::equals));
        assertEquals(List.of(List.of("rawiron", "iron")), EfficientGrouping.focus(rows, "iron", true, String::equals));
        assertEquals(rows, EfficientGrouping.focus(rows, "furnace", false, String::equals)); // catalyst etc. -> everything
        assertEquals(rows, EfficientGrouping.focus(rows, null, false, String::equals));
    }
}
