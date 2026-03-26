package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.item.CommandingStaffItem;
import mc.sayda.creraces.item.MermaidArmorMaterial;
import mc.sayda.creraces.item.ScrollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ModItems {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CreRaces.MODID, Registries.ITEM);

        public static final RegistrySupplier<Item> ABILITY_SCROLL = ITEMS.register("ability_scroll",
                        () -> new ScrollItem(new Item.Properties().stacksTo(1)));

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
        public static final RegistrySupplier<Item> TORI_BELL_ITEM = registerBlockItem(ModBlocks.TORI_BELL);
        public static final RegistrySupplier<Item> WEATHERED_TORI_BELL_ITEM = registerBlockItem(
                        ModBlocks.WEATHERED_TORI_BELL);
        public static final RegistrySupplier<Item> RED_STRIPPED_OAK_LOG_ITEM = registerBlockItem(
                        ModBlocks.RED_STRIPPED_OAK_LOG);
        public static final RegistrySupplier<Item> WEATHERED_RED_STRIPPED_OAK_LOG_ITEM = registerBlockItem(
                        ModBlocks.WEATHERED_RED_STRIPPED_OAK_LOG);
        public static final RegistrySupplier<Item> VOLCANIC_ROCK_ITEM = registerBlockItem(
                        ModBlocks.VOLCANIC_ROCK);
        public static final RegistrySupplier<Item> VOLCANIC_ROCK_HARDENED_ITEM = registerBlockItem(
                        ModBlocks.VOLCANIC_ROCK_HARDENED);

        // Mermaid Armor - Blue
        public static final RegistrySupplier<Item> BLUE_MERMAID_HELMET = ITEMS.register("blue_mermaid_helmet",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
        public static final RegistrySupplier<Item> BLUE_MERMAID_CHESTPLATE = ITEMS.register("blue_mermaid_chestplate",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        public static final RegistrySupplier<Item> BLUE_MERMAID_LEGGINGS = ITEMS.register("blue_mermaid_leggings",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
        public static final RegistrySupplier<Item> BLUE_MERMAID_BOOTS = ITEMS.register("blue_mermaid_boots",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.BLUE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

        // Mermaid Armor - Green
        public static final RegistrySupplier<Item> GREEN_MERMAID_HELMET = ITEMS.register("green_mermaid_helmet",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
        public static final RegistrySupplier<Item> GREEN_MERMAID_CHESTPLATE = ITEMS.register("green_mermaid_chestplate",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        public static final RegistrySupplier<Item> GREEN_MERMAID_LEGGINGS = ITEMS.register("green_mermaid_leggings",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
        public static final RegistrySupplier<Item> GREEN_MERMAID_BOOTS = ITEMS.register("green_mermaid_boots",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.GREEN, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

        // Mermaid Armor - Yellow
        public static final RegistrySupplier<Item> YELLOW_MERMAID_HELMET = ITEMS.register("yellow_mermaid_helmet",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
        public static final RegistrySupplier<Item> YELLOW_MERMAID_CHESTPLATE = ITEMS.register("yellow_mermaid_chestplate",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        public static final RegistrySupplier<Item> YELLOW_MERMAID_LEGGINGS = ITEMS.register("yellow_mermaid_leggings",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
        public static final RegistrySupplier<Item> YELLOW_MERMAID_BOOTS = ITEMS.register("yellow_mermaid_boots",
                        () -> new net.minecraft.world.item.ArmorItem(MermaidArmorMaterial.YELLOW, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

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

        private static RegistrySupplier<Item> registerBlockItem(
                        RegistrySupplier<? extends net.minecraft.world.level.block.Block> blockSupplier) {
                return ITEMS.register(blockSupplier.getId().getPath(),
                                () -> new net.minecraft.world.item.BlockItem(blockSupplier.get(),
                                                new net.minecraft.world.item.Item.Properties()));
        }

        public static void register() {
                ITEMS.register();
        }
}
