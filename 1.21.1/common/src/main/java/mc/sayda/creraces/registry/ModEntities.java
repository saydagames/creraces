package mc.sayda.creraces.registry;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.entity.DryadBoatEntity;
import mc.sayda.creraces.entity.DryadChestBoatEntity;
import mc.sayda.creraces.entity.FeatherProjectile;
import mc.sayda.creraces.entity.PoisonEmitterEntity;
import mc.sayda.creraces.entity.PoisonEmitterMobileEntity;
import mc.sayda.creraces.entity.RemainsEntity;
import mc.sayda.creraces.entity.TornadoEntity;
import mc.sayda.creraces.entity.TrollPillarEntity;
import mc.sayda.creraces.entity.UndeadRemainsEntity;
import mc.sayda.creraces.entity.VeilWillowBoatEntity;
import mc.sayda.creraces.entity.FloatingMoteEntity;
import mc.sayda.creraces.entity.VeilWillowChestBoatEntity;
import mc.sayda.creraces.mixin.SpawnPlacementsAccessor;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.Heightmap;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(CreRaces.MODID,
            Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<FeatherProjectile>> FEATHER_PROJECTILE = ENTITIES.register(
            "feather_projectile",
            () -> EntityType.Builder.<FeatherProjectile>of(FeatherProjectile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("feather_projectile"));

    public static final RegistrySupplier<EntityType<TrollPillarEntity>> TROLL_PILLAR = ENTITIES.register(
            "troll_pillar",
            () -> EntityType.Builder.<TrollPillarEntity>of(TrollPillarEntity::new, MobCategory.MISC)
                    .sized(0.8F, 2.0F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("troll_pillar"));

    public static final RegistrySupplier<EntityType<PoisonEmitterEntity>> POISON_EMITTER = ENTITIES.register(
            "poison_emitter",
            () -> EntityType.Builder.<PoisonEmitterEntity>of(PoisonEmitterEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("poison_emitter"));

    public static final RegistrySupplier<EntityType<PoisonEmitterMobileEntity>> POISON_EMITTER_MOBILE = ENTITIES
            .register(
                    "poison_emitter_mobile",
                    () -> EntityType.Builder
                            .<PoisonEmitterMobileEntity>of(PoisonEmitterMobileEntity::new,
                                    MobCategory.MISC)
                            .sized(0.8F, 1.2F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("poison_emitter_mobile"));

    public static final RegistrySupplier<EntityType<TornadoEntity>> TORNADO = ENTITIES
            .register(
                    "tornado",
                    () -> EntityType.Builder.<TornadoEntity>of(
                            TornadoEntity::new,
                            MobCategory.MISC)
                            .sized(1.2F, 3.0F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("tornado"));

    public static final RegistrySupplier<EntityType<RemainsEntity>> REMAINS = ENTITIES
            .register(
                    "remains",
                    () -> EntityType.Builder.<RemainsEntity>of(
                            RemainsEntity::new,
                            MobCategory.MISC)
                            .sized(0.6F, 0.4F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("remains"));

    public static final RegistrySupplier<EntityType<UndeadRemainsEntity>> REMAINS_UNDEAD = ENTITIES
            .register(
                    "remains_undead",
                    () -> EntityType.Builder.<UndeadRemainsEntity>of(
                            UndeadRemainsEntity::new,
                            MobCategory.MISC)
                            .sized(0.6F, 0.4F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("remains_undead"));

    public static final RegistrySupplier<EntityType<DryadBoatEntity>> DRYAD_BOAT = ENTITIES.register(
            "dryad_boat",
            () -> EntityType.Builder.<DryadBoatEntity>of(DryadBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("dryad_boat"));

    public static final RegistrySupplier<EntityType<DryadChestBoatEntity>> DRYAD_CHEST_BOAT = ENTITIES.register(
            "dryad_chest_boat",
            () -> EntityType.Builder.<DryadChestBoatEntity>of(DryadChestBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("dryad_chest_boat"));

    public static final RegistrySupplier<EntityType<VeilWillowBoatEntity>> VEIL_WILLOW_BOAT = ENTITIES.register(
            "veil_willow_boat",
            () -> EntityType.Builder.<VeilWillowBoatEntity>of(VeilWillowBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("veil_willow_boat"));

    public static final RegistrySupplier<EntityType<FloatingMoteEntity>> FLOATING_MOTE = ENTITIES.register(
            "floating_mote",
            () -> EntityType.Builder.<FloatingMoteEntity>of(FloatingMoteEntity::new, MobCategory.AMBIENT)
                    .sized(0.3F, 0.3F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("floating_mote"));

    public static final RegistrySupplier<EntityType<VeilWillowChestBoatEntity>> VEIL_WILLOW_CHEST_BOAT = ENTITIES
            .register(
                    "veil_willow_chest_boat",
                    () -> EntityType.Builder
                            .<VeilWillowChestBoatEntity>of(VeilWillowChestBoatEntity::new,
                                    MobCategory.MISC)
                            .sized(1.375F, 0.5625F)
                            .clientTrackingRange(10)
                            .build("veil_willow_chest_boat"));

    public static void registerAttributes() {
        EntityAttributeRegistry.register(TROLL_PILLAR,
                TrollPillarEntity::createAttributes);
        EntityAttributeRegistry.register(POISON_EMITTER,
                PoisonEmitterEntity::createAttributes);
        EntityAttributeRegistry.register(POISON_EMITTER_MOBILE,
                PoisonEmitterMobileEntity::createAttributes);
        EntityAttributeRegistry.register(TORNADO,
                TornadoEntity::createAttributes);
        EntityAttributeRegistry.register(REMAINS,
                RemainsEntity::createAttributes);
        EntityAttributeRegistry.register(REMAINS_UNDEAD,
                RemainsEntity::createAttributes);
        EntityAttributeRegistry.register(FLOATING_MOTE,
                FloatingMoteEntity::createAttributes);
    }

    /**
     * FLOATING_MOTE spawns naturally (veilwood_forest's ambient spawner list), so it needs a spawn
     * placement registration like any vanilla mob; NeoForge logs an error on every server start
     * without one. Goes through SpawnPlacementsAccessor rather than an access widener, see
     * AxeItemAccessor's use in CreRaces for why. CreRaces calls this from the SETUP event because
     * FLOATING_MOTE can't be resolved yet while register() runs.
     */
    public static void registerSpawnPlacements() {
        SpawnPlacementsAccessor.creraces$callRegister(FLOATING_MOTE.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> true);
    }

    public static void register() {
        ENTITIES.register();
        registerAttributes();
    }
}
