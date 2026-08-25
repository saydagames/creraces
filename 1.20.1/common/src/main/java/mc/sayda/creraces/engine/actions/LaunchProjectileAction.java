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

import javax.annotation.Nullable;

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
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        if (player.level() == null || player.level().isClientSide())
            return true;

        float dmg = (float) damage.evaluate(player, target, slot);
        float spd = (float) speed.evaluate(player, target, slot);
        float acc = (float) inaccuracy.evaluate(player, target, slot);

        String type = projectileType.contains(":") ? projectileType : "minecraft:" + projectileType;

        switch (type) {
            case "minecraft:arrow" -> {
                Arrow arrow = new Arrow(player.level(), player);
                arrow.setBaseDamage(dmg);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                player.level().addFreshEntity(arrow);
            }
            case "minecraft:spectral_arrow" -> {
                net.minecraft.world.entity.projectile.SpectralArrow spectral =
                        new net.minecraft.world.entity.projectile.SpectralArrow(player.level(), player);
                spectral.setBaseDamage(dmg);
                spectral.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                player.level().addFreshEntity(spectral);
            }
            case "minecraft:snowball" -> {
                net.minecraft.world.entity.projectile.Snowball snowball =
                        new net.minecraft.world.entity.projectile.Snowball(player.level(), player);
                snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                player.level().addFreshEntity(snowball);
            }
            case "minecraft:egg" -> {
                net.minecraft.world.entity.projectile.ThrownEgg egg =
                        new net.minecraft.world.entity.projectile.ThrownEgg(player.level(), player);
                egg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                player.level().addFreshEntity(egg);
            }
            case "minecraft:ender_pearl" -> {
                net.minecraft.world.entity.projectile.ThrownEnderpearl pearl =
                        new net.minecraft.world.entity.projectile.ThrownEnderpearl(player.level(), player);
                pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                player.level().addFreshEntity(pearl);
            }
            case "minecraft:fireball" -> {
                net.minecraft.world.phys.Vec3 look = player.getLookAngle().scale(spd);
                net.minecraft.world.entity.projectile.LargeFireball fireball =
                        new net.minecraft.world.entity.projectile.LargeFireball(player.level(), player,
                                look.x, look.y, look.z, (int) dmg);
                fireball.setPos(player.getX(), player.getEyeY(), player.getZ());
                player.level().addFreshEntity(fireball);
            }
            case "minecraft:small_fireball" -> {
                net.minecraft.world.phys.Vec3 look = player.getLookAngle().scale(spd);
                net.minecraft.world.entity.projectile.SmallFireball smallFireball =
                        new net.minecraft.world.entity.projectile.SmallFireball(player.level(), player,
                                look.x, look.y, look.z);
                smallFireball.setPos(player.getX(), player.getEyeY(), player.getZ());
                player.level().addFreshEntity(smallFireball);
            }
            default -> {
                ResourceLocation typeLoc = new ResourceLocation(type);
                var entityTypeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(typeLoc);
                if (entityTypeOpt.isPresent()) {
                    net.minecraft.world.entity.Entity entity = entityTypeOpt.get().create(player.level());
                    if (entity instanceof net.minecraft.world.entity.projectile.Projectile proj) {
                        entity.setPos(player.getX(), player.getEyeY(), player.getZ());
                        if (proj instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
                            arrow.setBaseDamage(dmg);
                            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                        } else {
                            proj.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                        }
                        player.level().addFreshEntity(entity);
                    }
                } else {
                    // Not a registered entity type - if it's a registered item, throw it as a
                    // feather-style projectile carrying that item (generalizes the old
                    // harpy_feather/feather special case to any race's item, no Java change needed).
                    net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.getOptional(typeLoc).orElse(null);
                    if (item != null) {
                        FeatherProjectile feather = new FeatherProjectile(player.level(), player);
                        feather.setDamage(dmg);
                        feather.setItem(new ItemStack(item));
                        feather.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spd, acc);
                        player.level().addFreshEntity(feather);
                    }
                }
            }
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
