package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Objects;
import java.util.function.Supplier;

/** C2S: an op edits their own variables, states or attributes from the debug screen. */
public class DebugActionPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "debug_action");

    private final String action;
    private final String key;
    private final String value;

    public DebugActionPacket(String action, String key, String value) {
        this.action = action;
        this.key = key;
        this.value = value;
    }

    public DebugActionPacket(FriendlyByteBuf buf) {
        this.action = Objects.requireNonNull(buf.readUtf(32));
        this.key = Objects.requireNonNull(buf.readUtf(256));
        this.value = Objects.requireNonNull(buf.readUtf(1024));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(Objects.requireNonNull(action));
        buf.writeUtf(Objects.requireNonNull(key));
        buf.writeUtf(Objects.requireNonNull(value));
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();

        context.queue(() -> {
            // Permission check runs on the server thread to avoid TOCTOU race.
            if (!(context.getPlayer() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
                return;
            }

            DataUtils.getVariables(player).ifPresent(vars -> {
                try {
                    switch (action) {
                        case "variable", "state" -> {
                            applyVariable(player, vars, Objects.requireNonNull(key), Objects.requireNonNull(value));
                            CreRaces.LOGGER.debug("Applied debug variable/state: {} = {}", key, value);
                        }
                        case "customization" -> {
                            vars.setCustomization(Objects.requireNonNull(key), Objects.requireNonNull(value));
                            CreRaces.LOGGER.debug("Applied debug customization: {} = {}", key, value);
                        }
                        case "ability_state" -> {
                            vars.setPersistentState(ResourceLocation.parse(Objects.requireNonNull(key)),
                                    Double.parseDouble(Objects.requireNonNull(value)));
                            CreRaces.LOGGER.debug("Applied debug ability state: {} = {}", key, value);
                        }
                        case "cooldown" -> {
                            vars.setCooldown(ResourceLocation.parse(Objects.requireNonNull(key)),
                                    (int) Double.parseDouble(Objects.requireNonNull(value)));
                            CreRaces.LOGGER.debug("Applied debug cooldown: {} = {}", key, value);
                        }
                        case "race" -> {
                            ResourceLocation id = ResourceLocation.parse(Objects.requireNonNull(value));
                            RaceIncidents.transformPlayer(player, id);
                            CreRaces.LOGGER.debug("Applied debug race transformation: {}", value);
                        }
                        case "attribute" -> {
                            applyAttribute(player, key, value);
                            CreRaces.LOGGER.debug("Applied debug attribute: {} = {}", key, value);
                        }
                        case "flag" -> applyFlag(vars, key, value);
                    }
                    // transformPlayer already resyncs everything
                    if (!action.equals("race")) {
                        RaceIncidents.refreshPlayer(player);
                    }
                } catch (Exception e) {
                    CreRaces.LOGGER.error("Failed to apply debug action: {} {} {}", action, key, value, e);
                }
            });
        });
    }

    private void applyVariable(ServerPlayer player, IPlayerVariables vars, String key, String value) {
        if (key.equalsIgnoreCase("race")) {
            RaceIncidents.transformPlayer(player, ResourceLocation.parse(Objects.requireNonNull(value)));
            return;
        }
        // A dimension id rather than a number, so it has to skip the numeric parse below
        if (key.equalsIgnoreCase("returndim")) {
            vars.setReturnDim(value);
            return;
        }

        double val;
        try {
            val = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if (value.equalsIgnoreCase("true"))
                val = 1.0;
            else if (value.equalsIgnoreCase("false"))
                val = 0.0;
            else
                return;
        }

        switch (key.toLowerCase()) {
            case "mana" -> vars.setMana(val);
            case "energy" -> vars.setEnergy(val);
            case "grit" -> vars.setGrit(val);
            case "rage" -> vars.setRage(val);
            case "karma" -> vars.setKarma(val);
            case "ap" -> vars.setAp(val);
            case "ad" -> vars.setAd(val);
            case "ah" -> vars.setAh(val);
            case "cr" -> vars.setCr(val);
            case "coins" -> vars.setCoins(val);
            case "soul" -> vars.setSoul(val);
            case "gstate" -> vars.setGState((int) val);
            case "pocketx" -> vars.setPocketX(val);
            case "pockety" -> vars.setPocketY(val);
            case "pocketz" -> vars.setPocketZ(val);
            case "pocketsize" -> vars.setPocketSize(val);
        }
    }

    private void applyFlag(IPlayerVariables vars, String key, String value) {
        boolean val = value.equalsIgnoreCase("true") || value.equals("1") || value.equals("1.0");
        switch (key) {
            case "isUndead" -> vars.setUndead(val);
            case "isAquatic" -> vars.setAquatic(val);
            case "isSpirit" -> vars.setSpirit(val);
            case "isTiny" -> vars.setTiny(val);
            case "inSpirit" -> vars.setInSpiritRealm(val);
            case "morphed" -> vars.setMorphed(val);
            case "smallBuild" -> vars.setSmallBuild(val);
        }
    }

    private void applyAttribute(ServerPlayer player, String attrId, String value) {
        try {
            double val = Double.parseDouble(value);
            ResourceLocation id = ResourceLocation.parse(Objects.requireNonNull(attrId));
            var attr = ModAttributes.getAttribute(id);
            if (attr != null) {
                AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) {
                    instance.setBaseValue(val);
                }
            }
        } catch (Exception e) {
            CreRaces.LOGGER.error("Failed to apply attribute debug: {} {}", attrId, value, e);
        }
    }
}
