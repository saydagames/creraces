package mc.sayda.creraces.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class SpiritCompassItem extends Item {

    private static final ResourceLocation VEILWOOD =
            ResourceLocation.fromNamespaceAndPath("creraces", "veilwood_forest");

    public SpiritCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer)) return InteractionResultHolder.pass(stack);

        ServerLevel serverLevel = (ServerLevel) level;
        Pair<BlockPos, Holder<Biome>> result = serverLevel.findClosestBiome3d(
                holder -> holder.is(VEILWOOD),
                player.blockPosition(),
                6400, 32, 64
        );

        if (result != null) {
            BlockPos biomePos = result.getFirst();
            mc.sayda.creraces.util.ItemNbt.mutate(stack, tag -> {
                tag.putInt("TargetX", biomePos.getX());
                tag.putInt("TargetZ", biomePos.getZ());
                tag.putBoolean("HasTarget", true);
            });
            player.displayClientMessage(
                    Component.literal("Tracking: " + biomePos.getX() + " / " + biomePos.getY() + " / " + biomePos.getZ()),
                    true);
        } else {
            mc.sayda.creraces.util.ItemNbt.mutate(stack, tag -> tag.putBoolean("HasTarget", false));
            player.displayClientMessage(Component.literal("No veilwood forest found nearby."), true);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return mc.sayda.creraces.util.ItemNbt.get(stack).getBoolean("HasTarget");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = mc.sayda.creraces.util.ItemNbt.get(stack);
        if (tag.getBoolean("HasTarget")) {
            tooltip.add(Component.translatable("item.creraces.spirit_compass.tracking",
                    tag.getInt("TargetX"), tag.getInt("TargetZ"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
