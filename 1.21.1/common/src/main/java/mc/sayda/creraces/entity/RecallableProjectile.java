package mc.sayda.creraces.entity;

/** A projectile with its own way of flying back to its owner when recall_projectiles calls it. */
public interface RecallableProjectile {
    void recall();
}
