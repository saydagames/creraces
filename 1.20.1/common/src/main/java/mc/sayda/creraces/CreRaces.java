package mc.sayda.creraces;

import com.mojang.logging.LogUtils;
import dev.architectury.registry.ReloadListenerRegistry;
import mc.sayda.creraces.ability.AbilityManager;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

public class CreRaces {
    public static final String MODID = "creraces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("CreRaces is loading...");

        mc.sayda.creraces.util.DocCache.init(dev.architectury.platform.Platform.getConfigFolder());

        ReloadListenerRegistry.register(PackType.SERVER_DATA, new AbilityManager());
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new mc.sayda.creraces.race.RaceManager());
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new mc.sayda.creraces.ability.HexRecipeManager());

        BoundaryHandler.init();
        mc.sayda.creraces.ability.ModAbilities.registerExecutors();

        mc.sayda.creraces.engine.ActionRegistry.init();
        mc.sayda.creraces.engine.condition.ConditionRegistry.init();
        mc.sayda.creraces.engine.TraitRegistry.init();

        IncidentResolver.init();
        mc.sayda.creraces.engine.SpiritSpawningHandler.init();

        dev.architectury.event.events.common.BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (state.getBlock() instanceof mc.sayda.creraces.block.RootBlock) {
                if (player != null && !player.isCreative() && !mc.sayda.creraces.block.RootBlock.isOwner(player, pos)) {
                    return dev.architectury.event.EventResult.interruptFalse();
                }
            }

            if (state.is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
                if (player != null) {
                    boolean isSmallBuild = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                            .map(mc.sayda.creraces.capability.IPlayerVariables::isSmallBuild)
                            .orElse(false);

                    if (isSmallBuild) {
                        return dev.architectury.event.EventResult.interruptFalse();
                    } else if (!player.isShiftKeyDown()) {
                        return dev.architectury.event.EventResult.interruptFalse();
                    }
                }
            }
            return dev.architectury.event.EventResult.pass();
        });

        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT
                .register((dispatcher, registry, selection) -> {
                    mc.sayda.creraces.commands.CreracesCommand.register(dispatcher);
                });

        mc.sayda.creraces.registry.ModWoodTypes.init();

        mc.sayda.creraces.registry.ModAttributes.init();
        mc.sayda.creraces.registry.ModGameRules.init();
        mc.sayda.creraces.registry.ModEnchantments.register();
        mc.sayda.creraces.registry.ModMobEffects.register();
        mc.sayda.creraces.registry.ModPotions.register();
        mc.sayda.creraces.registry.ModEntities.register();
        mc.sayda.creraces.registry.ModParticles.register();

        // Fluids must be registered before blocks due to fluid states in block registration
        mc.sayda.creraces.registry.ModFluids.register();
        mc.sayda.creraces.registry.ModBlocks.register();
        mc.sayda.creraces.registry.ModSounds.register();
        mc.sayda.creraces.registry.ModItems.register();
        mc.sayda.creraces.registry.ModRecipes.register();
        mc.sayda.creraces.registry.ModTabs.register();
        mc.sayda.creraces.registry.ModMenuTypes.register();
        mc.sayda.creraces.registry.ModFeatures.register();

        dev.architectury.event.events.common.LifecycleEvent.SETUP.register(() -> {
            // Register axe stripping for custom logs.
            // Direct field access works because the AW (Fabric) and AT (Forge) both widen
            // AxeItem.STRIPPABLES. Reflection with a string literal fails in production because
            // Loom remaps field references but not string literals.
            java.util.Map<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> newStrippables =
                    new java.util.HashMap<>(net.minecraft.world.item.AxeItem.STRIPPABLES);
            newStrippables.put(mc.sayda.creraces.registry.ModBlocks.DRYAD_LOG.get(),
                    mc.sayda.creraces.registry.ModBlocks.STRIPPED_DRYAD_LOG.get());
            newStrippables.put(mc.sayda.creraces.registry.ModBlocks.DRYAD_WOOD.get(),
                    mc.sayda.creraces.registry.ModBlocks.STRIPPED_DRYAD_WOOD.get());
            newStrippables.put(mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_LOG.get(),
                    mc.sayda.creraces.registry.ModBlocks.STRIPPED_VEIL_WILLOW_LOG.get());
            newStrippables.put(mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_WOOD.get(),
                    mc.sayda.creraces.registry.ModBlocks.STRIPPED_VEIL_WILLOW_WOOD.get());
            net.minecraft.world.item.AxeItem.STRIPPABLES =
                    java.util.Collections.unmodifiableMap(newStrippables);

            // Extend BlockEntityType.SIGN/HANGING_SIGN to include our custom sign blocks.
            // BlockEntityRenderDispatcher.render() skips any block entity whose type.isValid()
            // returns false, making them invisible even though they exist and have a renderer.
            // The access widener makes validBlocks public+mutable so we can write it directly.
            java.util.Set<net.minecraft.world.level.block.Block> newSignBlocks =
                    new java.util.HashSet<>(net.minecraft.world.level.block.entity.BlockEntityType.SIGN.validBlocks);
            newSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.DRYAD_SIGN.get());
            newSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.DRYAD_WALL_SIGN.get());
            newSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_SIGN.get());
            newSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_WALL_SIGN.get());
            net.minecraft.world.level.block.entity.BlockEntityType.SIGN.validBlocks =
                    java.util.Collections.unmodifiableSet(newSignBlocks);

            java.util.Set<net.minecraft.world.level.block.Block> newHangingSignBlocks =
                    new java.util.HashSet<>(net.minecraft.world.level.block.entity.BlockEntityType.HANGING_SIGN.validBlocks);
            newHangingSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.DRYAD_HANGING_SIGN.get());
            newHangingSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.DRYAD_WALL_HANGING_SIGN.get());
            newHangingSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_HANGING_SIGN.get());
            newHangingSignBlocks.add(mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_WALL_HANGING_SIGN.get());
            net.minecraft.world.level.block.entity.BlockEntityType.HANGING_SIGN.validBlocks =
                    java.util.Collections.unmodifiableSet(newHangingSignBlocks);

            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(
                    mc.sayda.creraces.registry.ModItems.DRYAD_SAPLING_ITEM.get(), 0.3f);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(
                    mc.sayda.creraces.registry.ModItems.DRYAD_LEAVES_ITEM.get(), 0.3f);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(
                    mc.sayda.creraces.registry.ModItems.DRYAD_LEAVES_FLOWERING_ITEM.get(), 0.3f);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(
                    mc.sayda.creraces.registry.ModItems.DRYAD_LEAVES_FRUIT_ITEM.get(), 0.3f);

            try {
                java.lang.reflect.Method setFlammable = net.minecraft.world.level.block.FireBlock.class
                        .getDeclaredMethod("setFlammable",
                                net.minecraft.world.level.block.Block.class, int.class, int.class);
                setFlammable.setAccessible(true);
                net.minecraft.world.level.block.FireBlock fire =
                        (net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE;
                // Veil Willow: logs/bark
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_LOG.get(), 5, 5);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_WOOD.get(), 5, 5);
                // Veil Willow: planks and derivatives
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_PLANKS.get(), 5, 20);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_STAIRS.get(), 5, 20);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_SLAB.get(), 5, 20);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_FENCE.get(), 5, 20);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_FENCE_GATE.get(), 5, 20);
                // Veil Willow: leaves and drape (vines burn fast)
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_LEAVES.get(), 30, 60);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_DRAPE.get(), 15, 100);
                // Stripped logs and woods
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.STRIPPED_DRYAD_LOG.get(), 5, 5);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.STRIPPED_DRYAD_WOOD.get(), 5, 5);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.STRIPPED_VEIL_WILLOW_LOG.get(), 5, 5);
                setFlammable.invoke(fire, mc.sayda.creraces.registry.ModBlocks.STRIPPED_VEIL_WILLOW_WOOD.get(), 5, 5);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("Failed to register veil willow flammability: {}", e.getMessage());
            }
        });

        LOGGER.info("CreRaces initialized (Common).");
    }
}