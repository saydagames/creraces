package mc.sayda.creraces.race;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.territory.FactionLeaderManager;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;

import javax.annotation.Nullable;
import java.util.Objects;

public class RaceIncidents {
    private static final ResourceLocation FAIRY_REALM = new ResourceLocation(CreRaces.MODID, "fairy_realm");
    private static final float FAIRY_REALM_SCALE_MULTIPLIER = 4.0f;
    // Set once a player has placed a territory root; such players are no longer "fresh".
    // Starting abilities fill these slots in order; any beyond them are only unlocked.
    private static final AbilitySlot[] STARTING_ABILITY_SLOTS = { AbilitySlot.A1, AbilitySlot.A2 };

    public static void transformPlayer(ServerPlayer player, ResourceLocation raceId) {
        if (raceId.equals(RaceRegistry.NONE)) {
            leaveCurrentRace(player, null);
            resetToNoRace(player);
            return;
        }

        Race race = RaceRegistry.get(raceId);
        if (race == null)
            return;

        leaveCurrentRace(player, race);

        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.fantasySealReset();

            vars.setRace(raceId);
            vars.setHasChosenRace(true);

            applyScaleWithDimensionOverride(player, race);

            vars.setAp(race.baseAp());
            vars.setAd(race.baseAd());
            vars.setAh(race.baseAh());
            vars.setCr(race.baseCr());

            // Base stats first: max mana/energy below depend on the modifiers this applies.
            AttributeIncidents.eikiJudgment(player);

            vars.setMana((int) player.getAttributeValue(Objects.requireNonNull(ModAttributes.resolve(ModAttributes.MAX_MANA))));
            vars.setGrit(0);
            vars.setEnergy((int) player.getAttributeValue(Objects.requireNonNull(ModAttributes.resolve(ModAttributes.MAX_ENERGY))));
            vars.setRage(0);
            player.setHealth(player.getMaxHealth());

            if (race.customization() != null) {
                for (RaceCustomization cust : race.customization()) {
                    vars.setCustomization(cust.id(), cust.getDefaultValue(raceId));
                }
            }
            // Must run before applyCustomizations so trait-based addons see the forced gState.
            CosmeticIncidents.applyGStateCosmetics(player, race, vars);
            CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(), race);

            if (race.startingAbilities() != null) {
                int index = 0;
                for (ResourceLocation abilityId : race.startingAbilities()) {
                    vars.unlockAbility(abilityId);
                    if (index < STARTING_ABILITY_SLOTS.length) {
                        vars.equipAbility(STARTING_ABILITY_SLOTS[index], abilityId);
                    }
                    index++;
                }
            }

