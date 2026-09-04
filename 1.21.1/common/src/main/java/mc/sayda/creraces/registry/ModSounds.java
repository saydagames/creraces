package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(CreRaces.MODID,
            Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> ACTIVATE_1 = register("activate1");
    public static final RegistrySupplier<SoundEvent> ANDROID_CHARGE = register("android_charge");
    public static final RegistrySupplier<SoundEvent> BADAPPLE = register("badapple");
    public static final RegistrySupplier<SoundEvent> BLESSING = register("blessing");
    public static final RegistrySupplier<SoundEvent> BOOST = register("boost");
    public static final RegistrySupplier<SoundEvent> COIN_PICKUP_1 = register("coin_pickup_1");
    public static final RegistrySupplier<SoundEvent> COIN_PICKUP_2 = register("coin_pickup_2");
    public static final RegistrySupplier<SoundEvent> DASH = register("dash");
    public static final RegistrySupplier<SoundEvent> DEATH_CHALLENGE = register("death_challenge");
    public static final RegistrySupplier<SoundEvent> DRAGON_ROAR = register("dragon_roar");
    public static final RegistrySupplier<SoundEvent> DRAGON_ROARING_AND_BREATHE_FIRE = register(
            "dragon_roaring_and_breathe_fire");
    public static final RegistrySupplier<SoundEvent> ENGAGE = register("engage");
    public static final RegistrySupplier<SoundEvent> ENGINE_HEAVY_LOOP = register("engine_heavy_loop");
    public static final RegistrySupplier<SoundEvent> EXPLOSION = register("explosion");
    public static final RegistrySupplier<SoundEvent> FIRE_PROJECTILE = register("fire_projectile");
    public static final RegistrySupplier<SoundEvent> FLIP_PAGE = register("flip_page");
    public static final RegistrySupplier<SoundEvent> GUN_REVERB = register("gun_reverb");
    public static final RegistrySupplier<SoundEvent> HEAL = register("heal");
    public static final RegistrySupplier<SoundEvent> HESA_FREDRIK = register("hesa_fredrik");
    public static final RegistrySupplier<SoundEvent> LAZER_CHARGE = register("lazer_charge");
    public static final RegistrySupplier<SoundEvent> LOFI_WARCRIMES = register("lofi_warcrimes");
    public static final RegistrySupplier<SoundEvent> MAGICAL = register("magical");
    public static final RegistrySupplier<SoundEvent> MASTER_SPARK = register("master_spark");
    public static final RegistrySupplier<SoundEvent> MOAI_SOUND = register("moai_sound");
    public static final RegistrySupplier<SoundEvent> ORC_WARCRY = register("orc_warcry");
    public static final RegistrySupplier<SoundEvent> ORC_WARCRY_SHORT = register("orc_warcry_short");
    public static final RegistrySupplier<SoundEvent> PLEASANT_BOPS = register("pleasant_bops");
    public static final RegistrySupplier<SoundEvent> PROJECTILE_HIT = register("projectile_hit");
    public static final RegistrySupplier<SoundEvent> PROJECTILE_HIT_1 = register("projectile_hit_1");
    public static final RegistrySupplier<SoundEvent> PROJECTILE_HIT_2 = register("projectile_hit_2");
    public static final RegistrySupplier<SoundEvent> RATKIN_A3 = register("ratkin_a3");
    public static final RegistrySupplier<SoundEvent> REDEMPTION = register("redemption");
    public static final RegistrySupplier<SoundEvent> SCREAM_MEME = register("scream_meme");
    public static final RegistrySupplier<SoundEvent> SHOOT_1 = register("shoot1");
    public static final RegistrySupplier<SoundEvent> SLASH = register("slash");
    public static final RegistrySupplier<SoundEvent> SLIME_JUMP = register("slime_jump");
    public static final RegistrySupplier<SoundEvent> SLIME_RUB = register("slime_rub");
    public static final RegistrySupplier<SoundEvent> SUCCUBUS_CHARM = register("succubus_charm");
    public static final RegistrySupplier<SoundEvent> UNDERGROUND_CLUB = register("underground_club");
    public static final RegistrySupplier<SoundEvent> UNLOADING = register("unloading");
    public static final RegistrySupplier<SoundEvent> VERMINWAVE = register("verminwave");
    public static final RegistrySupplier<SoundEvent> WAR_HORN = register("war_horn");
    public static final RegistrySupplier<SoundEvent> WINGS_SOUND = register("wings_sound");
    public static final RegistrySupplier<SoundEvent> WINGS_SOUND_SHORT = register("wings_sound_short");

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, name)));
    }

    public static void register() {
        SOUND_EVENTS.register();
    }
}
