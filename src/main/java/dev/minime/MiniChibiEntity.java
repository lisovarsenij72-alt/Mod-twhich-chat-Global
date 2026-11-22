package dev.minime;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** Клиентская сущность-чибик (на сервер не отправляется). */
public class MiniChibiEntity extends PathfinderMob {
    public Identifier texture;   // текстура скина (для гуманоидного чибика)
    public boolean fox;          // true — рисовать лиса

    public MiniChibiEntity(EntityType<? extends MiniChibiEntity> type, Level level) {
        super(type, level);
    }

    @Override public boolean isPushable() { return false; }
}
