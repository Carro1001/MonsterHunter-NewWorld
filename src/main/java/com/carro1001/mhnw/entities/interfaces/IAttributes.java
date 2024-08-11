package com.carro1001.mhnw.entities.interfaces;

import java.util.List;

public interface IAttributes {

    enum Elements{
        FIRE,WATER,THUNDER,ICE,DRAGON,NONE
    }
    enum Blights{
        POISON,SLEEP,STAGGER,NONE
    }
    List<Elements> getMonsterWeakness();
    List<Elements> getMonsterAttackElements();

    List<Blights> getMonsterBlightWeakness();
    List<Blights> getMonsterBlightResistance();
    List<Blights> getMonsterPossibleAttackingBlights();

}
