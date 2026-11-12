package com.aamoreadditions.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Alloying result. Two JSON forms (both usable from KubeJS event.custom):
 * <pre>
 *   {"id":"mekanism:ingot_bronze","count":4}   fixed item; its same-tag group is the item's c:&lt;kind&gt;/&lt;material&gt; tag
 *   {"tag":"c:ingots/bronze","count":4}        any item of the tag (the player picks one per furnace when there are several)
 * </pre>
 * A tag with no items makes the recipe unavailable (hidden in JEI, never matches).
 */
public final class AlloyResult {
    private final ItemStack item;      // id form (EMPTY for tag form)
    private final TagKey<Item> tag;    // tag form (null for id form)
    private final int count;

    private AlloyResult(ItemStack item, TagKey<Item> tag, int count) { this.item = item; this.tag = tag; this.count = count; }

    public static AlloyResult ofItem(ItemStack stack) { return new AlloyResult(stack.copy(), null, stack.getCount()); }
    public static AlloyResult ofTag(TagKey<Item> tag, int count) { return new AlloyResult(ItemStack.EMPTY, tag, count); }

    public boolean isTag() { return tag != null; }
    public TagKey<Item> tag() { return tag; }
    /** The declared item (id form) or EMPTY (tag form). */
    public ItemStack fixedItem() { return item; }
    public int count() { return count; }

    /** Same-tag group id used as the choice key, or null if this result can only be its fixed item. */
    public String group() {
        if (tag != null) return tag.location().toString();
        List<String> tags = new ArrayList<>();
        item.getItem().builtInRegistryHolder().tags().forEach(t -> tags.add(t.location().toString()));
        return OutputChoices.materialTag(tags);
    }

    /** All items this result may produce in the current game (sorted by id, with the result count). */
    public List<ItemStack> candidates() {
        String g = group();
        if (g == null) return item.isEmpty() ? List.of() : List.of(item.copy());
        ResourceLocation rl = ResourceLocation.tryParse(g);
        List<ItemStack> out = new ArrayList<>();
        if (rl != null) {
            TagKey<Item> key = TagKey.create(Registries.ITEM, rl);
            BuiltInRegistries.ITEM.getTag(key).ifPresent(set -> {
                for (Holder<Item> h : set) {
                    // keep the declared stack (incl. components) for the fixed item itself
                    out.add(!item.isEmpty() && h.value() == item.getItem() ? item.copy() : new ItemStack(h, count));
                }
            });
        }
        if (out.isEmpty() && !item.isEmpty()) out.add(item.copy()); // id form whose tag lookup is empty
        out.sort(Comparator.comparing(s -> idOf(s.getItem())));
        // de-duplicate (a tag may list an item twice through nested tags)
        List<ItemStack> uniq = new ArrayList<>();
        String last = null;
        for (ItemStack s : out) { String id = idOf(s.getItem()); if (!id.equals(last)) uniq.add(s); last = id; }
        return uniq;
    }

    public List<String> candidateIds() {
        List<String> l = new ArrayList<>();
        for (ItemStack s : candidates()) l.add(idOf(s.getItem()));
        return l;
    }

    /** True if at least one item can be produced in this game. */
    public boolean available() {
        if (tag == null) return !item.isEmpty();
        return BuiltInRegistries.ITEM.getTag(tag).map(set -> set.size() > 0).orElse(false);
    }

    /** The candidate stack for an item id (EMPTY if not a candidate). */
    public ItemStack stackFor(String itemId) {
        if (itemId == null) return ItemStack.EMPTY;
        for (ItemStack s : candidates()) if (idOf(s.getItem()).equals(itemId)) return s.copy();
        return ItemStack.EMPTY;
    }

    /** Resolve against a furnace's saved choices. */
    public OutputChoices.Resolution resolve(OutputChoices choices) { return choices.resolve(group(), candidateIds()); }

    /** Stack to produce for a resolution, or EMPTY. */
    public ItemStack produce(OutputChoices choices) {
        var r = resolve(choices);
        return r.producible() ? stackFor(r.item()) : ItemStack.EMPTY;
    }

    /** Item ids currently in a tag (empty if unknown tag). */
    public static List<String> itemsInTag(String tagId) {
        ResourceLocation rl = tagId == null ? null : ResourceLocation.tryParse(tagId);
        if (rl == null) return List.of();
        List<String> l = new ArrayList<>();
        BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl)).ifPresent(set -> { for (Holder<Item> h : set) l.add(idOf(h.value())); });
        return l;
    }

    public static String idOf(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }

    // --- codecs ---
    private record TagForm(TagKey<Item> tag, int count) {}

    private static final Codec<TagForm> TAG_FORM = RecordCodecBuilder.create(i -> i.group(
            TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(TagForm::tag),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(TagForm::count)
    ).apply(i, TagForm::new));

    public static final Codec<AlloyResult> CODEC = Codec.either(ItemStack.STRICT_CODEC, TAG_FORM).xmap(
            e -> e.map(AlloyResult::ofItem, t -> ofTag(t.tag(), t.count())),
            r -> r.isTag() ? Either.right(new TagForm(r.tag, r.count)) : Either.left(r.item));

    private static final StreamCodec<RegistryFriendlyByteBuf, TagForm> TAG_STREAM = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.map(rl -> TagKey.create(Registries.ITEM, rl), TagKey::location), TagForm::tag,
            ByteBufCodecs.VAR_INT, TagForm::count, TagForm::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyResult> STREAM_CODEC = ByteBufCodecs.either(ItemStack.STREAM_CODEC, TAG_STREAM).map(
            e -> e.map(AlloyResult::ofItem, t -> ofTag(t.tag(), t.count())),
            r -> r.isTag() ? Either.right(new TagForm(r.tag, r.count)) : Either.left(r.item));
}
