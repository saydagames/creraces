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
    private final mc.sayda.creraces.engine.ScalingValue damage;
    private final mc.sayda.creraces.engine.ScalingValue speed;
    private final mc.sayda.creraces.engine.ScalingValue inaccuracy;

    public LaunchProjectileAction(String projectileType, mc.sayda.creraces.engine.ScalingValue damage,
            mc.sayda.creraces.engine.ScalingValue speed, mc.sayda.creraces.engine.ScalingValue inaccuracy) {
        this.projectileType = projectileType;
        this.damage = damage;
        this.speed = speed;
        this.inaccuracy = inaccuracy;
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        if (player.level() == null)
            return true;

        float dmg = (float) damage.evaluate(player, target);
        float spd = (float) speed.evaluate(player, target);
        float acc = (float) inaccuracy.evaluate(player, target);

        if ("arrow".equals(projectileType) || "minecraft:arrow".equals(projectileType)) {
            Arrow arrow = new Arrow(player.level(), player);
            arrow.setBaseDamage(dmg);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
            player.level().addFreshEntity(arrow);
        } else if ("feather".equals(projectileType) || "minecraft:feather".equals(projectileType)
                || "harpy_feather".equals(projectileType) || "creraces:harpy_feather".equals(projectileType)) {
            FeatherProjectile feather = new FeatherProjectile(player.level(), player);
            feather.setDamage(dmg);

            if ("harpy_feather".equals(projectileType) || "creraces:harpy_feather".equals(projectileType)) {
                feather.setItem(new ItemStack(mc.sayda.creraces.registry.ModItems.HARPY_FEATHER.get()));
            } else {
                feather.setItem(new ItemStack(Items.FEATHER));
            }

            feather.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
            player.level().addFreshEntity(feather);
        } else if (projectileType.contains(":")) {
            // Generic fallback for other entities if we want to support them later
            // For now, let's stick to these
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "launch_projectile"), json -> {
            String type = GsonHelper.getAsString(json, "projectile", "minecraft:arrow");
            mc.sayda.creraces.engine.ScalingValue damage = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "damage", 1.0);
            mc.sayda.creraces.engine.ScalingValue speed = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "speed",
                    1.0);
            mc.sayda.creraces.engine.ScalingValue inaccuracy = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "inaccuracy", 1.0);
            return new LaunchProjectileAction(type, damage, speed, inaccuracy);
        });
    }
}
