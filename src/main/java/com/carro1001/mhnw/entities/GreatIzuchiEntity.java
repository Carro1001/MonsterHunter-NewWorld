package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.RallyGoal;
import com.carro1001.mhnw.entities.ai.SleepGoal;
import com.carro1001.mhnw.entities.helper.MonsterPart;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class GreatIzuchiEntity extends Monster {

    public final double[][] positions = new double[64][3];
    public int posPointer = -1;

    protected MonsterPart[] subEntities;
    private final MonsterPart body;
    private final MonsterPart tail1;
    private final MonsterPart tail2;

    public GreatIzuchiEntity(EntityType<? extends Monster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = GREAT + "_" + IZUCHI;
        this.body = new MonsterPart(this, "body", 1.5F, 3.0F);
        this.tail1 = new MonsterPart(this, "tail", 1.5F, 1.0F);
        this.tail2 = new MonsterPart(this, "tail2", 1.5F, 1.0F);
        this.subEntities = new MonsterPart[]{this.body, this.tail1, this.tail2};
        this.setId(ENTITY_COUNTER.getAndAdd(this.subEntities.length + 1) + 1); // Forge: Fix MC-158205: Make sure part ids are successors of parent mob id

    }
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new RallyGoal(this));
        this.goalSelector.addGoal(10, new SleepGoal(this));
        this.addBehaviourGoals();
    }

    @Override
    public void aiStep() {
        if (this.posPointer < 0) {
            for(int i = 0; i < this.positions.length; ++i) {
                this.positions[i][0] = (double)this.getYRot();
                this.positions[i][1] = this.getY();
            }
        }

        if (++this.posPointer == this.positions.length) {
            this.posPointer = 0;
        }
        this.positions[this.posPointer][0] = (double)this.getYRot();
        this.positions[this.posPointer][1] = this.getY();
        if (this.level().isClientSide) {
            if (this.lerpSteps > 0) {
                double d6 = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
                double d0 = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
                double d1 = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
                double d2 = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
                this.setYRot(this.getYRot() + (float)d2 / (float)this.lerpSteps);
                this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
                --this.lerpSteps;
                this.setPos(d6, d0, d1);
                this.setRot(this.getYRot(), this.getXRot());
            }
        }

        this.yBodyRot = this.getYRot();
        Vec3[] avec3 = new Vec3[this.subEntities.length];

        for(int j = 0; j < this.subEntities.length; ++j) {
            avec3[j] = new Vec3(this.subEntities[j].getX(), this.subEntities[j].getY(), this.subEntities[j].getZ());
        }
        float f12 = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * ((float)Math.PI / 180F);
        float f13 = Mth.cos(f12);
        float f = Mth.sin(f12);
        float f14 = this.getYRot() * ((float)Math.PI / 180F);
        float f1 = Mth.sin(f14);
        float f15 = Mth.cos(f14);
        this.tickPart(this.body, (double)(f1 * 0.5F), 0.0D, (double)(-f15 * 0.5F));
        double[] adouble = this.getLatencyPos(5, 1.0F);
        for(int k = 0; k < 2; ++k) {
            MonsterPart monsterPart = null;
            if (k == 0) {
                monsterPart = this.tail1;
            }

            if (k == 1) {
                monsterPart = this.tail2;
            }


            double[] adouble1 = this.getLatencyPos(12 + k * 2, 1.0F);
            float f17 = this.getYRot() * ((float)Math.PI / 180F) + this.rotWrap(adouble1[0] - adouble[0]) * ((float)Math.PI / 180F);
            float f18 = Mth.sin(f17);
            float f20 = Mth.cos(f17);
            float f21 = 1.5F;
            float f22 = (float)(k + 1) * 2.0F;
            this.tickPart(monsterPart, (double)(-(f1 * 1.5F + f18 * f22) * f13), adouble1[1] - adouble[1] - (double)((f22 + 1.5F) * f) + 0.5D, (double)((f15 * 1.5F + f20 * f22) * f13));
        }

        for(int l = 0; l < this.subEntities.length; ++l) {
            this.subEntities[l].xo = avec3[l].x;
            this.subEntities[l].yo = avec3[l].y;
            this.subEntities[l].zo = avec3[l].z;
            this.subEntities[l].xOld = avec3[l].x;
            this.subEntities[l].yOld = avec3[l].y;
            this.subEntities[l].zOld = avec3[l].z;
        }
    }
    //BB Part Stuff
    @Override
    public void setId(int pId) {
        super.setId(pId);
        for (int i = 0; i < this.subEntities.length; i++) // Forge: Fix MC-158205: Set part ids to successors of parent mob id
            this.subEntities[i].setId(pId + i + 1);
    }
    /**
     * Returns a double[3] array with movement offsets, used to calculate trailing tail/neck positions. [0] = yaw offset,
     * [1] = y offset, [2] = unused, always 0. Parameters: buffer index offset, partial ticks.
     */
    public double[] getLatencyPos(int pBufferIndexOffset, float pPartialTicks) {
        if (this.isDeadOrDying()) {
            pPartialTicks = 0.0F;
        }

        pPartialTicks = 1.0F - pPartialTicks;
        int i = this.posPointer - pBufferIndexOffset & 63;
        int j = this.posPointer - pBufferIndexOffset - 1 & 63;
        double[] adouble = new double[3];
        double d0 = this.positions[i][0];
        double d1 = Mth.wrapDegrees(this.positions[j][0] - d0);
        adouble[0] = d0 + d1 * (double)pPartialTicks;
        d0 = this.positions[i][1];
        d1 = this.positions[j][1] - d0;
        adouble[1] = d0 + d1 * (double)pPartialTicks;
        adouble[2] = Mth.lerp((double)pPartialTicks, this.positions[i][2], this.positions[j][2]);
        return adouble;
    }

    protected void tickPart(MonsterPart pPart, double pOffsetX, double pOffsetY, double pOffsetZ) {
        pPart.setPos(this.getX() + pOffsetX, this.getY() + pOffsetY, this.getZ() + pOffsetZ);
    }
    protected float rotWrap(double pAngle) {
        return (float)Mth.wrapDegrees(pAngle);
    }
    protected float getHeadYOffset() {
        double[] adouble = this.getLatencyPos(5, 1.0F);
        double[] adouble1 = this.getLatencyPos(0, 1.0F);
        return (float)(adouble[1] - adouble1[1]);
    }

    public MonsterPart[] getSubEntities() {
        return this.subEntities;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
        return this.subEntities;
    }

    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        if (true) return; // Forge: Fix MC-158205: Moved into setId()
        MonsterPart[] monsterParts = this.getSubEntities();

        for(int i = 0; i < monsterParts.length; ++i) {
            monsterParts[i].setId(i + pPacket.getId());
        }

    }
}
