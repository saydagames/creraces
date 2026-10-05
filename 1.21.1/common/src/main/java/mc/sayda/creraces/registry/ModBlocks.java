package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.block.DryadExpansionPanelBlock;
import mc.sayda.creraces.block.DryadLeavesFloweringBlock;
import mc.sayda.creraces.block.DryadLeavesFruitBlock;
import mc.sayda.creraces.block.DryadTotemBlock;
import mc.sayda.creraces.block.ElysianVeilBloomBlock;
import mc.sayda.creraces.block.EssenceCauldronBlock;
import mc.sayda.creraces.block.EterveilBlock;
import mc.sayda.creraces.block.FairySourceBlock;
import mc.sayda.creraces.block.MicroBlock;
import mc.sayda.creraces.block.NaiadStatueBlock;
import mc.sayda.creraces.block.NymphNodeBlock;
import mc.sayda.creraces.block.QuestBoardBlock;
import mc.sayda.creraces.block.RatHoleBlock;
import mc.sayda.creraces.block.ResearchTableBlock;
import mc.sayda.creraces.block.RootBlock;
import mc.sayda.creraces.block.SummonedDirtBlock;
import mc.sayda.creraces.block.ToriiBellBlock;
import mc.sayda.creraces.block.TreeGatewayBlock;
import mc.sayda.creraces.block.VeilMushroomBlock;
import mc.sayda.creraces.block.VeilWillowDrapeBlock;
import mc.sayda.creraces.block.VeilWillowSaplingBlock;
import mc.sayda.creraces.block.VolcanicRockBlock;
import mc.sayda.creraces.block.VolcanicRockHardenedBlock;
import mc.sayda.creraces.block.entity.ElysianVeilBloomBlockEntity;
import mc.sayda.creraces.block.entity.EssenceCauldronBlockEntity;
import mc.sayda.creraces.block.entity.EssenceVortexBlockEntity;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.block.entity.QuestBoardBlockEntity;
import mc.sayda.creraces.block.entity.RatHoleBlockEntity;
import mc.sayda.creraces.block.entity.ResearchTableBlockEntity;
import mc.sayda.creraces.block.entity.ToriiBellBlockEntity;
import mc.sayda.creraces.block.entity.VeilMushroomBlockEntity;
import mc.sayda.creraces.block.entity.VeilWillowSaplingBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Optional;

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
            () -> new DryadLeavesFloweringBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.2f)
                            .randomTicks().sound(SoundType.GRASS).noOcclusion()));
    public static final RegistrySupplier<Block> DRYAD_LEAVES_FRUIT = BLOCKS.register("dryad_leaves_fruit",
            () -> new DryadLeavesFruitBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.2f)
                            .randomTicks().sound(SoundType.GRASS).noOcclusion()));

    public static final RegistrySupplier<Block> STRIPPED_DRYAD_LOG = BLOCKS.register("stripped_dryad_log",
            () -> new RotatedPillarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                            .sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> STRIPPED_DRYAD_WOOD = BLOCKS.register("stripped_dryad_wood",
            () -> new RotatedPillarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                            .sound(SoundType.WOOD)));

    // Dryad Signs
    public static final RegistrySupplier<Block> DRYAD_SIGN = BLOCKS.register("dryad_sign",
            () -> new StandingSignBlock(ModWoodTypes.DRYAD, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollission()
                            .strength(1.0F).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> DRYAD_WALL_SIGN = BLOCKS.register("dryad_wall_sign",
            () -> new WallSignBlock(ModWoodTypes.DRYAD, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollission()
                            .strength(1.0F).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> DRYAD_HANGING_SIGN = BLOCKS.register("dryad_hanging_sign",
            () -> new CeilingHangingSignBlock(ModWoodTypes.DRYAD, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollission()
                            .strength(1.0F).sound(SoundType.HANGING_SIGN)));
    public static final RegistrySupplier<Block> DRYAD_WALL_HANGING_SIGN = BLOCKS.register("dryad_wall_hanging_sign",
            () -> new WallHangingSignBlock(ModWoodTypes.DRYAD, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollission()
                            .strength(1.0F).sound(SoundType.HANGING_SIGN)));

    // Dryad Functional/Decorative
    @SuppressWarnings("unchecked")
    public static final RegistrySupplier<Block> DRYAD_SAPLING = BLOCKS.register("dryad_sapling",
            () -> new SaplingBlock(
                    new TreeGrower("creraces_dryad",
                            Optional.empty(),
                            Optional.of(ResourceKey.create(
                                    Registries.CONFIGURED_FEATURE,
                                    ResourceLocation.fromNamespaceAndPath("creraces", "dryad_tree"))),
                            Optional.empty()),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)
                            .mapColor(MapColor.COLOR_GREEN)));
    public static final RegistrySupplier<Block> DRYAD_ROOT = BLOCKS.register("dryad_root",
            () -> new RootBlock(
                    RootBlock.getDefaultProperties()));
    public static final RegistrySupplier<Block> DRYAD_LANTERN = BLOCKS.register("dryad_lantern",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(3.5f)
                    .sound(SoundType.WOOD).lightLevel(state -> 15)));
    public static final RegistrySupplier<Block> DRYAD_EXPANSION_PANEL = BLOCKS.register("dryad_expansion_panel",
            () -> new DryadExpansionPanelBlock());

    // Dryad Furniture/Structural
    public static final RegistrySupplier<Block> DRYAD_STAIRS = BLOCKS.register("dryad_stairs",
            () -> new StairBlock(DRYAD_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(DRYAD_PLANKS.get())));
    public static final RegistrySupplier<Block> DRYAD_SLAB = BLOCKS.register("dryad_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(DRYAD_PLANKS.get())));
    public static final RegistrySupplier<Block> DRYAD_FENCE = BLOCKS.register("dryad_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(DRYAD_PLANKS.get())));
    public static final RegistrySupplier<Block> DRYAD_FENCE_GATE = BLOCKS.register("dryad_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(DRYAD_PLANKS.get())));
    public static final RegistrySupplier<Block> DRYAD_BUTTON = BLOCKS.register("dryad_button",
            () -> new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.of().noCollission().strength(0.5f)
                            .pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<Block> DRYAD_PRESSURE_PLATE = BLOCKS.register("dryad_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(DRYAD_PLANKS.get())));
    public static final RegistrySupplier<Block> DRYAD_TRAPDOOR = BLOCKS.register("dryad_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(DRYAD_PLANKS.get()).noOcclusion()));
    public static final RegistrySupplier<Block> DRYAD_DOOR = BLOCKS.register("dryad_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(DRYAD_PLANKS.get())));

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

    public static final RegistrySupplier<Block> TORII_BELL = BLOCKS.register("torii_bell",
            () -> new ToriiBellBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BELL).mapColor(MapColor.COLOR_RED),
                    false));

    public static final RegistrySupplier<Block> WEATHERED_TORII_BELL = BLOCKS.register("weathered_torii_bell",
            () -> new ToriiBellBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BELL).mapColor(MapColor.COLOR_RED),
                    true));

    public static final RegistrySupplier<Block> RED_STRIPPED_OAK_LOG = BLOCKS.register("red_stripped_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)
                    .mapColor(MapColor.COLOR_RED)));

    public static final RegistrySupplier<Block> WEATHERED_RED_STRIPPED_OAK_LOG = BLOCKS.register(
            "weathered_red_stripped_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)
                    .mapColor(MapColor.COLOR_RED)));

    // Nymph Nodes
    // Registry names match CreRaces Classic's original block IDs for legacy-world compatibility.
    public static final RegistrySupplier<Block> AURAI_SCULPTURE = BLOCKS.register("angelic_sculpture",
            () -> new NymphNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ).strength(-1.0f, 3600000.0f)
                    .sound(SoundType.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> NAIAD_STATUE = BLOCKS.register("oceanic_statue",
            () -> new NaiadStatueBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).strength(-1.0f, 3600000.0f)
                    .sound(SoundType.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> OREAD_IDOL = BLOCKS.register("volcanic_idol",
            () -> new NymphNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(-1.0f, 3600000.0f)
                    .sound(SoundType.STONE).noOcclusion()));

    // Oread
    public static final RegistrySupplier<Block> VOLCANIC_ROCK = BLOCKS.register("volcanic_rock",
            VolcanicRockBlock::new);
    public static final RegistrySupplier<Block> VOLCANIC_ROCK_HARDENED = BLOCKS.register(
            "volcanic_rock_hardened", VolcanicRockHardenedBlock::new);

    // Spirit
    public static final RegistrySupplier<Block> VEIL_WILLOW_LOG = BLOCKS.register("veil_willow_log",
            () -> new RotatedPillarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f)
                            .sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_LEAVES = BLOCKS.register("veil_willow_leaves",
            () -> new LeavesBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.2f)
                            .randomTicks().sound(SoundType.GRASS).noOcclusion()
                            .lightLevel(state -> 3)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_DRAPE = BLOCKS.register("veil_willow_drape",
            () -> new VeilWillowDrapeBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.1f)
                            .sound(SoundType.GRASS).noCollission().noOcclusion()
                            .lightLevel(state -> 4)));

    public static final RegistrySupplier<Block> VEIL_WILLOW_WOOD = BLOCKS.register("veil_willow_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> STRIPPED_VEIL_WILLOW_LOG = BLOCKS.register("stripped_veil_willow_log",
            () -> new RotatedPillarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f)
                            .sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> STRIPPED_VEIL_WILLOW_WOOD = BLOCKS.register("stripped_veil_willow_wood",
            () -> new RotatedPillarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f)
                            .sound(SoundType.WOOD)));

    // Veil Willow Signs
    public static final RegistrySupplier<Block> VEIL_WILLOW_SIGN = BLOCKS.register("veil_willow_sign",
            () -> new StandingSignBlock(ModWoodTypes.VEIL_WILLOW, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollission()
                            .strength(1.0F).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_WALL_SIGN = BLOCKS.register("veil_willow_wall_sign",
            () -> new WallSignBlock(ModWoodTypes.VEIL_WILLOW, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollission()
                            .strength(1.0F).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_HANGING_SIGN = BLOCKS.register("veil_willow_hanging_sign",
            () -> new CeilingHangingSignBlock(ModWoodTypes.VEIL_WILLOW, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollission()
                            .strength(1.0F).sound(SoundType.HANGING_SIGN)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_WALL_HANGING_SIGN = BLOCKS.register("veil_willow_wall_hanging_sign",
            () -> new WallHangingSignBlock(ModWoodTypes.VEIL_WILLOW, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollission()
                            .strength(1.0F).sound(SoundType.HANGING_SIGN)));

    public static final RegistrySupplier<Block> VEIL_WILLOW_PLANKS = BLOCKS.register("veil_willow_planks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 3.0f).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_STAIRS = BLOCKS.register("veil_willow_stairs",
            () -> new StairBlock(VEIL_WILLOW_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(VEIL_WILLOW_PLANKS.get())));
    public static final RegistrySupplier<Block> VEIL_WILLOW_SLAB = BLOCKS.register("veil_willow_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(VEIL_WILLOW_PLANKS.get())));
    public static final RegistrySupplier<Block> VEIL_WILLOW_FENCE = BLOCKS.register("veil_willow_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(VEIL_WILLOW_PLANKS.get())));
    public static final RegistrySupplier<Block> VEIL_WILLOW_FENCE_GATE = BLOCKS.register("veil_willow_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(VEIL_WILLOW_PLANKS.get())));
    public static final RegistrySupplier<Block> VEIL_WILLOW_BUTTON = BLOCKS.register("veil_willow_button",
            () -> new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.of().noCollission().strength(0.5f)
                    .sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_PRESSURE_PLATE = BLOCKS.register(
            "veil_willow_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                            .noCollission().strength(0.5f).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> VEIL_WILLOW_TRAPDOOR = BLOCKS.register("veil_willow_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(VEIL_WILLOW_PLANKS.get()).noOcclusion()));
    public static final RegistrySupplier<Block> VEIL_WILLOW_DOOR = BLOCKS.register("veil_willow_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(VEIL_WILLOW_PLANKS.get()).noOcclusion()));
    public static final RegistrySupplier<Block> VEIL_WILLOW_SAPLING = BLOCKS.register("veil_willow_sapling",
            () -> new VeilWillowSaplingBlock(
                    new TreeGrower("creraces_veil_willow",
                            Optional.empty(),
                            Optional.of(ResourceKey.create(
                                    Registries.CONFIGURED_FEATURE,
                                    ResourceLocation.fromNamespaceAndPath("creraces", "veil_willow_tree"))),
                            Optional.empty()),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)
                            .mapColor(MapColor.COLOR_CYAN)));

    public static final RegistrySupplier<Block> VEIL_MUSHROOM = BLOCKS.register("veil_mushroom",
            () -> new VeilMushroomBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak()
                            .sound(SoundType.GRASS).lightLevel(state -> 0)
                            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistrySupplier<Block> VEIL_GRIT = BLOCKS.register("veil_grit",
            () -> new ColoredFallingBlock(new ColorRGBA(14406560), BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.SNARE)
                    .strength(0.5f).sound(SoundType.SAND)));

    public static final RegistrySupplier<Block> VEIL_BLOOM = BLOCKS.register("veil_bloom",
            () -> new ElysianVeilBloomBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).randomTicks().noLootTable()
                            .lightLevel(state -> state.getValue(
                                    ElysianVeilBloomBlock.BLOOMING) ? 8 : 0)));

    // Ratkin
    /**
     * Placed by Ratkin's Rat Tunnels ability. Indestructible flat marker, no
     * collision.
     */
    public static final RegistrySupplier<Block> RAT_HOLE = BLOCKS.register("rat_hole",
            () -> new RatHoleBlock());

    // Fairy Tree Gateways
    public static final RegistrySupplier<Block> CHERRY_TREE_GATEWAY = BLOCKS.register("cherry_tree_gateway",
            () -> new TreeGatewayBlock(
                    BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                            .sound(SoundType.CHERRY_WOOD).lightLevel(state -> 7)));
    public static final RegistrySupplier<Block> OAK_TREE_GATEWAY = BLOCKS.register("oak_tree_gateway",
            () -> new TreeGatewayBlock(
                    BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                            .sound(SoundType.WOOD).lightLevel(state -> 7)));
    public static final RegistrySupplier<Block> SPRUCE_TREE_GATEWAY = BLOCKS.register("spruce_tree_gateway",
            () -> new TreeGatewayBlock(
                    BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f)
                            .sound(SoundType.WOOD).lightLevel(state -> 7)));

    // Fairy Source
    public static final RegistrySupplier<Block> FAIRY_SOURCE_BLOCK = BLOCKS.register("fairy_source",
            () -> new FairySourceBlock(
                    ModFluids.FAIRY_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission().strength(100.0F).noLootTable()
                            .liquid().replaceable()));

    // Eterveil, ported from CreRaces Classic's "Holy Water"; registry name intentionally
    // diverges from Classic's "blessed_water", see LegacyBlockRemaps for the migration remap.
    public static final RegistrySupplier<Block> ETERVEIL_BLOCK = BLOCKS.register("eterveil",
            () -> new EterveilBlock(
                    ModFluids.ETERVEIL.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WATER).noCollission().strength(100.0F)
                            .noLootTable().liquid().replaceable()
                            .lightLevel(state -> 1)));

    // Summoned Dirt, ported from CreRaces Classic
    public static final RegistrySupplier<Block> SUMMONED_DIRT = BLOCKS.register("summoned_dirt",
            SummonedDirtBlock::new);

    // Forest Totem, ported from CreRaces Classic; registry name intentionally diverges from
    // Classic's "dryad_totem", see LegacyBlockRemaps for the migration remap.
    public static final RegistrySupplier<Block> DRYAD_TOTEM = BLOCKS.register("forest_totem",
            () -> new DryadTotemBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                            .strength(5.0f, 6.0f).sound(SoundType.WOOD)
                            .noOcclusion()));

    // Research Table
    public static final RegistrySupplier<Block> RESEARCH_TABLE = BLOCKS.register("research_table",
            () -> new ResearchTableBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f)
                            .sound(SoundType.WOOD).noOcclusion()));

    // Essence Cauldron
    public static final RegistrySupplier<Block> ESSENCE_CAULDRON = BLOCKS.register("essence_cauldron",
            () -> new EssenceCauldronBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)));

    // Quest Board - placeholder texture (oak_planks) until real art is provided
    public static final RegistrySupplier<Block> QUEST_BOARD = BLOCKS.register("quest_board",
            () -> new QuestBoardBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f)
                            .sound(SoundType.WOOD).noOcclusion()));

    // Host block for mini builds. Always registered so MICRO_BLOCK is never null; the mini build
    // mixins check CreRacesConfig.MINI_BUILD_ENABLED themselves.
    public static RegistrySupplier<Block> MICRO_BLOCK;

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(CreRaces.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static RegistrySupplier<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_ENTITY;
    public static RegistrySupplier<BlockEntityType<MicroBlockEntity>> MICRO_BLOCK_ENTITY;
    public static RegistrySupplier<BlockEntityType<RatHoleBlockEntity>> RAT_HOLE_ENTITY;
    public static RegistrySupplier<BlockEntityType<BellBlockEntity>> TORII_BELL_ENTITY;
    public static RegistrySupplier<BlockEntityType<VeilMushroomBlockEntity>> VEIL_MUSHROOM_BE;
    public static RegistrySupplier<BlockEntityType<VeilWillowSaplingBlockEntity>> VEIL_WILLOW_SAPLING_BE;
    public static RegistrySupplier<BlockEntityType<ElysianVeilBloomBlockEntity>> VEIL_BLOOM_BE;
    public static RegistrySupplier<BlockEntityType<EssenceCauldronBlockEntity>> ESSENCE_CAULDRON_ENTITY;
    public static RegistrySupplier<BlockEntityType<EssenceVortexBlockEntity>> ESSENCE_VORTEX_ENTITY;
    public static RegistrySupplier<BlockEntityType<QuestBoardBlockEntity>> QUEST_BOARD_ENTITY;

    public static void register() {
        EssenceRegistry.registerBlocks();
        BLOCKS.register();

        RESEARCH_TABLE_ENTITY = BLOCK_ENTITIES.register("research_table",
                () -> BlockEntityType.Builder
                        .of(ResearchTableBlockEntity::new,
                                RESEARCH_TABLE.get())
                        .build(null));

        MICRO_BLOCK = BLOCKS.register("mini_block",
                () -> new MicroBlock(
                        MicroBlock.PROPERTIES));

        MICRO_BLOCK_ENTITY = BLOCK_ENTITIES.register("mini_block",
                () -> BlockEntityType.Builder
                        .of(MicroBlockEntity::new,
                                MICRO_BLOCK.get())
                        .build(null));

        RAT_HOLE_ENTITY = BLOCK_ENTITIES.register("rat_hole",
                () -> BlockEntityType.Builder
                        .of(RatHoleBlockEntity::new,
                                RAT_HOLE.get())
                        .build(null));

        VEIL_MUSHROOM_BE = BLOCK_ENTITIES.register("veil_mushroom",
                () -> BlockEntityType.Builder
                        .of(VeilMushroomBlockEntity::new,
                                VEIL_MUSHROOM.get())
                        .build(null));

        VEIL_WILLOW_SAPLING_BE = BLOCK_ENTITIES.register("veil_willow_sapling",
                () -> BlockEntityType.Builder
                        .of(VeilWillowSaplingBlockEntity::new,
                                VEIL_WILLOW_SAPLING.get())
                        .build(null));

        VEIL_BLOOM_BE = BLOCK_ENTITIES.register("veil_bloom",
                () -> BlockEntityType.Builder
                        .of(ElysianVeilBloomBlockEntity::new,
                                VEIL_BLOOM.get())
                        .build(null));

        ESSENCE_CAULDRON_ENTITY = BLOCK_ENTITIES.register("essence_cauldron",
                () -> BlockEntityType.Builder
                        .of(EssenceCauldronBlockEntity::new,
                                ESSENCE_CAULDRON.get())
                        .build(null));

        QUEST_BOARD_ENTITY = BLOCK_ENTITIES.register("quest_board",
                () -> BlockEntityType.Builder
                        .of(QuestBoardBlockEntity::new,
                                QUEST_BOARD.get())
                        .build(null));

        ESSENCE_VORTEX_ENTITY = BLOCK_ENTITIES.register("essence_vortex", () -> {
                Block[] validBlocks =
                        EssenceRegistry.VORTEXES.values().stream()
                                .map(RegistrySupplier::get)
                                .toArray(Block[]::new);
                return BlockEntityType.Builder
                        .of(EssenceVortexBlockEntity::new, validBlocks)
                        .build(null);
        });

        TORII_BELL_ENTITY = BLOCK_ENTITIES.register("torii_bell", () -> {
                BlockEntityType.BlockEntitySupplier<BellBlockEntity> factory =
                        (pos, state) -> new ToriiBellBlockEntity(pos, state);
                return BlockEntityType.Builder.of(factory, TORII_BELL.get(), WEATHERED_TORII_BELL.get()).build(null);
                });

        BLOCK_ENTITIES.register();
    }
}
