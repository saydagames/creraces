package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.entity.FeatherProjectile;
import mc.sayda.creraces.entity.PoisonEmitterEntity;
import mc.sayda.creraces.entity.PoisonEmitterMobileEntity;
import mc.sayda.creraces.entity.TrollPillarEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

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

        public static final RegistrySupplier<EntityType<mc.sayda.creraces.entity.TornadoEntity>> TORNADO = ENTITIES
                        .register(
                                        "tornado",
                                        () -> EntityType.Builder.<mc.sayda.creraces.entity.TornadoEntity>of(
                                                        mc.sayda.creraces.entity.TornadoEntity::new,
                                                        MobCategory.MISC)
                                                        .sized(1.2F, 3.0F)
                                                        .clientTrackingRange(8)
                                                        .updateInterval(3)
                                                        .build("tornado"));

        public static final RegistrySupplier<EntityType<mc.sayda.creraces.entity.RemainsEntity>> REMAINS = ENTITIES
                        .register(
                                        "remains",
                                        () -> EntityType.Builder.<mc.sayda.creraces.entity.RemainsEntity>of(
                                                        mc.sayda.creraces.entity.RemainsEntity::new,
                                                        MobCategory.MISC)
                                                        .sized(0.6F, 0.4F)
                                                        .clientTrackingRange(8)
                                                        .updateInterval(3)
                                                        .build("remains"));

        public static final RegistrySupplier<EntityType<mc.sayda.creraces.entity.UndeadRemainsEntity>> REMAINS_UNDEAD = ENTITIES
                        .register(
                                        "remains_undead",
                                        () -> EntityType.Builder.<mc.sayda.creraces.entity.UndeadRemainsEntity>of(
                                                        mc.sayda.creraces.entity.UndeadRemainsEntity::new,
                                                        MobCategory.MISC)
                                                        .sized(0.6F, 0.4F)
                                                        .clientTrackingRange(8)
                                                        .updateInterval(3)
                                                        .build("remains_undead"));

        public static void registerAttributes() {
                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(TROLL_PILLAR,
                                TrollPillarEntity::createAttributes);
                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(POISON_EMITTER,
                                PoisonEmitterEntity::createAttributes);
                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(POISON_EMITTER_MOBILE,
                                PoisonEmitterMobileEntity::createAttributes);
                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(TORNADO,
                                mc.sayda.creraces.entity.TornadoEntity::createAttributes);
                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(REMAINS,
                                mc.sayda.creraces.entity.RemainsEntity::createAttributes);
                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(REMAINS_UNDEAD,
                                mc.sayda.creraces.entity.RemainsEntity::createAttributes);
        }

        public static void register() {
                ENTITIES.register();
                registerAttributes();
        }
}
