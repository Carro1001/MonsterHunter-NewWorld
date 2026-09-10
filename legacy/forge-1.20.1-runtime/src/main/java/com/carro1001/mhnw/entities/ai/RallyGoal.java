package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.BlangongaEntity;
import com.carro1001.mhnw.entities.GreatIzuchiEntity;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.registration.ModEntities;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RallyGoal extends Goal {

    NewWorldMonsterEntity summoner;
    //3 sec anim
    int animTicks = 0;
    int maxTicks = 20*4;

    public RallyGoal(NewWorldMonsterEntity newWorldMonsterEntity){
        summoner = newWorldMonsterEntity;
    }

    @Override
    public boolean canUse() {
        return summoner.isLimpining() && summoner.getRallyState() == NewWorldMonsterEntity.RallyState.READY;
    }

    @Override
    public void start() {
        MHNW.debugLog("RallyGoal: start");
        super.start();
        animTicks = 0;
        summoner.triggerAnim("main_controller","rally");

    }

    @Override
    public void stop() {
        MHNW.debugLog("RallyGoal: stop");
        super.stop();
        summoner.setRallyState(NewWorldMonsterEntity.RallyState.COOL_DOWN);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        return animTicks <= maxTicks;
    }

    @Override
    public void tick() {
        super.tick();
        if(!summoner.level().isClientSide) {
            animTicks++;
            if (animTicks == maxTicks / 2) {
                summonGang(summoner.level(), summoner.position());
            }
        }
    }

    public void summonGang(Level level, Position position){
        NewWorldMonsterEntity summon = null;
        for (int i = 0; i < summoner.getRandom().nextInt(2,6); i++) {
            if(summoner instanceof GreatIzuchiEntity){
                summon = ModEntities.IZUCHI.get().create(level);
            }
            if(summoner instanceof BlangongaEntity){
                summon = ModEntities.BLANGO.get().create(level);
            }
            Vec3 newPos = new Vec3(position.x() ,position.y(), position.z());
            if(summon != null) {
                summon.setPos(newPos);
                newPos = DefaultRandomPos.getPos(summon, 10, 4);
                if (newPos != null) {
                    summon.setPos(newPos);
                    level.addFreshEntity(summon);
                }
            }
        }
    }
}
