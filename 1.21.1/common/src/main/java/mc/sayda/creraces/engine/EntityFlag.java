package mc.sayda.creraces.engine;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.RaceUtils;
import mc.sayda.creraces.util.SpiritRealmUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * The yes/no switches the flag condition reads and set_flag writes. Players keep them in their race
 * data; any other living entity carries them as a "creraces:&lt;flag&gt;" tag, the tag the spirit
 * handlers already look for on spirit mobs.
 */
public enum EntityFlag {
    IN_SPIRIT_REALM("in_spirit_realm"),
    MINI_BUILD("mini_build"),
    /** Also true for every spirit race, whatever the flag itself says. */
    SPIRIT("spirit"),
    TINY("tiny"),
    /** Read-only here: morphing goes through the morph action. */
    MORPHED("morphed");

    private final String id;

    EntityFlag(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isWritable() {
        return this != MORPHED;
    }

    public boolean get(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return entity.getTags().contains(tag());
        }
        if (this == SPIRIT) {
            return RaceUtils.isSpirit(player);
        }
        return DataUtils.getVariables(player).map(this::read).orElse(false);
    }

    /** Sets the flag and syncs it. Mini build stays off in the dimensions the config blacklists. */
    public void set(LivingEntity entity, boolean value) {
        if (!(entity instanceof Player player)) {
            if (value) {
                entity.addTag(tag());
            } else {
                entity.removeTag(tag());
            }
            return;
        }
        if (this == IN_SPIRIT_REALM) {
            SpiritRealmUtils.setInSpiritRealm(player, value);
            return;
        }
        DataUtils.getVariables(player).ifPresent(vars -> {
            boolean next = value;
            if (this == MINI_BUILD && CreRacesConfig.MINI_BUILD_DIMENSION_BLACKLIST.get()
                    .contains(player.level().dimension().location().toString())) {
                next = false;
            }
            if (read(vars) == next) {
                return;
            }
            switch (this) {
                case MINI_BUILD -> vars.setMiniBuild(next);
                case SPIRIT -> vars.setSpirit(next);
                case TINY -> vars.setTiny(next);
                default -> {
                    return;
                }
            }
            // Other players render mini builds, spirits and tiny players differently, so they need it too.
            BoundaryHandler.resyncForAllTrackers(player);
            BoundaryHandler.resyncVariables(player, player);
        });
    }

    private String tag() {
        return CreRaces.MODID + ":" + id;
    }

    private boolean read(IPlayerVariables vars) {
        return switch (this) {
            case MINI_BUILD -> vars.isMiniBuild();
            case SPIRIT -> vars.isSpirit();
            case TINY -> vars.isTiny();
            case IN_SPIRIT_REALM -> vars.isInSpiritRealm();
            case MORPHED -> vars.isMorphed();
        };
    }

    @Nullable
    public static EntityFlag byId(String id) {
        for (EntityFlag flag : values()) {
            if (flag.id.equals(id)) {
                return flag;
            }
        }
        return null;
    }

    public static String knownIds() {
        StringBuilder ids = new StringBuilder();
        for (EntityFlag flag : values()) {
            ids.append(ids.isEmpty() ? "" : ", ").append(flag.id);
        }
        return ids.toString();
    }
}
