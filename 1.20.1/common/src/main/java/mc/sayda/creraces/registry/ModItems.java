package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
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
        public static final RegistrySupplier<Item> DRYAD_TOTEM_ITEM = registerBlockItem(ModBlocks.DRYAD_TOTEM);
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

        // Music Discs
        public static final RegistrySupplier<Item> LOFI_WARCRIMES_DISC = ITEMS.register("lofi_warcrimes_disc",
                        () -> new net.minecraft.world.item.RecordItem(15, ModSounds.LOFI_WARCRIMES.get(),
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)
                                                        .rarity(net.minecraft.world.item.Rarity.RARE),
                                        4200));
        public static final RegistrySupplier<Item> VERMINWAVE_DISC = ITEMS.register("verminwave_disc",
                        () -> new net.minecraft.world.item.RecordItem(15, ModSounds.VERMINWAVE.get(),
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)
                                                        .rarity(net.minecraft.world.item.Rarity.RARE),
                                        2800));
        public static final RegistrySupplier<Item> UNDERGROUND_CLUB_DISC = ITEMS.register("underground_club_disc",
                        () -> new net.minecraft.world.item.RecordItem(15, ModSounds.UNDERGROUND_CLUB.get(),
                                        new net.minecraft.world.item.Item.Properties().stacksTo(1)
                                                        .rarity(net.minecraft.world.item.Rarity.RARE),
                                        2000));
        public static final RegistrySupplier<Item> PLEASANT_BOPS_DISC = ITEMS.register("pleasant_bops_disc",
                        () -> new net.minecraft.world.item.RecordItem(15, ModSounds.PLEASANT_BOPS.get(),
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
