package mc.sayda.creraces.client.particle;
 
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
 
public class DamageCritParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
 
    protected DamageCritParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        
        this.xd *= 0.1D;
        this.yd *= 0.1D;
        this.zd *= 0.1D;
        this.xd += vx;
        this.yd += vy;
        this.zd += vz;
        
        float colorRand = (float)(Math.random() * 0.3D + 0.6D);
        this.rCol = colorRand;
        this.gCol = colorRand;
        this.bCol = colorRand;
        
        this.quadSize *= 0.75F;
        this.lifetime = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
        this.hasPhysics = true;
        this.setSpriteFromAge(spriteSet);
    }
 
    @Override
    public float getQuadSize(float partialTicks) {
        float f = ((float)this.age + partialTicks) / (float)this.lifetime;
        return this.quadSize * (1.0F - f * f * 0.5F);
    }
 
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.spriteSet);
            this.yd -= 0.04D; // Gravity
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.98D; // Friction
            this.yd *= 0.98D;
            this.zd *= 0.98D;
            if (this.onGround) {
                this.xd *= 0.7D;
                this.zd *= 0.7D;
            }
        }
    }
 
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
 
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
 
        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
 
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double vx, double vy, double vz) {
            return new DamageCritParticle(world, x, y, z, vx, vy, vz, this.spriteSet);
        }
    }
}
