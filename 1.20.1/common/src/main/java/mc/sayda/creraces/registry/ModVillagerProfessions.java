package mc.sayda.creraces.registry;

import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;

public class ModVillagerProfessions {
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(CreRaces.MODID,
            Registries.VILLAGER_PROFESSION);

    public static final RegistrySupplier<VillagerProfession> GUILD_RECEPTIONIST = PROFESSIONS.register(
            "guild_receptionist",
            () -> new VillagerProfession("guild_receptionist",
                    holder -> holder.is(ModPoiTypes.GUILD_RECEPTIONIST.getId()),
                    holder -> holder.is(ModPoiTypes.GUILD_RECEPTIONIST.getId()),
                    ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    public static void register() {
        PROFESSIONS.register();
    }
}
