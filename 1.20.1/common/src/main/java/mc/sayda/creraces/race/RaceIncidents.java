package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

public class RaceIncidents {
    public static void transformPlayer(ServerPlayer player, ResourceLocation raceId) {
        // Clear all engine-managed attribute modifiers before transforming
        AttributeIncidents.purgeRacialAttributes(player);

        if (raceId.equals(RaceRegistry.NONE)) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                vars.fantasySealReset();
                // Clear cosmetic addons when resetting race
                CosmeticIncidents.clearAllRacialAddons(player);

                // gState: restore forced aesthetics (model/chest) after reset
                CosmeticIncidents.applyGStateAddons(player);

                AttributeIncidents.eikiJudgment(player);

                // Scrub any lingering potion effects that might have been applied by passives
                // or traits, ensuring the player is completely clean when unequipped.
                player.removeAllEffects();

                // Reset Scale
                applyScale(player, RaceScale.DEFAULT);

                // Reset Flight flags (if not in creative/spectator)
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }

                BoundaryHandler.resyncVariables(player, player);
            });
            return;
        }

        Race race = RaceRegistry.get(raceId);
        if (race == null)
            return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            // Reset state first (Fantasy Seal)
            vars.fantasySealReset();

            // Set new race
            vars.setRace(raceId);
            vars.setHasChosenRace(true);
            var stunned = mc.sayda.creraces.registry.ModMobEffects.STUNNED.get();
            if (stunned != null) {
                player.removeEffect(stunned);
            }
            player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);

            // Apply Scale (with fairy realm override if needed)
            applyScaleWithDimensionOverride(player, race);

            // Apply Base Stats
            vars.setAp(race.baseAp());
            vars.setAd(race.baseAd());
            vars.setAh(race.baseAh());
            vars.setCr(race.baseCr());

            // Apply Eiki's Judgment (Sync attributes to Vanilla)
            AttributeIncidents.eikiJudgment(player);

            // Initialize resources to max
            vars.setMana((int) player
                    .getAttributeValue(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModAttributes
                            .resolve(mc.sayda.creraces.registry.ModAttributes.MAX_MANA))));
            vars.setGrit(0);
            vars.setEnergy((int) player
                    .getAttributeValue(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModAttributes
                            .resolve(mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY))));
            vars.setRage(0);

            // Apply Default Customizations
            if (race.customization() != null) {
                for (mc.sayda.creraces.race.RaceCustomization cust : race.customization()) {
                    vars.setCustomization(cust.id(), cust.getDefaultValue(raceId));
                }
            }
            // Apply female model and/or chest addon if this race forces a gState
            // This MUST happen before applyCustomizations so trait-based addons see the
            // correct gState if they depend on it.
            CosmeticIncidents.applyGStateCosmetics(player, race, vars);

            CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(), race);

            // Grant Starting Abilities
            if (race.startingAbilities() != null) {
                int equippedCount = 0;
                for (ResourceLocation abilityId : race.startingAbilities()) {
                    // Unlock
                    vars.unlockAbility(abilityId);

                    // Auto-equip first two to A1 and A2
                    if (equippedCount == 0) {
                        vars.equipAbility(mc.sayda.creraces.ability.AbilitySlot.A1, abilityId);
                    } else if (equippedCount == 1) {
                        vars.equipAbility(mc.sayda.creraces.ability.AbilitySlot.A2, abilityId);
                    }
                    equippedCount++;
                }
            }

            // Grant Starting Items (Server side only)
            if (race.startingItems() != null && !player.level().isClientSide()) {
                for (ResourceLocation itemId : race.startingItems()) {
                    if (itemId == null)
                        continue;
                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .get(itemId);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        player.getInventory().add(new net.minecraft.world.item.ItemStack(item));
                    }
                }
            }

            // Final Sync - vars.sync handles both local and tracking players
            vars.sync(player);

            // Fire on_select traits (e.g. grace effects defined in the race JSON)
            if (race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    trait.onSelect(player);
                }
            }

        });

        // Teleport to race-specific selection dimension only for fresh players
        // (no existing territory/root: existing players keep their overworld context)
        if (race.selectionDimension() != null && player.getServer() != null) {
            boolean hasRoot = DataUtils.getVariables(player)
                .map(v -> v.getPersistentState(new net.minecraft.resources.ResourceLocation("creraces", "node_x")) != 0.0)
                .orElse(true);
            if (!hasRoot) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey =
                    net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        race.selectionDimension());
                net.minecraft.server.level.ServerLevel targetLevel = player.getServer().getLevel(dimKey);
                if (targetLevel != null) {
                    double[] pos = race.selectionPos();
                    double sx = pos != null ? pos[0] : 0.5;
                    double sy = pos != null ? pos[1] : 65.0;
                    double sz = pos != null ? pos[2] : 0.5;
                    player.teleportTo(targetLevel, sx, sy, sz, player.getYRot(), player.getXRot());
                    BoundaryHandler.resyncForAllTrackers(player);
                    BoundaryHandler.resyncVariables(player, player);
                }
            }
        }
    }

    /**
     * Lightweight refresh for player's race attributes and cosmetics.
     */
    public static void refreshPlayer(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation raceId = vars.getRace();
            Race race = RaceRegistry.get(raceId);

            // Re-apply Vanilla Attributes (Attributes may still depend on race even if
            // null-checked inside)
            AttributeIncidents.eikiJudgment(player);

            if (race != null) {
                // Re-apply Scale (with fairy realm override if needed)
                applyScaleWithDimensionOverride(player, race);
            }

            // Re-apply GState Cosmetics (Crucial: This must happen even for NONE race
            // players)
            if (race != null) {
                CosmeticIncidents.applyGStateCosmetics(player, race, vars);
                // Re-apply Customizations (already handles gstate logic if needed)
                CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(), race);
            } else {
                CosmeticIncidents.applyGStateAddons(player, true);
                // Reset Flight flags on refresh for NONE race (e.g. login cleanup)
                if (!player.isCreative() && !player.isSpectator()) {
                    if (player.getAbilities().mayfly || player.getAbilities().flying) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                }
            }

            // Full Sync to client and trackers
            vars.sync(player);
        });
    }

    private static final net.minecraft.resources.ResourceLocation FAIRY_REALM =
            new net.minecraft.resources.ResourceLocation(mc.sayda.creraces.CreRaces.MODID, "fairy_realm");

    /**
     * Applies the race scale, then overrides with the fairy realm scale if the
     * player is currently inside fairy_realm. Use this everywhere a race change
     * can happen so the dimension context is always respected.
     */
    public static void applyScaleWithDimensionOverride(net.minecraft.server.level.ServerPlayer player, Race race) {
        applyScale(player, race.scale());
        if (player.level().dimension().location().equals(FAIRY_REALM)) {
            applyFairyRealmScale(player, race);
        }
    }

    public static void applyScale(net.minecraft.world.entity.LivingEntity entity, RaceScale scale) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player))
            return;

        try {
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.BASE, (float) scale.base().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.WIDTH, (float) scale.width().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.HEIGHT, (float) scale.height().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.HITBOX_WIDTH,
                    (float) scale.hitboxWidth().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.HITBOX_HEIGHT,
                    (float) scale.hitboxHeight().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.EYE_HEIGHT,
                    (float) scale.eyeHeight().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.REACH, (float) scale.reach().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.MINING_SPEED,
                    (float) scale.miningSpeed().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.MOTION, (float) scale.motion().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.STEP_HEIGHT,
                    (float) scale.stepHeight().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.JUMP_HEIGHT,
                    (float) scale.jumpHeight().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.KNOCKBACK,
                    (float) scale.knockback().evaluate(player));
            applyScaleType(entity, virtuoel.pehkui.api.ScaleTypes.FALLING, (float) scale.fallSpeed().evaluate(player));
        } catch (Throwable e) {
            // Pehkui may be absent or throw on unsupported scale types, not fatal, but log
            // it
            mc.sayda.creraces.CreRaces.LOGGER.warn("Failed to apply scale for entity {}: {}",
                    entity.getName().getString(), e.getMessage());
        }
    }

    /**
     * Sets only the Pehkui BASE scale to 4× the race's configured BASE scale,
     * letting Pehkui derive all other scale types from BASE automatically.
     */
    public static void applyFairyRealmScale(net.minecraft.server.level.ServerPlayer player, Race race) {
        float base = (float) race.scale().base().evaluate(player) * 4.0f;
        applyFlatBaseScale(player, base);
    }

    public static void applyFlatBaseScale(net.minecraft.world.entity.LivingEntity entity, float scale) {
        try {
            virtuoel.pehkui.api.ScaleData data = virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(entity);
            data.setScale(scale);
            data.setTargetScale(scale);
        } catch (Throwable e) {
            mc.sayda.creraces.CreRaces.LOGGER.warn("Failed to apply flat scale for {}: {}",
                    entity.getName().getString(), e.getMessage());
        }
    }

    private static void applyScaleType(net.minecraft.world.entity.LivingEntity entity,
            virtuoel.pehkui.api.ScaleType type,
            float scale) {
        virtuoel.pehkui.api.ScaleData data = type.getScaleData(entity);
        data.setScale(scale);
        data.setTargetScale(scale);
    }
}
