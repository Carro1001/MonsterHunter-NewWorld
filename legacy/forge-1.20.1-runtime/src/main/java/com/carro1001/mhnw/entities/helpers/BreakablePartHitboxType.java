package com.carro1001.mhnw.entities.helpers;

import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.dertoaster.multihitboxlib.entity.MHLibPartEntity;
import de.dertoaster.multihitboxlib.entity.hitbox.SubPartConfig;
import de.dertoaster.multihitboxlib.entity.hitbox.type.IHitboxType;
import de.dertoaster.multihitboxlib.entity.hitbox.type.implementation.AABBHitboxType;
import de.dertoaster.multihitboxlib.util.UtilityCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class BreakablePartHitboxType extends AABBHitboxType {

    protected final float hp;
    protected final boolean isGoneOnDeath;
    protected final String partTypeName;

    public BreakablePartHitboxType(Vec2 size, Vec3 pos, Vec3 pivot, float hp, boolean isGoneOnDeath, String partType) {
        super(size, pos, pivot);
        this.hp = hp;
        this.isGoneOnDeath = isGoneOnDeath;
        this.partTypeName = partType;
    }

    private IMonsterBreakablePart.PART getPartType(String partType) {
        return switch (partType) {
            case "tail" -> IMonsterBreakablePart.PART.TAIL;
            case "head" -> IMonsterBreakablePart.PART.HEAD;
            case "wing" -> IMonsterBreakablePart.PART.WING;
            case "claw" -> IMonsterBreakablePart.PART.CLAW;
            default -> IMonsterBreakablePart.PART.OTHER;
        };
    }

    public static final Codec<BreakablePartHitboxType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UtilityCodecs.VEC2_CODEC.fieldOf("size").forGetter(obj -> obj.baseSize),
            Vec3.CODEC.fieldOf("position").forGetter(obj -> obj.basePosition),
            Vec3.CODEC.optionalFieldOf("pivot", Vec3.ZERO).forGetter(obj -> obj.pivot),
            Codec.FLOAT.fieldOf("hp").forGetter(obj -> obj.hp),
            Codec.BOOL.fieldOf("will_cut").forGetter(obj -> obj.isGoneOnDeath),
            Codec.STRING.fieldOf("part_type").forGetter(obj -> obj.partTypeName)
    ).apply(instance, BreakablePartHitboxType::new));

    @Override
    public Codec<? extends IHitboxType> getType() {
        return CODEC;
    }

    @Override
    public <T extends Entity> MHLibPartEntity<T> createPartEntity(SubPartConfig config, T parent, int partNumber) {
        return new MonsterBreakablePartEntity<T>(parent, config, EntityDimensions.scalable(this.baseSize.x, this.baseSize.y), this.basePosition, this.pivot, this.hp,this.isGoneOnDeath,getPartType(this.partTypeName));
    }


}
