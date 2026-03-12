package mc.sayda.creraces.race;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import mc.sayda.creraces.engine.TraitRegistry.RaceTrait;

/**
 * Represents a playable race in CreRaces.
 * Defined via JSON in data/creraces/races/
 */
public class Race {
        private final ResourceLocation id;
        private final net.minecraft.network.chat.Component name;
        private final net.minecraft.network.chat.Component description;
        private final ResourceLocation icon;
        private final ResourceLocation portrait;
        private final ResourceLocation splash;
        private final ResourceLocation nameTexture;
        private final ResourceLocation parentRace;
        private final double legacyId;
        private final int baseAp;
        private final int baseAd;
        private final int baseAh;
        private final int baseCr;
        private final RaceScale scale;
        private final int maxResource;
        private final ResourceType resourceType;
        private final ResourceLocation bgTexture;
        private final ResourceLocation tier;
        private final int difficulty;
        private final int splashX, splashY, splashW, splashH;
        private final int nameTexX, nameTexY, nameTexW, nameTexH;
        private final List<RaceCustomization> customization;
        private final List<ResourceLocation> startingAbilities;
        private final List<ResourceLocation> startingItems;
        private final Passives passives;
        private final List<RaceTrait> traits;
        private final boolean isSpirit;
        private final boolean isTiny;
        private final boolean stacksAffectResource;
        private final mc.sayda.creraces.engine.GState gState;

        public Race(ResourceLocation id, net.minecraft.network.chat.Component name,
                        net.minecraft.network.chat.Component description, ResourceLocation icon,
                        ResourceLocation portrait,
                        ResourceLocation splash, @Nullable ResourceLocation nameTexture,
                        @Nullable ResourceLocation parentRace,
                        double legacyId, int baseAp, int baseAd, int baseAh, int baseCr, RaceScale scale,
                        int maxResource,
                        ResourceType resourceType, ResourceLocation bgTexture, ResourceLocation tier, int difficulty,
                        int splashX,
                        int splashY, int splashW, int splashH, int nameTexX, int nameTexY, int nameTexW, int nameTexH,
                        List<RaceCustomization> customization, List<ResourceLocation> startingAbilities,
                        List<ResourceLocation> startingItems, @Nullable Passives passives, List<RaceTrait> traits,
                        boolean isSpirit,
                        boolean isTiny, boolean stacksAffectResource,
                        mc.sayda.creraces.engine.GState gState) {
                this.id = id;
                this.name = name;
                this.description = description;
                this.icon = icon;
                this.portrait = portrait;
                this.splash = splash;
                this.nameTexture = nameTexture;
                this.parentRace = parentRace;
                this.legacyId = legacyId;
                this.baseAp = baseAp;
                this.baseAd = baseAd;
                this.baseAh = baseAh;
                this.baseCr = baseCr;
                this.scale = scale;
                this.maxResource = maxResource;
                this.resourceType = resourceType;
                this.bgTexture = bgTexture;
                this.tier = tier;
                this.difficulty = difficulty;
                this.splashX = splashX;
                this.splashY = splashY;
                this.splashW = splashW;
                this.splashH = splashH;
                this.nameTexX = nameTexX;
                this.nameTexY = nameTexY;
                this.nameTexW = nameTexW;
                this.nameTexH = nameTexH;
                this.customization = customization;
                this.startingAbilities = startingAbilities;
                this.startingItems = startingItems;
                this.passives = passives;
                this.traits = traits;
                this.isSpirit = isSpirit;
                this.isTiny = isTiny;
                this.stacksAffectResource = stacksAffectResource;
                this.gState = gState;
        }

        public ResourceLocation id() {
                return id;
        }

        public net.minecraft.network.chat.Component name() {
                return name;
        }

        public net.minecraft.network.chat.Component description() {
                return description;
        }

        public ResourceLocation icon() {
                return icon;
        }

        public ResourceLocation portrait() {
                return portrait;
        }

        public ResourceLocation splash() {
                return splash;
        }

        public @Nullable ResourceLocation nameTexture() {
                return nameTexture;
        }

        public @Nullable ResourceLocation parentRace() {
                return parentRace;
        }

        public double legacyId() {
                return legacyId;
        }

        public int baseAp() {
                return baseAp;
        }

        public int baseAd() {
                return baseAd;
        }

        public int baseAh() {
                return baseAh;
        }

        public int baseCr() {
                return baseCr;
        }

