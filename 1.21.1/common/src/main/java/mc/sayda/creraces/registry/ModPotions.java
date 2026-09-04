package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(CreRaces.MODID, Registries.POTION);

    public static final RegistrySupplier<Potion> REVEALING = POTIONS.register("revealing",
            () -> new Potion("creraces.revealing", new MobEffectInstance(ModMobEffects.REVEALING, 1)));

    public static final RegistrySupplier<Potion> BANISHMENT = POTIONS.register("banishment",
            () -> new Potion("creraces.banishment", new MobEffectInstance(ModMobEffects.BANISHMENT, 1)));

    public static void register() {
        POTIONS.register();
    }
}
