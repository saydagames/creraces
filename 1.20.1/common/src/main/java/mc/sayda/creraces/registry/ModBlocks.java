package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

@SuppressWarnings("null")
public class ModBlocks {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(CreRaces.MODID, Registries.BLOCK);

        // Dryad Woods
        public static final RegistrySupplier<Block> DRYAD_LOG = BLOCKS.register("dryad_log",
                        () -> new RotatedPillarBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                                                        .sound(SoundType.WOOD)));
        public static final RegistrySupplier<Block> DRYAD_LOG_CORE = BLOCKS.register("dryad_log_core",
                        () -> new RotatedPillarBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                                                        .sound(SoundType.WOOD)));
        public static final RegistrySupplier<Block> DRYAD_WOOD = BLOCKS.register("dryad_wood",
                        () -> new RotatedPillarBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                                                        .sound(SoundType.WOOD)));
        public static final RegistrySupplier<Block> DRYAD_PETRIFIED_WOOD = BLOCKS.register("dryad_petrified_wood",
                        () -> new RotatedPillarBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                                                        .strength(-1.0f, 3600000.0f).sound(SoundType.WOOD)));
        public static final RegistrySupplier<Block> DRYAD_PLANKS = BLOCKS.register("dryad_planks",
                        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                                        .sound(SoundType.WOOD)));

        // Dryad Leaves
        public static final RegistrySupplier<Block> DRYAD_LEAVES = BLOCKS.register("dryad_leaves",
                        () -> new LeavesBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.2f)
                                                        .randomTicks().sound(SoundType.GRASS).noOcclusion()));
        public static final RegistrySupplier<Block> DRYAD_LEAVES_FLOWERING = BLOCKS.register("dryad_leaves_flowering",
                        () -> new mc.sayda.creraces.block.DryadLeavesFloweringBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.2f)
                                                        .randomTicks().sound(SoundType.GRASS).noOcclusion()));
        public static final RegistrySupplier<Block> DRYAD_LEAVES_FRUIT = BLOCKS.register("dryad_leaves_fruit",
                        () -> new mc.sayda.creraces.block.DryadLeavesFruitBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.2f)
                                                        .randomTicks().sound(SoundType.GRASS).noOcclusion()));

        // Dryad Functional/Decorative
        @SuppressWarnings("unchecked")
        public static final RegistrySupplier<Block> DRYAD_SAPLING = BLOCKS.register("dryad_sapling",
                        () -> new SaplingBlock(
                                        new mc.sayda.creraces.world.tree.DryadTreeGrower(),
                                        BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                                                        .mapColor(MapColor.COLOR_GREEN))); // Grower
                                                                                           // to
                                                                                           // be
                                                                                           // added
        public static final RegistrySupplier<Block> DRYAD_ROOT = BLOCKS.register("dryad_root",
                        () -> new mc.sayda.creraces.block.RootBlock(
                                        mc.sayda.creraces.block.RootBlock.getDefaultProperties()));
        public static final RegistrySupplier<Block> DRYAD_LANTERN = BLOCKS.register("dryad_lantern",
                        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(3.5f)
                                        .sound(SoundType.WOOD).lightLevel(state -> 15)));
        public static final RegistrySupplier<Block> DRYAD_EXPANSION_PANEL = BLOCKS.register("dryad_expansion_panel",
                        () -> new mc.sayda.creraces.block.DryadExpansionPanelBlock());

        // Dryad Furniture/Structural
        public static final RegistrySupplier<Block> DRYAD_STAIRS = BLOCKS.register("dryad_stairs",
                        () -> new StairBlock(DRYAD_PLANKS.get().defaultBlockState(),
                                        BlockBehaviour.Properties.copy(DRYAD_PLANKS.get())));
        public static final RegistrySupplier<Block> DRYAD_SLAB = BLOCKS.register("dryad_slab",
                        () -> new SlabBlock(BlockBehaviour.Properties.copy(DRYAD_PLANKS.get())));
        public static final RegistrySupplier<Block> DRYAD_FENCE = BLOCKS.register("dryad_fence",
                        () -> new FenceBlock(BlockBehaviour.Properties.copy(DRYAD_PLANKS.get())));
        public static final RegistrySupplier<Block> DRYAD_FENCE_GATE = BLOCKS.register("dryad_fence_gate",
                        () -> new FenceGateBlock(BlockBehaviour.Properties.copy(DRYAD_PLANKS.get()),
                                        net.minecraft.world.level.block.state.properties.WoodType.OAK));
        public static final RegistrySupplier<Block> DRYAD_BUTTON = BLOCKS.register("dryad_button",
                        () -> new ButtonBlock(
                                        BlockBehaviour.Properties.of().noCollission().strength(0.5f)
                                                        .pushReaction(PushReaction.DESTROY),
                                        net.minecraft.world.level.block.state.properties.BlockSetType.OAK, 30, true));
        public static final RegistrySupplier<Block> DRYAD_PRESSURE_PLATE = BLOCKS.register("dryad_pressure_plate",
                        () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                                        BlockBehaviour.Properties.copy(DRYAD_PLANKS.get()),
                                        net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
        public static final RegistrySupplier<Block> DRYAD_TRAPDOOR = BLOCKS.register("dryad_trapdoor",
                        () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(DRYAD_PLANKS.get()),
                                        net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
        public static final RegistrySupplier<Block> DRYAD_DOOR = BLOCKS.register("dryad_door",
                        () -> new DoorBlock(BlockBehaviour.Properties.copy(DRYAD_PLANKS.get()),
                                        net.minecraft.world.level.block.state.properties.BlockSetType.OAK));

        // Fairy Orbs
        public static final RegistrySupplier<Block> SPRING_FAIRY_ORB = BLOCKS.register("spring_fairy_orb",
                        () -> new Block(
                                        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                                                        .lightLevel(state -> 10).noOcclusion()));
        public static final RegistrySupplier<Block> SUMMER_FAIRY_ORB = BLOCKS.register("summer_fairy_orb",
                        () -> new Block(
                                        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                                                        .lightLevel(state -> 10).noOcclusion()));
        public static final RegistrySupplier<Block> AUTUMN_FAIRY_ORB = BLOCKS.register("autumn_fairy_orb",
                        () -> new Block(
                                        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                                                        .lightLevel(state -> 10).noOcclusion()));
        public static final RegistrySupplier<Block> WINTER_FAIRY_ORB = BLOCKS.register("winter_fairy_orb",
                        () -> new Block(
                                        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                                                        .lightLevel(state -> 10).noOcclusion()));
        public static final RegistrySupplier<Block> DAY_FAIRY_ORB = BLOCKS.register("day_fairy_orb",
                        () -> new Block(
                                        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                                                        .lightLevel(state -> 10).noOcclusion()));
        public static final RegistrySupplier<Block> NIGHT_FAIRY_ORB = BLOCKS.register("night_fairy_orb",
                        () -> new Block(
                                        BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                                                        .lightLevel(state -> 10).noOcclusion()));

        public static final RegistrySupplier<Block> TORI_BELL = BLOCKS.register("tori_bell",
                        () -> new mc.sayda.creraces.block.ToriBellBlock(
                                        BlockBehaviour.Properties.copy(Blocks.BELL).mapColor(MapColor.COLOR_RED),
                                        false));

        public static final RegistrySupplier<Block> WEATHERED_TORI_BELL = BLOCKS.register("weathered_tori_bell",
                        () -> new mc.sayda.creraces.block.ToriBellBlock(
                                        BlockBehaviour.Properties.copy(Blocks.BELL).mapColor(MapColor.COLOR_RED),
                                        true));

        public static final RegistrySupplier<Block> RED_STRIPPED_OAK_LOG = BLOCKS.register("red_stripped_oak_log",
                        () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_LOG)
                                        .mapColor(MapColor.COLOR_RED)));

        public static final RegistrySupplier<Block> WEATHERED_RED_STRIPPED_OAK_LOG = BLOCKS.register(
                        "weathered_red_stripped_oak_log",
                        () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_LOG)
                                        .mapColor(MapColor.COLOR_RED)));

        // ─── Oread ─────────────────────────────────────────────────────────────────
        public static final RegistrySupplier<Block> VOLCANIC_ROCK = BLOCKS.register("volcanic_rock",
                        mc.sayda.creraces.block.VolcanicRockBlock::new);
        public static final RegistrySupplier<Block> VOLCANIC_ROCK_HARDENED = BLOCKS.register(
                        "volcanic_rock_hardened", mc.sayda.creraces.block.VolcanicRockHardenedBlock::new);

        // ─── Ratkin ────────────────────────────────────────────────────────────────
        /**
         * Placed by Ratkin's Rat Tunnels ability. Indestructible flat marker, no
         * collision.
         */
        public static final RegistrySupplier<Block> RAT_HOLE = BLOCKS.register("rat_hole",
                        () -> new mc.sayda.creraces.block.RatHoleBlock());

        // ─── Mini Build System ─────────────────────────────────────────────────────
        // Always registered so MICRO_BLOCK is never null.
        // The runtime Mixin behavior is gated by CreRacesConfig.MINI_BUILD_ENABLED at
        // the call site.
        public static RegistrySupplier<Block> MICRO_BLOCK;

        public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
                        .create(CreRaces.MODID, net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE);

        public static dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<mc.sayda.creraces.block.entity.MicroBlockEntity>> MICRO_BLOCK_ENTITY;
        public static dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<mc.sayda.creraces.block.entity.RatHoleBlockEntity>> RAT_HOLE_ENTITY;
        public static dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.BellBlockEntity>> TORI_BELL_ENTITY;

        public static void register() {
                BLOCKS.register();

                MICRO_BLOCK = BLOCKS.register("mini_block",
                                () -> new mc.sayda.creraces.block.MicroBlock(
                                                mc.sayda.creraces.block.MicroBlock.PROPERTIES));

                MICRO_BLOCK_ENTITY = BLOCK_ENTITIES.register("mini_block",
                                () -> BlockEntityType.Builder
                                                .of(mc.sayda.creraces.block.entity.MicroBlockEntity::new,
                                                                MICRO_BLOCK.get())
                                                .build(null));

                RAT_HOLE_ENTITY = BLOCK_ENTITIES.register("rat_hole",
                                () -> BlockEntityType.Builder
                                                .of(mc.sayda.creraces.block.entity.RatHoleBlockEntity::new,
                                                                RAT_HOLE.get())
                                                .build(null));

                TORI_BELL_ENTITY = BLOCK_ENTITIES.register("tori_bell",
                                () -> BlockEntityType.Builder
                                                .of(net.minecraft.world.level.block.entity.BellBlockEntity::new,
                                                                TORI_BELL.get(), WEATHERED_TORI_BELL.get())
                                                .build(null));

                BLOCK_ENTITIES.register();
        }
}
