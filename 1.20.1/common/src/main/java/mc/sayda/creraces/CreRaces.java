package mc.sayda.creraces;

import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import mc.sayda.creraces.ability.AbilityManager;
import mc.sayda.creraces.ability.HexRecipeManager;
import mc.sayda.creraces.block.QuestBoardBlock;
import mc.sayda.creraces.block.RootBlock;
import mc.sayda.creraces.block.ToriiGateInteractions;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.commands.CreracesCommand;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.SpiritSpawningHandler;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.global.GlobalManager;
import mc.sayda.creraces.mixin.AxeItemAccessor;
import mc.sayda.creraces.mixin.BlockEntityTypeAccessor;
import mc.sayda.creraces.mixin.FireBlockAccessor;
import mc.sayda.creraces.mixin.PoiTypesAccessor;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.quest.QuestManager;
import mc.sayda.creraces.race.RaceManager;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModEnchantments;
import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.registry.ModFeatures;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModGameRules;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModMenuTypes;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.registry.ModParticles;
import mc.sayda.creraces.registry.ModPoiTypes;
import mc.sayda.creraces.registry.ModPotions;
import mc.sayda.creraces.registry.ModRecipes;
import mc.sayda.creraces.registry.ModSounds;
import mc.sayda.creraces.registry.ModTabs;
import mc.sayda.creraces.registry.ModVillagerProfessions;
import mc.sayda.creraces.registry.ModWoodTypes;
import mc.sayda.creraces.util.DocCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreRaces {
    public static final String MODID = "creraces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("CreRaces is loading...");

        DocCache.init(Platform.getConfigFolder());

        ReloadListenerRegistry.register(PackType.SERVER_DATA, new AbilityManager());
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new RaceManager());
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new QuestManager());
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new HexRecipeManager());
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new GlobalManager());

        BoundaryHandler.init();

        ActionRegistry.init();
        ConditionRegistry.init();
        TraitRegistry.init();

        IncidentResolver.init();
        SpiritSpawningHandler.init();
        ToriiGateInteractions.register();

        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (state.getBlock() instanceof RootBlock) {
                if (player != null && !player.isCreative() && !RootBlock.isOwner(player, pos)) {
                    return EventResult.interruptFalse();
                }
            }

            // A micro block only breaks as a whole for a sneaking player outside small-build mode;
            // in small-build mode single slots are removed through MiniRemovePacket instead.
            if (state.is(ModBlocks.MICRO_BLOCK.get()) && player != null) {
                boolean isSmallBuild = DataUtils.getVariables(player)
                        .map(IPlayerVariables::isSmallBuild)
                        .orElse(false);
                if (isSmallBuild || !player.isShiftKeyDown()) {
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        });

        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> CreracesCommand.register(dispatcher));

        ModWoodTypes.init();

        ModAttributes.init();
        ModGameRules.init();
        ModEnchantments.register();
        ModMobEffects.register();
        ModPotions.register();
        ModEntities.register();
        ModParticles.register();

        // Fluids must be registered before blocks due to fluid states in block registration
        ModFluids.register();
        ModBlocks.register();
        ModSounds.register();
        ModItems.register();
        ModRecipes.register();
        ModTabs.register();
        ModMenuTypes.register();
        ModFeatures.register();
        ModPoiTypes.register();
        ModVillagerProfessions.register();

        LifecycleEvent.SETUP.register(() -> {
            registerStrippables();
            extendSignBlockEntities();

            // Must run here, not from ModEntities.register(): FLOATING_MOTE.get() throws "Registry
            // Object not present" if resolved synchronously during mod construction.
            ModEntities.registerSpawnPlacements();

            registerReceptionistPoiStates();
            registerCompostables();
            registerFlammables();
        });

        LOGGER.info("CreRaces initialized (Common).");
    }

    /**
     * Axe stripping for custom logs. This goes through AxeItemAccessor (a mixin), not the access
     * widener: on real (non-dev) NeoForge, JPMS module boundaries still reject cross-module field
     * access even once the flag says public, while a mixin accessor patches the field's own class.
     * 1.20.1 does it the same way to stay in step with 1.21.1.
     */
    private static void registerStrippables() {
        Map<Block, Block> strippables = new HashMap<>(AxeItemAccessor.creraces$getStrippables());
        strippables.put(ModBlocks.DRYAD_LOG.get(), ModBlocks.STRIPPED_DRYAD_LOG.get());
        strippables.put(ModBlocks.DRYAD_WOOD.get(), ModBlocks.STRIPPED_DRYAD_WOOD.get());
        strippables.put(ModBlocks.VEIL_WILLOW_LOG.get(), ModBlocks.STRIPPED_VEIL_WILLOW_LOG.get());
        strippables.put(ModBlocks.VEIL_WILLOW_WOOD.get(), ModBlocks.STRIPPED_VEIL_WILLOW_WOOD.get());
        AxeItemAccessor.creraces$setStrippables(Collections.unmodifiableMap(strippables));
    }

    /**
     * BlockEntityRenderDispatcher skips any block entity whose type.isValid() is false, so our
     * sign blocks would be invisible unless SIGN/HANGING_SIGN accept them. BlockEntityTypeAccessor
     * is a mixin accessor for the same JPMS reason as in registerStrippables.
     */
    private static void extendSignBlockEntities() {
        addValidBlocks(BlockEntityType.SIGN,
                ModBlocks.DRYAD_SIGN.get(), ModBlocks.DRYAD_WALL_SIGN.get(),
                ModBlocks.VEIL_WILLOW_SIGN.get(), ModBlocks.VEIL_WILLOW_WALL_SIGN.get());
        addValidBlocks(BlockEntityType.HANGING_SIGN,
                ModBlocks.DRYAD_HANGING_SIGN.get(), ModBlocks.DRYAD_WALL_HANGING_SIGN.get(),
                ModBlocks.VEIL_WILLOW_HANGING_SIGN.get(), ModBlocks.VEIL_WILLOW_WALL_HANGING_SIGN.get());
    }

    private static void addValidBlocks(BlockEntityType<?> type, Block... blocks) {
        var accessor = (BlockEntityTypeAccessor) type;
        Set<Block> validBlocks = new HashSet<>(accessor.creraces$getValidBlocks());
        Collections.addAll(validBlocks, blocks);
        accessor.creraces$setValidBlocks(Collections.unmodifiableSet(validBlocks));
    }

    /**
     * PoiTypes.TYPE_BY_STATE only maps vanilla's own bootstrap states, so modded PoiTypes
     * need their states added manually once registration finishes.
     */
    private static void registerReceptionistPoiStates() {
        var holder = BuiltInRegistries.POINT_OF_INTEREST_TYPE.wrapAsHolder(ModPoiTypes.GUILD_RECEPTIONIST.get());
        var typeByState = PoiTypesAccessor.creraces$getTypeByState();
        ModBlocks.QUEST_BOARD.get().getStateDefinition().getPossibleStates().stream()
                .filter(QuestBoardBlock::isMaster)
                .forEach(state -> typeByState.putIfAbsent(state, holder));
    }

    private static void registerCompostables() {
        ComposterBlock.COMPOSTABLES.put(ModItems.DRYAD_SAPLING_ITEM.get(), 0.3f);
        ComposterBlock.COMPOSTABLES.put(ModItems.DRYAD_LEAVES_ITEM.get(), 0.3f);
        ComposterBlock.COMPOSTABLES.put(ModItems.DRYAD_LEAVES_FLOWERING_ITEM.get(), 0.3f);
        ComposterBlock.COMPOSTABLES.put(ModItems.DRYAD_LEAVES_FRUIT_ITEM.get(), 0.3f);
    }

    /**
     * FireBlock.setFlammable is private on 1.20.1 (public from 1.21), so this goes through
     * FireBlockAccessor. Plain reflection would be a cross-module call on JPMS loaders, and the
     * resulting InaccessibleObjectException isn't even a ReflectiveOperationException.
     */
    private static void registerFlammables() {
        var fire = (FireBlockAccessor) Blocks.FIRE;
        // Veil Willow: logs/bark
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_LOG.get(), 5, 5);
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_WOOD.get(), 5, 5);
        // Veil Willow: planks and derivatives
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_PLANKS.get(), 5, 20);
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_STAIRS.get(), 5, 20);
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_SLAB.get(), 5, 20);
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_FENCE.get(), 5, 20);
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_FENCE_GATE.get(), 5, 20);
        // Veil Willow: leaves and drape (vines burn fast)
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_LEAVES.get(), 30, 60);
        fire.creraces$callSetFlammable(ModBlocks.VEIL_WILLOW_DRAPE.get(), 15, 100);
        // Stripped logs and woods
        fire.creraces$callSetFlammable(ModBlocks.STRIPPED_DRYAD_LOG.get(), 5, 5);
        fire.creraces$callSetFlammable(ModBlocks.STRIPPED_DRYAD_WOOD.get(), 5, 5);
        fire.creraces$callSetFlammable(ModBlocks.STRIPPED_VEIL_WILLOW_LOG.get(), 5, 5);
        fire.creraces$callSetFlammable(ModBlocks.STRIPPED_VEIL_WILLOW_WOOD.get(), 5, 5);
    }
}
