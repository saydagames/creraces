package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.entity.FeatherProjectile;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/**
 * Action that launches a projectile from the player.
 */
public class LaunchProjectileAction implements ActionRegistry.RaceAction {
    private final String projectileType;
    private final float damage;
    private final float speed;
    private final float inaccuracy;

    public LaunchProjectileAction(String projectileType, float damage, float speed, float inaccuracy) {
        this.projectileType = projectileType;
        this.damage = damage;
        this.speed = speed;
        this.inaccuracy = inaccuracy;
    }

    @Override
    public void execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        if (player.level() == null)
            return;

        if ("arrow".equals(projectileType) || "minecraft:arrow".equals(projectileType)) {
            Arrow arrow = new Arrow(player.level(), player);
            arrow.setBaseDamage(damage);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, inaccuracy);
            player.level().addFreshEntity(arrow);
        } else if ("feather".equals(projectileType) || "minecraft:feather".equals(projectileType)
                || "harpy_feather".equals(projectileType) || "creraces:harpy_feather".equals(projectileType)) {
            FeatherProjectile feather = new FeatherProjectile(player.level(), player);
            feather.setDamage(damage);

            if ("harpy_feather".equals(projectileType) || "creraces:harpy_feather".equals(projectileType)) {
                feather.setItem(new ItemStack(mc.sayda.creraces.registry.ModItems.HARPY_FEATHER.get()));
            } else {
                feather.setItem(new ItemStack(Items.FEATHER));
            }

            feather.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, inaccuracy);
            player.level().addFreshEntity(feather);
        } else if (projectileType.contains(":")) {
            // Generic fallback for other entities if we want to support them later
            // For now, let's stick to these
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "launch_projectile"), json -> {
            String type = GsonHelper.getAsString(json, "projectile", "arrow");
            float damage = GsonHelper.getAsFloat(json, "damage", 2.0f);
            float speed = GsonHelper.getAsFloat(json, "speed", 2.5f);
            float inaccuracy = GsonHelper.getAsFloat(json, "inaccuracy", 1.0f);
            return new LaunchProjectileAction(type, damage, speed, inaccuracy);
        });
    }
}