        public RaceScale scale() {
                return scale;
        }

        public int maxResource() {
                return maxResource;
        }

        public ResourceType resourceType() {
                return resourceType;
        }

        public ResourceLocation bgTexture() {
                return bgTexture;
        }

        public ResourceLocation tier() {
                return tier;
        }

        public int difficulty() {
                return difficulty;
        }

        public int splashX() {
                return splashX;
        }

        public int splashY() {
                return splashY;
        }

        public int splashW() {
                return splashW;
        }

        public int splashH() {
                return splashH;
        }

        public int nameTexX() {
                return nameTexX;
        }

        public int nameTexY() {
                return nameTexY;
        }

        public int nameTexW() {
                return nameTexW;
        }

        public int nameTexH() {
                return nameTexH;
        }

        public List<RaceCustomization> customization() {
                return customization;
        }

        public List<ResourceLocation> startingAbilities() {
                return startingAbilities;
        }

        public List<ResourceLocation> startingItems() {
                return startingItems;
        }

        public @Nullable Passives passives() {
                return passives;
        }

        public List<RaceTrait> traits() {
                return traits;
        }

        public boolean isSpirit() {
                return isSpirit;
        }

        public boolean isTiny() {
                return isTiny;
        }

        public boolean stacksAffectResource() {
                return stacksAffectResource;
        }

        /**
         * The forced gender state for this race.
         */
        public mc.sayda.creraces.engine.GState getGState() {
                return gState;
        }

        /**
         * Static passive traits that don't require abilities/effects
         */
        public static class Passives {
                private final boolean canBreatheUnderwater;
                private final boolean canBreatheOnLand;
                private final boolean burnsInSunlight;
                private final List<String> immuneToDamageTypes;
                private final List<String> immuneToPotionEffects;
                private final boolean nightVision;
                private final boolean waterVision;
                private final boolean lavaVision;
                private final mc.sayda.creraces.engine.ScalingValue fallDamageMultiplier;
                private final boolean canFly;
                private final mc.sayda.creraces.engine.ScalingValue liquidSpeedMultiplier;
                private final boolean unaffectedByWater;
                private final boolean unaffectedByLava;
                private final boolean cannotSprint;
                private final boolean noNaturalRegeneration;
                private final mc.sayda.creraces.engine.ScalingValue regenerationMultiplier;
                private final boolean immuneToKnockback;
                private final mc.sayda.creraces.engine.ScalingValue invulnerabilityTicksMultiplier;
                private final boolean noHunger;
                private final boolean noHungerDrain;
                private final mc.sayda.creraces.engine.ScalingValue fixedHunger;
                private final boolean canEatMeat;
                private final boolean canEatWhenFull;
                private final List<String> hatedByEntities;
                private final List<String> respectedByEntities;
                private final List<String> defendedByEntities;
                private final EntitySpawnData spawnOnDeath;
                private final boolean canCommandSocials;

                public Passives(boolean canBreatheUnderwater, boolean canBreatheOnLand, boolean burnsInSunlight,
                                List<String> immuneToDamageTypes, List<String> immuneToPotionEffects,
                                boolean nightVision,
                                boolean waterVision, boolean lavaVision,
                                mc.sayda.creraces.engine.ScalingValue fallDamageMultiplier,
                                boolean canFly, mc.sayda.creraces.engine.ScalingValue liquidSpeedMultiplier,
                                boolean unaffectedByWater,
                                boolean unaffectedByLava, boolean cannotSprint, boolean noNaturalRegeneration,
                                mc.sayda.creraces.engine.ScalingValue regenerationMultiplier, boolean immuneToKnockback,
                                mc.sayda.creraces.engine.ScalingValue invulnerabilityTicksMultiplier, boolean noHunger,
                                boolean noHungerDrain, mc.sayda.creraces.engine.ScalingValue fixedHunger,
                                boolean canEatMeat,
                                boolean canEatWhenFull, List<String> hatedByEntities, List<String> respectedByEntities,
                                List<String> defendedByEntities, @Nullable EntitySpawnData spawnOnDeath,
                                boolean canCommandSocials) {
                        this.canBreatheUnderwater = canBreatheUnderwater;
                        this.canBreatheOnLand = canBreatheOnLand;
                        this.burnsInSunlight = burnsInSunlight;
                        this.immuneToDamageTypes = immuneToDamageTypes;
                        this.immuneToPotionEffects = immuneToPotionEffects;
                        this.nightVision = nightVision;
                        this.waterVision = waterVision;
                        this.lavaVision = lavaVision;
                        this.fallDamageMultiplier = fallDamageMultiplier;
                        this.canFly = canFly;
                        this.liquidSpeedMultiplier = liquidSpeedMultiplier;
                        this.unaffectedByWater = unaffectedByWater;
                        this.unaffectedByLava = unaffectedByLava;
                        this.cannotSprint = cannotSprint;
                        this.noNaturalRegeneration = noNaturalRegeneration;
                        this.regenerationMultiplier = regenerationMultiplier;
                        this.immuneToKnockback = immuneToKnockback;
                        this.invulnerabilityTicksMultiplier = invulnerabilityTicksMultiplier;
                        this.noHunger = noHunger;
                        this.noHungerDrain = noHungerDrain;
                        this.fixedHunger = fixedHunger;
                        this.canEatMeat = canEatMeat;
                        this.canEatWhenFull = canEatWhenFull;
                        this.hatedByEntities = hatedByEntities;
                        this.respectedByEntities = respectedByEntities;
                        this.defendedByEntities = defendedByEntities;
                        this.spawnOnDeath = spawnOnDeath;
                        this.canCommandSocials = canCommandSocials;
                }

