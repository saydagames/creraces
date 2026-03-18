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
        private final double index;
        private final int baseAp;
        private final int baseAd;
        private final int baseAh;
        private final int baseCr;
        private final RaceScale scale;
        private final int maxResource;
        private final ResourceType resourceType;
        private final ResourceLocation bgTexture;
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

        private Race(Builder builder) {
                this.id = builder.id;
                this.name = builder.name;
                this.description = builder.description;
                this.icon = builder.icon;
                this.portrait = builder.portrait;
                this.splash = builder.splash;
                this.nameTexture = builder.nameTexture;
                this.parentRace = builder.parentRace;
                this.index = builder.index;
                this.baseAp = builder.baseAp;
                this.baseAd = builder.baseAd;
                this.baseAh = builder.baseAh;
                this.baseCr = builder.baseCr;
                this.scale = builder.scale;
                this.maxResource = builder.maxResource;
                this.resourceType = builder.resourceType;
                this.bgTexture = builder.bgTexture;
                this.difficulty = builder.difficulty;
                this.splashX = builder.splashX;
                this.splashY = builder.splashY;
                this.splashW = builder.splashW;
                this.splashH = builder.splashH;
                this.nameTexX = builder.nameTexX;
                this.nameTexY = builder.nameTexY;
                this.nameTexW = builder.nameTexW;
                this.nameTexH = builder.nameTexH;
                this.customization = builder.customization;
                this.startingAbilities = builder.startingAbilities;
                this.startingItems = builder.startingItems;
                this.passives = builder.passives;
                this.traits = builder.traits;
                this.isSpirit = builder.isSpirit;
                this.isTiny = builder.isTiny;
                this.stacksAffectResource = builder.stacksAffectResource;
                this.gState = builder.gState;
        }

        public static class Builder {
                private final ResourceLocation id;
                private final net.minecraft.network.chat.Component name;
                
                private net.minecraft.network.chat.Component description = net.minecraft.network.chat.Component.empty();
                private ResourceLocation icon = new ResourceLocation("minecraft", "textures/item/barrier.png");
                private ResourceLocation portrait = new ResourceLocation("creraces", "textures/screens/race.png");
                private ResourceLocation splash = new ResourceLocation("creraces", "textures/screens/unknown_splash.png");
                private ResourceLocation nameTexture = null;
                private ResourceLocation parentRace = null;
                private double index = Double.MAX_VALUE;
                private int baseAp = 0, baseAd = 0, baseAh = 0, baseCr = 0;
                private RaceScale scale = null;
                private int maxResource = 100;
                private ResourceType resourceType = ResourceType.NONE;
                private ResourceLocation bgTexture = new ResourceLocation("creraces", "textures/screens/selection_bg.png");
                private int difficulty = 0;
                private int splashX = 15, splashY = -10, splashW = 141, splashH = 199;
                private int nameTexX = 44, nameTexY = -35, nameTexW = 86, nameTexH = 20;
                private List<RaceCustomization> customization = new java.util.ArrayList<>();
                private List<ResourceLocation> startingAbilities = new java.util.ArrayList<>();
                private List<ResourceLocation> startingItems = new java.util.ArrayList<>();
                private Passives passives = null;
                private List<RaceTrait> traits = new java.util.ArrayList<>();
                private boolean isSpirit = false;
                private boolean isTiny = false;
                private boolean stacksAffectResource = false;
                private mc.sayda.creraces.engine.GState gState = mc.sayda.creraces.engine.GState.BOTH;

                public Builder(ResourceLocation id, net.minecraft.network.chat.Component name) {
                        this.id = id;
                        this.name = name;
                }

                public Builder description(net.minecraft.network.chat.Component description) { this.description = description; return this; }
                public Builder icon(ResourceLocation icon) { if (icon != null) this.icon = icon; return this; }
                public Builder portrait(ResourceLocation portrait) { if (portrait != null) this.portrait = portrait; return this; }
                public Builder splash(ResourceLocation splash) { if (splash != null) this.splash = splash; return this; }
                public Builder nameTexture(@Nullable ResourceLocation nameTexture) { this.nameTexture = nameTexture; return this; }
                public Builder parentRace(@Nullable ResourceLocation parentRace) { this.parentRace = parentRace; return this; }
                public Builder index(double index) { this.index = index; return this; }
                
                public Builder stats(int baseAp, int baseAd, int baseAh, int baseCr) {
                        this.baseAp = baseAp;
                        this.baseAd = baseAd;
                        this.baseAh = baseAh;
                        this.baseCr = baseCr;
                        return this;
                }

                public Builder scale(RaceScale scale) { this.scale = scale; return this; }
                
                public Builder resource(ResourceType type, int maxResource) {
                        this.resourceType = type;
                        this.maxResource = maxResource;
                        return this;
                }

                public Builder bgTexture(ResourceLocation bgTexture) { if (bgTexture != null) this.bgTexture = bgTexture; return this; }
                public Builder difficulty(int difficulty) { this.difficulty = difficulty; return this; }

                public Builder splashDimensions(int x, int y, int w, int h) {
                        this.splashX = x;
                        this.splashY = y;
                        this.splashW = w;
                        this.splashH = h;
                        return this;
                }

                public Builder nameBoxDimensions(int x, int y, int w, int h) {
                        this.nameTexX = x;
                        this.nameTexY = y;
                        this.nameTexW = w;
                        this.nameTexH = h;
                        return this;
                }

                public Builder customizations(List<RaceCustomization> customization) { if (customization != null) this.customization = customization; return this; }
                public Builder startingAbilities(List<ResourceLocation> abilities) { if (abilities != null) this.startingAbilities = abilities; return this; }
                public Builder startingItems(List<ResourceLocation> items) { if (items != null) this.startingItems = items; return this; }
                public Builder passives(@Nullable Passives passives) { this.passives = passives; return this; }
                public Builder traits(List<RaceTrait> traits) { if (traits != null) this.traits = traits; return this; }
                
                public Builder isSpirit(boolean isSpirit) { this.isSpirit = isSpirit; return this; }
                public Builder isTiny(boolean isTiny) { this.isTiny = isTiny; return this; }
                public Builder stacksAffectResource(boolean stacksAffectResource) { this.stacksAffectResource = stacksAffectResource; return this; }
                public Builder gState(mc.sayda.creraces.engine.GState gState) { if (gState != null) this.gState = gState; return this; }

                public Race build() {
                        if (this.id == null) throw new IllegalStateException("Race ID cannot be null");
                        if (this.name == null) throw new IllegalStateException("Race Name cannot be null");
                        if (this.scale == null) throw new IllegalStateException("Race Scale cannot be null for race " + this.id);
                        return new Race(this);
                }
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

        public double index() {
                return index;
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
                private final List<String> blockedFoodTypes;
                private final List<String> allowedFoodTypes;
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
                                boolean canFly, mc.sayda.creraces.engine.ScalingValue liquidSpeedMultiplier,
                                boolean unaffectedByWater,
                                boolean unaffectedByLava, boolean cannotSprint, boolean noNaturalRegeneration,
                                mc.sayda.creraces.engine.ScalingValue regenerationMultiplier, boolean immuneToKnockback,
                                mc.sayda.creraces.engine.ScalingValue invulnerabilityTicksMultiplier, boolean noHunger,
                                boolean noHungerDrain, mc.sayda.creraces.engine.ScalingValue fixedHunger,
                                List<String> blockedFoodTypes,
                                List<String> allowedFoodTypes,
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
                        this.blockedFoodTypes = blockedFoodTypes != null ? blockedFoodTypes : new java.util.ArrayList<>();
                        this.allowedFoodTypes = allowedFoodTypes != null ? allowedFoodTypes : new java.util.ArrayList<>();
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

                public List<String> blockedFoodTypes() {
                        return blockedFoodTypes;
                }

                public List<String> allowedFoodTypes() {
                        return allowedFoodTypes;
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
                                false, true, false, new java.util.ArrayList<>(), new java.util.ArrayList<>(), // Breathing (+ effect immunity)
                                false, false, false, // Vision
                                false, // canFly
                                new mc.sayda.creraces.engine.ScalingValue(1.0, null, 0, new java.util.ArrayList<>()), // liquidSpeedMultiplier
                                false, false, false, // Environmental (unaffected by water/lava, cannot sprint)
                                false, // noNaturalRegeneration
                                new mc.sayda.creraces.engine.ScalingValue(1.0, null, 0, new java.util.ArrayList<>()), // regenerationMultiplier
                                false, // immuneToKnockback
                                new mc.sayda.creraces.engine.ScalingValue(1.0, null, 0, new java.util.ArrayList<>()), // invulnerabilityTicksMultiplier
                                false, false, // noHunger, noHungerDrain
                                new mc.sayda.creraces.engine.ScalingValue(0.0, null, 0, new java.util.ArrayList<>()), // fixedHunger
                                new java.util.ArrayList<>(), new java.util.ArrayList<>(), false, // Food (blocked, allowed, canEatWhenFull)
                                new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), // Social (hated, respected, defended)
                                null, // spawnOnDeath
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