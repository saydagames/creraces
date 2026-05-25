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
	public enum RaceState {
		NEW, UNFINISHED, EXPERIMENTAL, FINISHED;

		public static RaceState fromString(String str) {
			if (str == null)
				return FINISHED;
			try {
				return valueOf(str.toUpperCase());
			} catch (Exception e) {
				return FINISHED;
			}
		}
	}
        private final ResourceLocation id;
        private final net.minecraft.network.chat.Component name;
        private final net.minecraft.network.chat.Component description;
        private final ResourceLocation icon;
        private final ResourceLocation portrait;
        private final ResourceLocation splash;
        private final ResourceLocation nameTexture;
        private final List<ResourceLocation> parentRaces;
        private final double index;
        private final int baseAp;
        private final int baseAd;
        private final int baseAh;
        private final int baseCr;
        private final RaceScale scale;
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
        private final boolean isAquatic;
        private final boolean isUndead;
        private final boolean selectable;
        private final mc.sayda.creraces.engine.GState gState;
        private final RaceState state;
        @Nullable private final ResourceLocation selectionDimension;
        @Nullable private final double[] selectionPos;
        @Nullable private final ResourceLocation respawnDimension;
        @Nullable private final double[] respawnPos;

        private Race(Builder builder) {
                this.id = builder.id;
                this.name = builder.name;
                this.description = builder.description;
                this.icon = builder.icon;
                this.portrait = builder.portrait;
                this.splash = builder.splash;
                this.nameTexture = builder.nameTexture;
                this.parentRaces = builder.parentRaces;
                this.index = builder.index;
                this.baseAp = builder.baseAp;
                this.baseAd = builder.baseAd;
                this.baseAh = builder.baseAh;
                this.baseCr = builder.baseCr;
                this.scale = builder.scale;
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
                this.isAquatic = builder.isAquatic;
                this.isUndead = builder.isUndead;
                this.selectable = builder.selectable;
                this.gState = builder.gState;
                this.state = builder.state;
                this.selectionDimension = builder.selectionDimension;
                this.selectionPos = builder.selectionPos;
                this.respawnDimension = builder.respawnDimension;
                this.respawnPos = builder.respawnPos;
        }

        public static class Builder {
                private final ResourceLocation id;
                private final net.minecraft.network.chat.Component name;

                private net.minecraft.network.chat.Component description = net.minecraft.network.chat.Component.empty();
                private ResourceLocation icon = new ResourceLocation("minecraft", "textures/item/barrier.png");
                private ResourceLocation portrait = new ResourceLocation("creraces", "textures/screens/race.png");
                private ResourceLocation splash = new ResourceLocation("creraces",
                                "textures/screens/unknown_splash.png");
                private ResourceLocation nameTexture = null;
                private List<ResourceLocation> parentRaces = new java.util.ArrayList<>();
                private double index = Double.MAX_VALUE;
                private int baseAp = 0, baseAd = 0, baseAh = 0, baseCr = 0;
                private RaceScale scale = null;
                private ResourceType resourceType = ResourceType.NONE;
                private ResourceLocation bgTexture = new ResourceLocation("creraces",
                                "textures/screens/selection_bg.png");
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
                private boolean isAquatic = false;
                private boolean isUndead = false;
                private boolean selectable = true;
                private mc.sayda.creraces.engine.GState gState = mc.sayda.creraces.engine.GState.BOTH;
                private RaceState state = RaceState.FINISHED;
                @Nullable private ResourceLocation selectionDimension = null;
                @Nullable private double[] selectionPos = null;
                @Nullable private ResourceLocation respawnDimension = null;
                @Nullable private double[] respawnPos = null;

                public Builder(ResourceLocation id, net.minecraft.network.chat.Component name) {
                        this.id = id;
                        this.name = name;
                }

                public Builder description(net.minecraft.network.chat.Component description) {
                        this.description = description;
                        return this;
                }

                public Builder icon(ResourceLocation icon) {
                        if (icon != null)
                                this.icon = icon;
                        return this;
                }

                public Builder portrait(ResourceLocation portrait) {
                        if (portrait != null)
                                this.portrait = portrait;
                        return this;
                }

                public Builder splash(ResourceLocation splash) {
                        if (splash != null)
                                this.splash = splash;
                        return this;
                }

                public Builder nameTexture(@Nullable ResourceLocation nameTexture) {
                        this.nameTexture = nameTexture;
                        return this;
                }

                public Builder parentRaces(List<ResourceLocation> parentRaces) {
                        this.parentRaces = parentRaces;
                        return this;
                }

                public Builder index(double index) {
                        this.index = index;
                        return this;
                }

                public Builder stats(int baseAp, int baseAd, int baseAh, int baseCr) {
                        this.baseAp = baseAp;
                        this.baseAd = baseAd;
                        this.baseAh = baseAh;
                        this.baseCr = baseCr;
                        return this;
                }

                public Builder scale(RaceScale scale) {
                        this.scale = scale;
                        return this;
                }

                public Builder resource(ResourceType type) {
                        this.resourceType = type;
                        return this;
                }

                public Builder bgTexture(ResourceLocation bgTexture) {
                        if (bgTexture != null)
                                this.bgTexture = bgTexture;
                        return this;
                }

                public Builder difficulty(int difficulty) {
                        this.difficulty = difficulty;
                        return this;
                }

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

                public Builder customizations(List<RaceCustomization> customization) {
                        if (customization != null)
                                this.customization = customization;
                        return this;
                }

                public Builder startingAbilities(List<ResourceLocation> abilities) {
                        if (abilities != null)
                                this.startingAbilities = abilities;
                        return this;
                }

                public Builder startingItems(List<ResourceLocation> items) {
                        if (items != null)
                                this.startingItems = items;
                        return this;
                }

                public Builder passives(@Nullable Passives passives) {
                        this.passives = passives;
                        return this;
                }

                public Builder traits(List<RaceTrait> traits) {
                        if (traits != null)
                                this.traits = traits;
                        return this;
                }

                public Builder isSpirit(boolean isSpirit) {
                        this.isSpirit = isSpirit;
                        return this;
                }

                public Builder isTiny(boolean isTiny) {
                        this.isTiny = isTiny;
                        return this;
                }

                public Builder isAquatic(boolean isAquatic) {
                        this.isAquatic = isAquatic;
                        return this;
                }

                public Builder isUndead(boolean isUndead) {
                        this.isUndead = isUndead;
                        return this;
                }

                public Builder selectable(boolean selectable) {
                        this.selectable = selectable;
                        return this;
                }


                public Builder gState(mc.sayda.creraces.engine.GState gState) {
                        if (gState != null)
                                this.gState = gState;
                        return this;
                }

                public Builder state(RaceState state) {
                        if (state != null)
                                this.state = state;
                        return this;
                }

                public Builder selectionDimension(@Nullable ResourceLocation selectionDimension) {
                        this.selectionDimension = selectionDimension;
                        return this;
                }

                public Builder selectionPos(@Nullable double[] selectionPos) {
                        this.selectionPos = selectionPos;
                        return this;
                }

                public Builder respawnDimension(@Nullable ResourceLocation respawnDimension) {
                        this.respawnDimension = respawnDimension;
                        return this;
                }

                public Builder respawnPos(@Nullable double[] respawnPos) {
                        this.respawnPos = respawnPos;
                        return this;
                }

                public Race build() {
                        if (this.id == null)
                                throw new IllegalStateException("Race ID cannot be null");
                        if (this.name == null)
                                throw new IllegalStateException("Race Name cannot be null");
                        if (this.scale == null)
                                throw new IllegalStateException("Race Scale cannot be null for race " + this.id);
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

        public List<ResourceLocation> parentRaces() {
                return parentRaces;
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

        public boolean isAquatic() {
                return isAquatic;
        }

        public boolean isUndead() {
                return isUndead;
        }


        public boolean selectable() {
                return selectable;
        }

        /**
         * The forced gender state for this race.
         */
        public mc.sayda.creraces.engine.GState getGState() {
                return gState;
        }

        public RaceState state() {
                return state;
        }

        /**
         * Static passive traits that don't require abilities/effects
         */
        public static class Passives {
                private final boolean canBreatheUnderwater;
                private final int landSuffocationInterval;
                private final int sunlightBurnInterval;
                private final List<String> immuneToDamageTypes;
                private final List<String> immuneToPotionEffects;
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

                public Passives(boolean canBreatheUnderwater, int landSuffocationInterval, int sunlightBurnInterval,
                                List<String> immuneToDamageTypes, List<String> immuneToPotionEffects,
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
                        this.landSuffocationInterval = landSuffocationInterval;
                        this.sunlightBurnInterval = sunlightBurnInterval;
                        this.immuneToDamageTypes = immuneToDamageTypes;
                        this.immuneToPotionEffects = immuneToPotionEffects;
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
                        this.blockedFoodTypes = blockedFoodTypes != null ? blockedFoodTypes
                                        : new java.util.ArrayList<>();
                        this.allowedFoodTypes = allowedFoodTypes != null ? allowedFoodTypes
                                        : new java.util.ArrayList<>();
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

                public int landSuffocationInterval() {
                        return landSuffocationInterval;
                }

                public boolean canBreatheOnLand() {
                        return landSuffocationInterval <= 0;
                }

                public int sunlightBurnInterval() {
                        return sunlightBurnInterval;
                }

                public List<String> immuneToDamageTypes() {
                        return immuneToDamageTypes;
                }

                public List<String> immuneToPotionEffects() {
                        return immuneToPotionEffects;
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
                                false, -1, -1, new java.util.ArrayList<>(), new java.util.ArrayList<>(), // Breathing (+ effect immunity)
                                false, false, // Vision (Water, Lava)
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

        /** Dimension to teleport to on race selection, or null for no dimension swap. */
        public @Nullable ResourceLocation selectionDimension() {
                return selectionDimension;
        }

        /** [x, y, z] destination on race selection, or null for no teleport. */
        public @Nullable double[] selectionPos() {
                return selectionPos;
        }

        /**
         * Dimension to respawn in when no bed/anchor spawn is set.
         * Falls back to world spawn if null.
         */
        public @Nullable ResourceLocation respawnDimension() {
                return respawnDimension;
        }

        /**
         * [x, y, z] position to respawn at when no bed/anchor spawn is set.
         * Only used when this is non-null; dimension defaults to overworld if
         * respawnDimension is null.
         */
        public @Nullable double[] respawnPos() {
                return respawnPos;
        }
}