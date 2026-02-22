package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.entity.FeatherProjectile;
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

    public static void register() {
        ENTITIES.register();
    }
}
