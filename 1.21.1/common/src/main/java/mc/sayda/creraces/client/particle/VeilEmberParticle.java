package mc.sayda.creraces.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class VeilEmberParticle extends TextureSheetParticle {

    private final float baseSize;

    protected VeilEmberParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z);
        this.quadSize *= 0.55f;
        this.baseSize = this.quadSize;
        this.lifetime = 100 + level.random.nextInt(80);
        this.hasPhysics = false;
        this.xd = (level.random.nextDouble() - 0.5) * 0.008;
        this.yd = level.random.nextDouble() * 0.012 + 0.002;
        this.zd = (level.random.nextDouble() - 0.5) * 0.008;
        this.alpha = 0f;
        this.pickSprite(spriteSet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.985;
        this.zd *= 0.985;
        this.yd += 0.0002;

        float progress = (float) this.age / this.lifetime;
        float fade = progress < 0.1f
                ? progress / 0.1f
                : (progress > 0.8f ? (1.0f - progress) / 0.2f : 1.0f);

        // sinusoidal "breath", one full cycle roughly every 5 seconds
        float breath = (float) (Math.sin(this.age * 0.063) * 0.5 + 0.5);

        this.alpha = fade * (0.55f + breath * 0.35f);
        this.quadSize = baseSize * (0.82f + breath * 0.18f);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new VeilEmberParticle(level, x, y, z, spriteSet);
        }
    }
}
