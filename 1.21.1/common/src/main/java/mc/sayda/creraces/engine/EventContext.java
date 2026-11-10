package mc.sayda.creraces.engine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * What happened to trigger the actions that are running: the damage of an on_hurt, the item of an
 * on_item_pickup, and so on. Traits set it around their conditions and actions; event: values and
 * the damage_type / event_item / event_ability conditions read it. Kept per thread rather than
 * passed through every action, and delayed actions capture it so they still see their event.
 */
public record EventContext(float damage, @Nullable DamageSource damageSource, ItemStack item,
        @Nullable ResourceLocation ability, float fallDistance) {

    public static final EventContext NONE = new EventContext(0, null, ItemStack.EMPTY, null, 0);

    private static final ThreadLocal<EventContext> CURRENT = ThreadLocal.withInitial(() -> NONE);

    public static EventContext current() {
        return CURRENT.get();
    }

    public static EventContext ofDamage(DamageSource source, float amount) {
        return new EventContext(amount, source, ItemStack.EMPTY, null, 0);
    }

    public static EventContext ofItem(ItemStack stack) {
        return new EventContext(0, null, stack, null, 0);
    }

    public static EventContext ofAbility(ResourceLocation ability) {
        return new EventContext(0, null, ItemStack.EMPTY, ability, 0);
    }

    public static EventContext ofFall(float distance) {
        return new EventContext(0, null, ItemStack.EMPTY, null, distance);
    }

    /** Runs {@code body} with this context, restoring the previous one afterwards. */
    public <T> T call(Supplier<T> body) {
        EventContext previous = CURRENT.get();
        CURRENT.set(this);
        try {
            return body.get();
        } finally {
            CURRENT.set(previous);
        }
    }

    public void run(Runnable body) {
        call(() -> {
            body.run();
            return null;
        });
    }
}
