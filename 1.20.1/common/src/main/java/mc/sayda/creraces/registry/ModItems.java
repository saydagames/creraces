package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.item.CommandingStaffItem;
import mc.sayda.creraces.item.CustomBoatItem;
import mc.sayda.creraces.item.MermaidArmorMaterial;
import mc.sayda.creraces.item.ScrollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ModItems {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CreRaces.MODID, Registries.ITEM);

        public static final RegistrySupplier<Item> ABILITY_SCROLL = ITEMS.register("ability_scroll",
                        () -> new ScrollItem(new Item.Properties().stacksTo(1)));

        public static final RegistrySupplier<Item> QUEST_SCROLL = ITEMS.register("quest_scroll",
                        () -> new mc.sayda.creraces.item.QuestScrollItem(new Item.Properties().stacksTo(1)));

        public static final RegistrySupplier<Item> MIRROR = ITEMS.register("mirror",
                        () -> new mc.sayda.creraces.item.MirrorItem(new Item.Properties().stacksTo(1)));

        public static final RegistrySupplier<Item> HARPY_FEATHER = ITEMS.register("harpy_feather",
                        () -> new Item(new Item.Properties()));

        public static final RegistrySupplier<Item> TOWEL = ITEMS.register("towel",
                        () -> new mc.sayda.creraces.item.TowelItem(new Item.Properties()));

        public static final RegistrySupplier<Item> OLD_BELL = ITEMS.register("old_bell",
                        () -> new Item(new Item.Properties()));

        public static final RegistrySupplier<Item> DIME = ITEMS.register("dime",
                        () -> new mc.sayda.creraces.item.currency.DimeItem(new Item.Properties()));

        public static final RegistrySupplier<Item> PENNY = ITEMS.register("penny",
                        () -> new mc.sayda.creraces.item.currency.PennyItem(new Item.Properties()));

        public static final RegistrySupplier<Item> DRYAD_APPLE = ITEMS.register("dryad_apple",
                        () -> new mc.sayda.creraces.item.DryadAppleItem(
                                        mc.sayda.creraces.item.DryadAppleItem.Variant.DEFAULT,
                                        new Item.Properties()));

        public static final RegistrySupplier<Item> GOLDEN_DRYAD_APPLE = ITEMS.register("golden_dryad_apple",
                        () -> new mc.sayda.creraces.item.DryadAppleItem(
                                        mc.sayda.creraces.item.DryadAppleItem.Variant.GOLDEN,
                                        new Item.Properties()));

        public static final RegistrySupplier<Item> ENCHANTED_GOLDEN_DRYAD_APPLE = ITEMS.register(
                        "enchanted_golden_dryad_apple",
                        () -> new mc.sayda.creraces.item.DryadAppleItem(
                                        mc.sayda.creraces.item.DryadAppleItem.Variant.ENCHANTED,
                                        new Item.Properties()));

        public static final RegistrySupplier<Item> COMMANDING_STAFF = ITEMS.register("commanding_staff",
                        () -> new CommandingStaffItem(new Item.Properties().stacksTo(1)));

        public static final RegistrySupplier<Item> ESSENCE_BUCKET = ITEMS.register("essence_bucket",
                        () -> new mc.sayda.creraces.item.EssenceBucketItem(new Item.Properties().stacksTo(1)));

        // Boats
        public static final RegistrySupplier<Item> DRYAD_BOAT_ITEM = ITEMS.register("dryad_boat",
                        () -> new CustomBoatItem(() -> ModEntities.DRYAD_BOAT.get(), new Item.Properties().stacksTo(1)));
        public static final RegistrySupplier<Item> DRYAD_CHEST_BOAT_ITEM = ITEMS.register("dryad_chest_boat",
                        () -> new CustomBoatItem(() -> ModEntities.DRYAD_CHEST_BOAT.get(), new Item.Properties().stacksTo(1)));
        public static final RegistrySupplier<Item> VEIL_WILLOW_BOAT_ITEM = ITEMS.register("veil_willow_boat",
                        () -> new CustomBoatItem(() -> ModEntities.VEIL_WILLOW_BOAT.get(), new Item.Properties().stacksTo(1)));
        public static final RegistrySupplier<Item> VEIL_WILLOW_CHEST_BOAT_ITEM = ITEMS.register("veil_willow_chest_boat",
                        () -> new CustomBoatItem(() -> ModEntities.VEIL_WILLOW_CHEST_BOAT.get(), new Item.Properties().stacksTo(1)));

        // Block Items
        public static final RegistrySupplier<Item> DRYAD_LOG_ITEM = registerBlockItem(ModBlocks.DRYAD_LOG);
        public static final RegistrySupplier<Item> DRYAD_LOG_CORE_ITEM = registerBlockItem(ModBlocks.DRYAD_LOG_CORE);
        public static final RegistrySupplier<Item> DRYAD_WOOD_ITEM = registerBlockItem(ModBlocks.DRYAD_WOOD);
        public static final RegistrySupplier<Item> DRYAD_PETRIFIED_WOOD_ITEM = registerBlockItem(
                        ModBlocks.DRYAD_PETRIFIED_WOOD);
        public static final RegistrySupplier<Item> DRYAD_PLANKS_ITEM = registerBlockItem(ModBlocks.DRYAD_PLANKS);
        public static final RegistrySupplier<Item> DRYAD_LEAVES_ITEM = registerBlockItem(ModBlocks.DRYAD_LEAVES);
        public static final RegistrySupplier<Item> DRYAD_LEAVES_FLOWERING_ITEM = registerBlockItem(
                        ModBlocks.DRYAD_LEAVES_FLOWERING);
        public static final RegistrySupplier<Item> DRYAD_LEAVES_FRUIT_ITEM = registerBlockItem(
                        ModBlocks.DRYAD_LEAVES_FRUIT);
        public static final RegistrySupplier<Item> DRYAD_SAPLING_ITEM = registerBlockItem(ModBlocks.DRYAD_SAPLING);
        public static final RegistrySupplier<Item> DRYAD_ROOT_ITEM = registerBlockItem(ModBlocks.DRYAD_ROOT);
        public static final RegistrySupplier<Item> DRYAD_TOTEM_ITEM = registerBlockItem(ModBlocks.DRYAD_TOTEM);
        public static final RegistrySupplier<Item> SUMMONED_DIRT_ITEM = registerBlockItem(ModBlocks.SUMMONED_DIRT);
        public static final RegistrySupplier<Item> AURAI_SCULPTURE_ITEM = registerBlockItem(ModBlocks.AURAI_SCULPTURE);
        public static final RegistrySupplier<Item> NAIAD_STATUE_ITEM = registerBlockItem(ModBlocks.NAIAD_STATUE);
        public static final RegistrySupplier<Item> OREAD_IDOL_ITEM = registerBlockItem(ModBlocks.OREAD_IDOL);
        public static final RegistrySupplier<Item> DRYAD_LANTERN_ITEM = registerBlockItem(ModBlocks.DRYAD_LANTERN);
        public static final RegistrySupplier<Item> DRYAD_EXPANSION_PANEL_ITEM = registerBlockItem(
                        ModBlocks.DRYAD_EXPANSION_PANEL);
        public static final RegistrySupplier<Item> DRYAD_STAIRS_ITEM = registerBlockItem(ModBlocks.DRYAD_STAIRS);
        public static final RegistrySupplier<Item> DRYAD_SLAB_ITEM = registerBlockItem(ModBlocks.DRYAD_SLAB);
        public static final RegistrySupplier<Item> DRYAD_FENCE_ITEM = registerBlockItem(ModBlocks.DRYAD_FENCE);
        public static final RegistrySupplier<Item> DRYAD_FENCE_GATE_ITEM = registerBlockItem(
                        ModBlocks.DRYAD_FENCE_GATE);
        public static final RegistrySupplier<Item> DRYAD_BUTTON_ITEM = registerBlockItem(ModBlocks.DRYAD_BUTTON);
        public static final RegistrySupplier<Item> DRYAD_PRESSURE_PLATE_ITEM = registerBlockItem(
                        ModBlocks.DRYAD_PRESSURE_PLATE);
        public static final RegistrySupplier<Item> DRYAD_TRAPDOOR_ITEM = registerBlockItem(ModBlocks.DRYAD_TRAPDOOR);
        public static final RegistrySupplier<Item> DRYAD_DOOR_ITEM = registerBlockItem(ModBlocks.DRYAD_DOOR);

        public static final RegistrySupplier<Item> SPRING_FAIRY_ORB_ITEM = registerBlockItem(
                        ModBlocks.SPRING_FAIRY_ORB);
        public static final RegistrySupplier<Item> SUMMER_FAIRY_ORB_ITEM = registerBlockItem(
                        ModBlocks.SUMMER_FAIRY_ORB);
        public static final RegistrySupplier<Item> AUTUMN_FAIRY_ORB_ITEM = registerBlockItem(
                        ModBlocks.AUTUMN_FAIRY_ORB);
        public static final RegistrySupplier<Item> WINTER_FAIRY_ORB_ITEM = registerBlockItem(
                        ModBlocks.WINTER_FAIRY_ORB);
        public static final RegistrySupplier<Item> DAY_FAIRY_ORB_ITEM = registerBlockItem(ModBlocks.DAY_FAIRY_ORB);
        public static final RegistrySupplier<Item> NIGHT_FAIRY_ORB_ITEM = registerBlockItem(ModBlocks.NIGHT_FAIRY_ORB);
        public static final RegistrySupplier<Item> TORII_BELL_ITEM = registerBlockItem(ModBlocks.TORII_BELL);
        public static final RegistrySupplier<Item> WEATHERED_TORII_BELL_ITEM = registerBlockItem(
                        ModBlocks.WEATHERED_TORII_BELL);
        public static final RegistrySupplier<Item> RED_STRIPPED_OAK_LOG_ITEM = registerBlockItem(
                        ModBlocks.RED_STRIPPED_OAK_LOG);
        public static final RegistrySupplier<Item> WEATHERED_RED_STRIPPED_OAK_LOG_ITEM = registerBlockItem(
                        ModBlocks.WEATHERED_RED_STRIPPED_OAK_LOG);
        public static final RegistrySupplier<Item> VOLCANIC_ROCK_ITEM = registerBlockItem(
                        ModBlocks.VOLCANIC_ROCK);
        public static final RegistrySupplier<Item> VOLCANIC_ROCK_HARDENED_ITEM = registerBlockItem(
                        ModBlocks.VOLCANIC_ROCK_HARDENED);
        public static final RegistrySupplier<Item> CHERRY_TREE_GATEWAY_ITEM = registerBlockItem(
                        ModBlocks.CHERRY_TREE_GATEWAY);
        public static final RegistrySupplier<Item> OAK_TREE_GATEWAY_ITEM = registerBlockItem(
                        ModBlocks.OAK_TREE_GATEWAY);
        public static final RegistrySupplier<Item> SPRUCE_TREE_GATEWAY_ITEM = registerBlockItem(
                        ModBlocks.SPRUCE_TREE_GATEWAY);
        public static final RegistrySupplier<Item> VEIL_MUSHROOM_ITEM = registerBlockItem(ModBlocks.VEIL_MUSHROOM);
        public static final RegistrySupplier<Item> VEIL_GRIT_ITEM = registerBlockItem(ModBlocks.VEIL_GRIT);
        public static final RegistrySupplier<Item> VEIL_BLOOM_ITEM = registerBlockItem(
                        ModBlocks.VEIL_BLOOM);
        public static final RegistrySupplier<Item> VEIL_WILLOW_LOG_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_LOG);
        public static final RegistrySupplier<Item> VEIL_WILLOW_LEAVES_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_LEAVES);
        public static final RegistrySupplier<Item> VEIL_WILLOW_DRAPE_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_DRAPE);
        public static final RegistrySupplier<Item> VEIL_WILLOW_WOOD_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_WOOD);
        public static final RegistrySupplier<Item> VEIL_WILLOW_PLANKS_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_PLANKS);
        public static final RegistrySupplier<Item> VEIL_WILLOW_STAIRS_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_STAIRS);
        public static final RegistrySupplier<Item> VEIL_WILLOW_SLAB_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_SLAB);
        public static final RegistrySupplier<Item> VEIL_WILLOW_FENCE_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_FENCE);
        public static final RegistrySupplier<Item> VEIL_WILLOW_FENCE_GATE_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_FENCE_GATE);
        public static final RegistrySupplier<Item> VEIL_WILLOW_BUTTON_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_BUTTON);
        public static final RegistrySupplier<Item> VEIL_WILLOW_PRESSURE_PLATE_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_PRESSURE_PLATE);
        public static final RegistrySupplier<Item> VEIL_WILLOW_TRAPDOOR_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_TRAPDOOR);
        public static final RegistrySupplier<Item> VEIL_WILLOW_DOOR_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_DOOR);
        public static final RegistrySupplier<Item> VEIL_WILLOW_SAPLING_ITEM = registerBlockItem(ModBlocks.VEIL_WILLOW_SAPLING);

        // Stripped Logs
        public static final RegistrySupplier<Item> STRIPPED_DRYAD_LOG_ITEM = registerBlockItem(ModBlocks.STRIPPED_DRYAD_LOG);
        public static final RegistrySupplier<Item> STRIPPED_DRYAD_WOOD_ITEM = registerBlockItem(ModBlocks.STRIPPED_DRYAD_WOOD);
        public static final RegistrySupplier<Item> STRIPPED_VEIL_WILLOW_LOG_ITEM = registerBlockItem(ModBlocks.STRIPPED_VEIL_WILLOW_LOG);
        public static final RegistrySupplier<Item> STRIPPED_VEIL_WILLOW_WOOD_ITEM = registerBlockItem(ModBlocks.STRIPPED_VEIL_WILLOW_WOOD);

        // Signs
        public static final RegistrySupplier<Item> DRYAD_SIGN_ITEM = ITEMS.register("dryad_sign",
                        () -> new net.minecraft.world.item.SignItem(new Item.Properties().stacksTo(16),
                                        ModBlocks.DRYAD_SIGN.get(), ModBlocks.DRYAD_WALL_SIGN.get()));
        public static final RegistrySupplier<Item> VEIL_WILLOW_SIGN_ITEM = ITEMS.register("veil_willow_sign",
                        () -> new net.minecraft.world.item.SignItem(new Item.Properties().stacksTo(16),
                                        ModBlocks.VEIL_WILLOW_SIGN.get(), ModBlocks.VEIL_WILLOW_WALL_SIGN.get()));

        // Hanging Signs
        public static final RegistrySupplier<Item> DRYAD_HANGING_SIGN_ITEM = ITEMS.register("dryad_hanging_sign",
                        () -> new net.minecraft.world.item.HangingSignItem(
                                        ModBlocks.DRYAD_HANGING_SIGN.get(), ModBlocks.DRYAD_WALL_HANGING_SIGN.get(),
                                        new Item.Properties().stacksTo(16)));
        public static final RegistrySupplier<Item> VEIL_WILLOW_HANGING_SIGN_ITEM = ITEMS.register("veil_willow_hanging_sign",
                        () -> new net.minecraft.world.item.HangingSignItem(
                                        ModBlocks.VEIL_WILLOW_HANGING_SIGN.get(), ModBlocks.VEIL_WILLOW_WALL_HANGING_SIGN.get(),
                                        new Item.Properties().stacksTo(16)));

        // Mermaid Armor - Blue
        public static final RegistrySupplier<Item> BLUE_MERMAID_HELMET = ITEMS.register("blue_mermaid_helmet",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE,
                                        net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
        public static final RegistrySupplier<Item> BLUE_MERMAID_CHESTPLATE = ITEMS.register("blue_mermaid_chestplate",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE,
                                        net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        public static final RegistrySupplier<Item> BLUE_MERMAID_LEGGINGS = ITEMS.register("blue_mermaid_leggings",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE,
                                        net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
        public static final RegistrySupplier<Item> BLUE_MERMAID_BOOTS = ITEMS.register("blue_mermaid_boots",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE,
                                        net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

        // Mermaid Armor - Green
        public static final RegistrySupplier<Item> GREEN_MERMAID_HELMET = ITEMS.register("green_mermaid_helmet",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN,
                                        net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
        public static final RegistrySupplier<Item> GREEN_MERMAID_CHESTPLATE = ITEMS.register("green_mermaid_chestplate",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN,
                                        net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        public static final RegistrySupplier<Item> GREEN_MERMAID_LEGGINGS = ITEMS.register("green_mermaid_leggings",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN,
                                        net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
        public static final RegistrySupplier<Item> GREEN_MERMAID_BOOTS = ITEMS.register("green_mermaid_boots",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN,
                                        net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

        // Mermaid Armor - Yellow
        public static final RegistrySupplier<Item> YELLOW_MERMAID_HELMET = ITEMS.register("yellow_mermaid_helmet",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW,
                                        net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
        public static final RegistrySupplier<Item> YELLOW_MERMAID_CHESTPLATE = ITEMS
                        .register("yellow_mermaid_chestplate",
                                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW,
                                                        net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                                                        new Item.Properties()));
        public static final RegistrySupplier<Item> YELLOW_MERMAID_LEGGINGS = ITEMS.register("yellow_mermaid_leggings",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW,
                                        net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
        public static final RegistrySupplier<Item> YELLOW_MERMAID_BOOTS = ITEMS.register("yellow_mermaid_boots",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW,
                                        net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

        // Music Discs
        public static final RegistrySupplier<Item> LOFI_WARCRIMES_DISC = ITEMS.register("lofi_warcrimes_disc",
                        () -> new mc.sayda.creraces.item.LazyRecordItem(15, ModSounds.LOFI_WARCRIMES::get,
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)
                                                        .rarity(net.minecraft.world.item.Rarity.RARE),
                                        4200));
        public static final RegistrySupplier<Item> VERMINWAVE_DISC = ITEMS.register("verminwave_disc",
                        () -> new mc.sayda.creraces.item.LazyRecordItem(15, ModSounds.VERMINWAVE::get,
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)
                                                        .rarity(net.minecraft.world.item.Rarity.RARE),
                                        2800));
        public static final RegistrySupplier<Item> UNDERGROUND_CLUB_DISC = ITEMS.register("underground_club_disc",
                        () -> new mc.sayda.creraces.item.LazyRecordItem(15, ModSounds.UNDERGROUND_CLUB::get,
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)
                                                        .rarity(net.minecraft.world.item.Rarity.RARE),
                                        2000));
        public static final RegistrySupplier<Item> PLEASANT_BOPS_DISC = ITEMS.register("pleasant_bops_disc",
                        () -> new mc.sayda.creraces.item.LazyRecordItem(15, ModSounds.PLEASANT_BOPS::get,
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)
                                                        .rarity(net.minecraft.world.item.Rarity.RARE),
                                        2200));

        // Fairy Source
        public static final RegistrySupplier<Item> FAIRY_DUST = ITEMS.register("fairy_dust",
                        () -> new Item(new Item.Properties()));

        public static final RegistrySupplier<Item> FAIRY_BUCKET = ITEMS.register("fairy_bucket",
                        () -> new net.minecraft.world.item.BucketItem(
                                        ModFluids.FAIRY_SOURCE.get(),
                                        new Item.Properties()
                                                        .craftRemainder(net.minecraft.world.item.Items.BUCKET)
                                                        .stacksTo(1)));

        // Registry name intentionally diverges from Classic's "blessed_water_bucket", see LegacyBlockRemaps.
        public static final RegistrySupplier<Item> ETERVEIL_BUCKET = ITEMS.register("eterveil_bucket",
                        () -> new net.minecraft.world.item.BucketItem(
                                        ModFluids.ETERVEIL.get(),
                                        new Item.Properties()
                                                        .craftRemainder(net.minecraft.world.item.Items.BUCKET)
                                                        .stacksTo(1)));

        public static final RegistrySupplier<Item> FAIRY_BOTTLE = ITEMS.register("fairy_bottle",
                        () -> new mc.sayda.creraces.item.FairyBottleItem(
                                        new Item.Properties().stacksTo(16)));

        // Research Table
        public static final RegistrySupplier<Item> INK_AND_QUILL = ITEMS.register("ink_and_quill",
                        () -> new mc.sayda.creraces.item.InkAndQuillItem(new Item.Properties().durability(10)));
        public static final RegistrySupplier<Item> RESEARCH_TABLE_ITEM = registerBlockItem(ModBlocks.RESEARCH_TABLE);

        // Spirit Compass
        public static final RegistrySupplier<Item> SPIRIT_COMPASS = ITEMS.register("spirit_compass",
                        () -> new mc.sayda.creraces.item.SpiritCompassItem(new Item.Properties().stacksTo(1)));

        // Essence Belt
        public static final RegistrySupplier<Item> ESSENCE_BELT = ITEMS.register("essence_belt",
                        () -> new mc.sayda.creraces.item.EssenceBeltItem(new Item.Properties().stacksTo(1)));

        public static final RegistrySupplier<Item> ESSENCE_CAULDRON_ITEM = registerBlockItem(ModBlocks.ESSENCE_CAULDRON);
        public static final RegistrySupplier<Item> QUEST_BOARD_ITEM = registerBlockItem(ModBlocks.QUEST_BOARD);

        private static RegistrySupplier<Item> registerBlockItem(
                        RegistrySupplier<? extends net.minecraft.world.level.block.Block> blockSupplier) {
                return ITEMS.register(blockSupplier.getId().getPath(),
                                () -> new net.minecraft.world.item.BlockItem(blockSupplier.get(),
                                                new net.minecraft.world.item.Item.Properties()));
        }

        public static void register() {
                mc.sayda.creraces.ability.EssenceRegistry.registerItems();
                ITEMS.register();
        }
}