                public boolean canBreatheUnderwater() {
                        return canBreatheUnderwater;
                }

                public boolean canBreatheOnLand() {
                        return canBreatheOnLand;
                }

                public boolean burnsInSunlight() {
                        return burnsInSunlight;
                }

                public List<String> immuneToDamageTypes() {
                        return immuneToDamageTypes;
                }

                public List<String> immuneToPotionEffects() {
                        return immuneToPotionEffects;
                }

                public boolean nightVision() {
                        return nightVision;
                }

                public boolean waterVision() {
                        return waterVision;
                }

                public boolean lavaVision() {
                        return lavaVision;
                }

                public mc.sayda.creraces.engine.ScalingValue fallDamageMultiplier() {
                        return fallDamageMultiplier;
                }

                public boolean canFly() {
                        return canFly;
                }

                public mc.sayda.creraces.engine.ScalingValue liquidSpeedMultiplier() {
                        return liquidSpeedMultiplier;
                }

                public boolean unaffectedByWater() {
                        return unaffectedByWater;
                }

                public boolean unaffectedByLava() {
                        return unaffectedByLava;
                }

                public boolean cannotSprint() {
                        return cannotSprint;
                }

                public boolean noNaturalRegeneration() {
                        return noNaturalRegeneration;
                }

                public mc.sayda.creraces.engine.ScalingValue regenerationMultiplier() {
                        return regenerationMultiplier;
                }

                public boolean immuneToKnockback() {
                        return immuneToKnockback;
                }

                public mc.sayda.creraces.engine.ScalingValue invulnerabilityTicksMultiplier() {
                        return invulnerabilityTicksMultiplier;
                }

                public boolean noHunger() {
                        return noHunger;
                }

                public boolean noHungerDrain() {
                        return noHungerDrain;
                }

                public mc.sayda.creraces.engine.ScalingValue fixedHunger() {
                        return fixedHunger;
                }

                public boolean canEatMeat() {
                        return canEatMeat;
                }

                public boolean canEatWhenFull() {
                        return canEatWhenFull;
                }

                public List<String> hatedByEntities() {
                        return hatedByEntities;
                }

                public List<String> respectedByEntities() {
                        return respectedByEntities;
                }

                public List<String> defendedByEntities() {
                        return defendedByEntities;
                }

                public @Nullable EntitySpawnData spawnOnDeath() {
                        return spawnOnDeath;
                }

                public boolean canCommandSocials() {
                        return canCommandSocials;
                }

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
                                null, // Special
                                false // canCommandSocials
                );
        }

        /**
         * Entity spawn configuration for spawnOnDeath
         */
        public static class EntitySpawnData {
                private final String entityType;
                private final String nbt;
                private final int count;

                public EntitySpawnData(String entityType, String nbt, int count) {
                        this.entityType = entityType;
                        this.nbt = nbt;
                        this.count = count;
                }

                public String entityType() {
                        return entityType;
                }

                public String nbt() {
                        return nbt;
                }

                public int count() {
                        return count;
                }
        }

        public String getTranslationKey() {
                return "race." + id.getNamespace() + "." + id.getPath();
        }
}
