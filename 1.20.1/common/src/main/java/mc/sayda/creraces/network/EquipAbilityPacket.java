package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.race.AttributeIncidents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * C2S: equips an ability into a slot. A null ability, or the ability already in that slot,
 * unequips it instead.
 */
public class EquipAbilityPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "equip_ability");

    private final AbilitySlot slot;
    private final ResourceLocation abilityId;

    public EquipAbilityPacket(AbilitySlot slot, ResourceLocation abilityId) {
        this.slot = slot;
        this.abilityId = abilityId;
    }

    public EquipAbilityPacket(FriendlyByteBuf buf) {
        this.slot = buf.readEnum(AbilitySlot.class);
        if (buf.readBoolean()) {
            this.abilityId = buf.readResourceLocation();
        } else {
            this.abilityId = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(slot);
        if (abilityId != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(abilityId);
        } else {
            buf.writeBoolean(false);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player player = context.getPlayer();
            DataUtils.getVariables(player).ifPresent(vars -> {
                if (abilityId != null) {
                    if (!vars.isAbilityUnlocked(abilityId)) {
                        CreRaces.LOGGER.warn("Player {} tried to equip unowned ability: {}",
                                player.getName().getString(), abilityId);
                        return;
                    }
                    if (AbilityRegistry.get(abilityId) == null) {
                        CreRaces.LOGGER.warn("Player {} tried to equip invalid/unregistered ability: {}",
                                player.getName().getString(), abilityId);
                        return;
                    }
                }

                ResourceLocation currentAbilityId = vars.getAbilityInSlot(slot);
                ResourceLocation abilityToEquip = Objects.equals(currentAbilityId, abilityId) ? null : abilityId;

                // Whatever leaves the slot gets its onDeactivate actions first
                if (currentAbilityId != null) {
                    Ability currentAbility = AbilityRegistry.get(currentAbilityId);
                    if (currentAbility != null && currentAbility.onDeactivate() != null) {
                        for (ActionRegistry.RaceAction action : currentAbility.onDeactivate()) {
                            action.execute(player, null, slot, null);
                        }
                    }
                }

                vars.equipAbility(slot, abilityToEquip);
                // Passive modifiers may have changed with the slot
                if (player instanceof ServerPlayer sp) {
                    AttributeIncidents.eikiJudgment(sp);
                }
                BoundaryHandler.resyncVariables(player, player);
            });
        });
    }
}