            if (race.startingItems() != null) {
                for (ResourceLocation itemId : race.startingItems()) {
                    if (itemId == null)
                        continue;
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item != null && item != Items.AIR) {
                        player.getInventory().add(new ItemStack(item));
                    }
                }
            }

            // Syncs to the player and everyone tracking them.
            vars.sync(player);

            TraitDispatch.runVoid("RaceIncidents.onSelect", player, trait -> trait.onSelect(player));
        });

        teleportToSelectionDimension(player, race);
    }

    /** Clears what the old race left behind, before either a reset or a new race is applied. */
    private static void leaveCurrentRace(ServerPlayer player, @Nullable Race newRace) {
        // Passives and traits may have left effects behind. Clearing them before the purge lets
        // vanilla remove the attribute modifiers they own.
        player.removeAllEffects();
        AttributeIncidents.purgeRacialAttributes(player);

        // Territory is race-bound. This runs while the old race is still set, so leadership
        // passes within the old faction group.
        FactionLeaderManager.onRaceChange(player, newRace);
        TerritoryManager.get().unclaimAllForPlayer(player.getUUID());

        // A race change is a fresh start, so the player leaves their team too.
        RaceTeamManager.leaveTeam(player);
    }

    private static void resetToNoRace(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.fantasySealReset();
            CosmeticIncidents.clearAllRacialAddons(player);
            // Clearing addons also dropped the gState model/chest, so restore those.
            CosmeticIncidents.applyGStateAddons(player);

            AttributeIncidents.eikiJudgment(player);
            player.setHealth(player.getMaxHealth());

            applyScale(player, RaceScale.DEFAULT);

            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }

            BoundaryHandler.resyncVariables(player, player);
        });
    }

    /** Starts the player over in the new race's selection dimension, if it has one. */
    private static void teleportToSelectionDimension(ServerPlayer player, Race race) {
        if (race.selectionDimension() == null || player.getServer() == null)
            return;

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, race.selectionDimension());
        ServerLevel targetLevel = player.getServer().getLevel(dimKey);
        if (targetLevel == null)
            return;

        double[] pos = race.selectionPos();
        double x = pos != null ? pos[0] : 0.5;
        double y = pos != null ? pos[1] : 65.0;
        double z = pos != null ? pos[2] : 0.5;
        player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
        BoundaryHandler.resyncForAllTrackers(player);
        BoundaryHandler.resyncVariables(player, player);
    }

    /**
     * Re-applies attributes, scale and cosmetics for the player's current race without resetting
     * anything, e.g. on login.
     */
    public static void refreshPlayer(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());

            // Runs even without a race: eikiJudgment purges leftover racial modifiers then.
            AttributeIncidents.eikiJudgment(player);

            if (race != null) {
                applyScaleWithDimensionOverride(player, race);
                CosmeticIncidents.applyGStateCosmetics(player, race, vars);
                CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(), race);
            } else {
                // Players without a race still get their gState model/chest.
                CosmeticIncidents.applyGStateAddons(player, true);
                // Clears flight left over from a previous race.
                if (!player.isCreative() && !player.isSpectator()
                        && (player.getAbilities().mayfly || player.getAbilities().flying)) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }

            vars.sync(player);
        });
    }

    /**
     * Applies the race scale, then the fairy realm override if the player is inside it. Use this
     * wherever a race change can happen so the dimension is always respected.
     */
    public static void applyScaleWithDimensionOverride(ServerPlayer player, Race race) {
        applyScale(player, race.scale());
        if (player.level().dimension().location().equals(FAIRY_REALM)) {
            applyFairyRealmScale(player, race);
        }
    }

    public static void applyScale(LivingEntity entity, RaceScale scale) {
        if (!(entity instanceof Player player))
            return;

        try {
            applyScaleType(player, ScaleTypes.BASE, scale.base());
            applyScaleType(player, ScaleTypes.WIDTH, scale.width());
            applyScaleType(player, ScaleTypes.HEIGHT, scale.height());
            applyScaleType(player, ScaleTypes.HITBOX_WIDTH, scale.hitboxWidth());
            applyScaleType(player, ScaleTypes.HITBOX_HEIGHT, scale.hitboxHeight());
            applyScaleType(player, ScaleTypes.EYE_HEIGHT, scale.eyeHeight());
            applyScaleType(player, ScaleTypes.REACH, scale.reach());
            applyScaleType(player, ScaleTypes.MINING_SPEED, scale.miningSpeed());
            applyScaleType(player, ScaleTypes.MOTION, scale.motion());
            applyScaleType(player, ScaleTypes.STEP_HEIGHT, scale.stepHeight());
            applyScaleType(player, ScaleTypes.JUMP_HEIGHT, scale.jumpHeight());
            applyScaleType(player, ScaleTypes.KNOCKBACK, scale.knockback());
            applyScaleType(player, ScaleTypes.FALLING, scale.fallSpeed());
        } catch (Throwable e) {
            // Throwable so a Pehkui API mismatch (a LinkageError) is logged instead of aborting the race change.
            CreRaces.LOGGER.warn("Failed to apply scale for entity {}: {}", entity.getName().getString(), e.getMessage());
        }
    }

    /**
     * Sets only Pehkui's BASE scale, to 4x the race's configured base, and lets Pehkui derive
     * every other scale type from it.
     */
    public static void applyFairyRealmScale(ServerPlayer player, Race race) {
        float base = (float) race.scale().base().evaluate(player) * FAIRY_REALM_SCALE_MULTIPLIER;
        applyFlatBaseScale(player, base);
    }

    public static void applyFlatBaseScale(LivingEntity entity, float scale) {
        try {
            setScale(ScaleTypes.BASE.getScaleData(entity), scale);
        } catch (Throwable e) {
            CreRaces.LOGGER.warn("Failed to apply flat scale for {}: {}", entity.getName().getString(), e.getMessage());
        }
    }

    private static void applyScaleType(Player player, ScaleType type, ScalingValue value) {
        setScale(type.getScaleData(player), (float) value.evaluate(player));
    }

    private static void setScale(ScaleData data, float scale) {
        data.setScale(scale);
        data.setTargetScale(scale);
    }
}
