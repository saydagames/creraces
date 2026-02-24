package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

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
                                                        .strength(2.0f).sound(SoundType.WOOD)));
        public static final RegistrySupplier<Block> DRYAD_PLANKS = BLOCKS.register("dryad_planks",
                        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                                        .sound(SoundType.WOOD)));

        // Dryad Leaves
        public static final RegistrySupplier<Block> DRYAD_LEAVES = BLOCKS.register("dryad_leaves",
                        () -> new LeavesBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.2f)
                                                        .randomTicks().sound(SoundType.GRASS).noOcclusion()));
        public static final RegistrySupplier<Block> DRYAD_LEAVES_FLOWERING = BLOCKS.register("dryad_leaves_flowering",
                        () -> new LeavesBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.2f)
                                                        .randomTicks().sound(SoundType.GRASS).noOcclusion()));
        public static final RegistrySupplier<Block> DRYAD_LEAVES_FRUIT = BLOCKS.register("dryad_leaves_fruit",
                        () -> new LeavesBlock(
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
                        () -> new mc.sayda.creraces.block.DryadRootBlock(BlockBehaviour.Properties.copy(Blocks.DIRT)
                                        .strength(-1.0f, 3600000.0f)));
        public static final RegistrySupplier<Block> DRYAD_LANTERN = BLOCKS.register("dryad_lantern",
                        () -> new LanternBlock(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.LANTERN)
                                        .lightLevel(state -> 15).noOcclusion()));
        public static final RegistrySupplier<Block> DRYAD_TOTEM = BLOCKS.register("dryad_totem",
                        () -> new Block(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD)));

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

        public static void register() {
                BLOCKS.register();
        }
}
