package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.entity.FeatherProjectile;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Shoots a projectile along the caster's view. Besides the common vanilla projectiles, any
 * projectile entity id works, and any item id is thrown as a feather-style projectile of that item.
 */
public class LaunchProjectileAction implements ActionRegistry.RaceAction {
    private final String projectileType;
    private final ScalingValue damage;
    private final ScalingValue speed;
    private final ScalingValue inaccuracy;

    public LaunchProjectileAction(String projectileType, ScalingValue damage, ScalingValue speed,
            ScalingValue inaccuracy) {
        this.projectileType = projectileType;
        this.damage = damage;
        this.speed = speed;
        this.inaccuracy = inaccuracy;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Level level = player.level();
        if (level.isClientSide()) {
            return true;
        }

        float dmg = (float) damage.evaluate(player, target, slot);
        float spd = (float) speed.evaluate(player, target, slot);
        float acc = (float) inaccuracy.evaluate(player, target, slot);
        String type = projectileType.contains(":") ? projectileType : "minecraft:" + projectileType;

        switch (type) {
            case "minecraft:arrow" -> {
                Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), ItemStack.EMPTY);
                arrow.setBaseDamage(dmg);
                shoot(player, arrow, spd, acc);
            }
            case "minecraft:spectral_arrow" -> {
                SpectralArrow arrow = new SpectralArrow(level, player, new ItemStack(Items.SPECTRAL_ARROW), ItemStack.EMPTY);
                arrow.setBaseDamage(dmg);
                shoot(player, arrow, spd, acc);
            }
            case "minecraft:snowball" -> shoot(player, new Snowball(level, player), spd, acc);
            case "minecraft:egg" -> shoot(player, new ThrownEgg(level, player), spd, acc);
            case "minecraft:ender_pearl" -> shoot(player, new ThrownEnderpearl(level, player), spd, acc);
            case "minecraft:fireball" -> {
                // Fireballs fly along their power vector instead of being shot; damage is the explosion power.
                Vec3 power = player.getLookAngle().scale(spd);
                LargeFireball fireball = new LargeFireball(level, player, power, (int) dmg);
                fireball.setPos(player.getX(), player.getEyeY(), player.getZ());
                level.addFreshEntity(fireball);
            }
            case "minecraft:small_fireball" -> {
                Vec3 power = player.getLookAngle().scale(spd);
                SmallFireball fireball = new SmallFireball(level, player, power);
                fireball.setPos(player.getX(), player.getEyeY(), player.getZ());
                level.addFreshEntity(fireball);
            }
            default -> launchById(player, ResourceLocation.parse(type), dmg, spd, acc);
        }
        return true;
    }

    private static void launchById(Player player, ResourceLocation id, float dmg, float spd, float acc) {
        Level level = player.level();
        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (entityType.isPresent()) {
            Entity entity = entityType.get().create(level);
            if (entity instanceof Projectile projectile) {
                projectile.setPos(player.getX(), player.getEyeY(), player.getZ());
                if (projectile instanceof AbstractArrow arrow) {
                    arrow.setBaseDamage(dmg);
                }
                shoot(player, projectile, spd, acc);
            }
            return;
        }
        // Not an entity: an item id is thrown as a feather-style projectile carrying that item.
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item != null) {
            FeatherProjectile feather = new FeatherProjectile(level, player);
            feather.setDamage(dmg);
            feather.setItem(new ItemStack(item));
            shoot(player, feather, spd, acc);
        }
    }

    private static void shoot(Player player, Projectile projectile, float speed, float inaccuracy) {
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, inaccuracy);
        player.level().addFreshEntity(projectile);
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "launch_projectile"),
                json -> new LaunchProjectileAction(
                        GsonHelper.getAsString(json, "projectile", "minecraft:arrow"),
                        ScalingValue.fromJson(json, "damage", 1.0),
                        ScalingValue.fromJson(json, "speed", 1.0),
                        ScalingValue.fromJson(json, "inaccuracy", 1.0)));
    }
}
