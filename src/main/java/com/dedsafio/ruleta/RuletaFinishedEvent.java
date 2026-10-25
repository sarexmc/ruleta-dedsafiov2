package com.dedsafio.ruleta;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Event;

/** Se lanza en el bus de Forge cuando la ruleta se detiene. Otros mods/eventos pueden dar premios segun el sector. */
public class RuletaFinishedEvent extends Event {
    private final ServerLevel level;
    private final BlockPos pos;
    private final int sector;

    public RuletaFinishedEvent(ServerLevel level, BlockPos pos, int sector) {
        this.level = level;
        this.pos = pos;
        this.sector = sector;
    }

    public ServerLevel getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    /** 0 verde, 1 cian, 2 azul, 3 morado, 4 rojo, 5 naranja, 6 amarillo. */
    public int getSector() { return sector; }
}
