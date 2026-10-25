package com.dedsafio.ruleta.block;

import com.dedsafio.ruleta.RuletaActivation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Altar visible (pilar de metal gris con lineas de neon cian, ~2 bloques). */
public class RuletaAltarBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 31, 13);

    public RuletaAltarBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return RuletaActivation.tryActivate(level, pos, player, hand);
    }
}
