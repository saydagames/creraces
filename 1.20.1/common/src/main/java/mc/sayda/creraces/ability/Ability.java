package mc.sayda.creraces.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/**
 * Represents a race ability.
 * Defined via JSON in data/creraces/abilities/
 */
public record Ability(
        ResourceLocation id,
        Component name,
        Component description,
        AbilityType type,
        ResourceLocation icon,
        int cooldown,
        int cost,
        boolean persistent,
        List<ResourceLocation> allowedRaces,
        List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onActivate) {
    public String getTranslationKey() {
        return "ability." + id.getNamespace() + "." + id.getPath();
    }
}
