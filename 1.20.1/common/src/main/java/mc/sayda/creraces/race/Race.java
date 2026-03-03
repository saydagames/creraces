package mc.sayda.creraces.race;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import mc.sayda.creraces.engine.TraitRegistry.RaceTrait;

/**
 * Represents a playable race in CreRaces.
 * Defined via JSON in data/creraces/races/
 */
public record Race(
                @Nonnull ResourceLocation id,
                @Nonnull Component name,
                @Nonnull Component description,
                @Nonnull ResourceLocation icon,
                @Nonnull ResourceLocation portrait, // 43x43 grid portrait texture (e.g. race0.png)
                @Nonnull ResourceLocation splash, // splash art shown on details screen
                @Nullable ResourceLocation nameTexture, // race name image (e.g. undead_text.png)
                @Nullable ResourceLocation parentRace, // null = top-level, non-null = sub-race of parent group
                double legacyId, // old numeric ID (e.g. 1.0 for undead)
                int baseAp,
                int baseAd,
                int baseAh,
                int baseCr,
                @Nonnull RaceScale scale,
                int maxResource,
                @Nonnull ResourceType resourceType,
                @Nonnull ResourceLocation bgTexture, // background image (standard vs spirit)
                @Nonnull ResourceLocation tier, // e.g., "creraces:common", "creraces:rare"
                int difficulty, // 0-4 rating
                int splashX, int splashY, int splashW, int splashH,
                int nameTexX, int nameTexY, int nameTexW, int nameTexH,
                @Nonnull List<RaceCustomization> customization,
                @Nonnull List<ResourceLocation> startingAbilities,
                @Nonnull List<ResourceLocation> startingItems,
                @Nullable Passives passives,
                @Nonnull List<RaceTrait> traits,
                boolean isSpirit,
                boolean isTiny,
                /**
                 * When true, the player's current stacks count is subtracted from maxResource
                 * before applying the resource attribute modifier. Previously hardcoded for
                 * 'lycan'.
                 */
                boolean stacksAffectResource) {

        /**
         * Static passive traits that don't require abilities/effects
         */
        public record Passives(
                        // Breathing & Environmental
                        boolean canBreatheUnderwater,
                        boolean canBreatheOnLand,
                        boolean burnsInSunlight,
                        @Nonnull List<String> immuneToDamageTypes, // e.g., ["fire", "drown", "wither", "fall"]
                        @Nonnull List<String> immuneToPotionEffects, // e.g., ["minecraft:slowness", "minecraft:poison"]

                        // Vision & Perception
                        boolean nightVision,
                        boolean waterVision,
                        boolean lavaVision,

                        // Movement & Physics
                        mc.sayda.creraces.engine.ScalingValue fallDamageMultiplier, // 0.0 = immune, 0.5 = 50%
                                                                                    // reduction, 1.0 = normal, 2.0 =
                                                                                    // double
                        boolean canFly, // Note: Actual flight handled by energy/conditions in code
                        mc.sayda.creraces.engine.ScalingValue liquidSpeedMultiplier, // 0.0 = can't move, 1.0 = normal,
                                                                                     // 2.0 = double speed
                        boolean unaffectedByWater, // Walk/see normally in water, no slowdown
                        boolean unaffectedByLava, // Walk/see normally in lava, no slowdown
                        boolean cannotSprint,

                        // Health & Regeneration
                        boolean noNaturalRegeneration,
                        mc.sayda.creraces.engine.ScalingValue regenerationMultiplier, // 0.0 = none, 1.0 = normal, 2.0 =
                                                                                      // double, etc.

                        // Combat & Damage
                        boolean immuneToKnockback,
                        mc.sayda.creraces.engine.ScalingValue invulnerabilityTicksMultiplier, // 0.0 = none, 1.0 =
                                                                                              // normal (20 ticks), 2.0
                                                                                              // = double

                        // Food & Hunger
                        boolean noHunger,
                        boolean noHungerDrain, // Hunger bar never decreases; eating still requires < 20 hunger
                        mc.sayda.creraces.engine.ScalingValue fixedHunger, // 0 = disabled, 1-20 = locked hunger level
                        boolean canEatMeat, // false = vegetarian
                        boolean canEatWhenFull, // Can eat even at max hunger

                        // Social & Interaction - Dynamic lists
                        @Nonnull List<String> hatedByEntities, // These entities attack this race on sight
                        @Nonnull List<String> respectedByEntities, // These entities ignore/are friendly to this race
                        @Nonnull List<String> defendedByEntities, // These entities defend this race when attacked

                        // Equipment Restrictions
                        boolean cannotWearHelmet,
                        boolean cannotWearChestplate,
                        boolean cannotWearLeggings,
                        boolean cannotWearBoots,

                        // Special Mechanics
                        @Nullable EntitySpawnData spawnOnDeath // Entity to spawn when killed
        ) {
                public static Passives DEFAULT = new Passives(
                                false, true, false, List.of(), List.of(), // Breathing (+ effect immunity)
                                false, false, false, // Vision
                                new mc.sayda.creraces.engine.ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
                                false,
                                new mc.sayda.creraces.engine.ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
                                false, false, false, // Movement
                                // (1.0 =
                                // normal)
                                false,
                                new mc.sayda.creraces.engine.ScalingValue(1.0, null, 0, new java.util.ArrayList<>()), // Health
                                                                                                                      // (1.0
                                                                                                                      // =
                                                                                                                      // normal
                                // regen)
                                false,
                                new mc.sayda.creraces.engine.ScalingValue(1.0, null, 0, new java.util.ArrayList<>()), // Combat
                                                                                                                      // (1.0
                                                                                                                      // =
                                                                                                                      // normal
                                // invuln)
                                false, false,
                                new mc.sayda.creraces.engine.ScalingValue(0.0, null, 0, new java.util.ArrayList<>()),
                                true, false, // Food
                                // (noHunger,
                                // noHungerDrain,
                                // fixedHunger,
                                // canEatMeat, canEatWhenFull)
                                List.of(), List.of(), List.of(), // Social (empty lists)
                                false, false, false, false, // Equipment
                                null // Special
                );
        }

        /**
         * Entity spawn configuration for spawnOnDeath
         */
        public record EntitySpawnData(
                        @Nonnull String entityType, // e.g., "minecraft:zombie"
                        @Nonnull String nbt, // NBT data as JSON string
                        int count // Number to spawn
        ) {
        }

        public String getTranslationKey() {
                return "race." + id.getNamespace() + "." + id.getPath();
        }
}
