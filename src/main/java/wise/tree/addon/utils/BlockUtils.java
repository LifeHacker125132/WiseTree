package wise.tree.addon.utils;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockUtils {
    public BlockState getState(BlockPos pos) {
        return pos == null ? null : mc.world.getBlockState(pos);
    }
    public Block getBlock(BlockPos pos) {
        return pos == null ? null : getState(pos).getBlock();
    }
    public boolean isAir(BlockPos pos) {
        return pos != null && getBlock(pos).equals(Blocks.AIR);
    }
    public float getBlastResistance(BlockPos pos) {return pos == null ? 0 : getBlock(pos).getBlastResistance();}
    public float getHardness(BlockPos pos) {
        return pos == null ? 0 : getBlock(pos).getHardness();
    }
    public VoxelShape getShape(BlockPos pos) {return pos == null ? null : getState(pos).getCollisionShape(mc.world, pos);}
    public Box getBox(BlockPos pos) {
        return pos == null ? null : getShape(pos).getBoundingBox();
    }
    public boolean canBed(BlockPos canPlace, BlockPos replace) {return getBlock(canPlace) instanceof BedBlock && getBlock(replace) instanceof BedBlock || canPlace(canPlace, false) && mc.world.getBlockState(replace).isReplaceable();}
    public boolean canCrystal(BlockPos pos) {return pos != null && (canPlace(pos, true) || getBlock(pos).equals(Blocks.OBSIDIAN) || getBlock(pos).equals(Blocks.BEDROCK));}
    public boolean canPlace(BlockPos pos) { return canPlace(pos, false); }
    public boolean canPlace(BlockPos blockPos, boolean checkEntities) {
        if (blockPos == null) return false;
        if (!World.isValid(blockPos)) return false;
        if (!mc.world.getBlockState(blockPos).isReplaceable()) return false;
        return !checkEntities || mc.world.canPlace(mc.world.getBlockState(blockPos), blockPos, ShapeContext.absent());
    }
    public boolean isBurrowed(PlayerEntity entity) {
        if (entity == null) return false;
        BlockPos pos = entity.getBlockPos();
        return getBlastResistance(pos) >= 600;
    }
    public boolean canTorch(BlockPos pos) {
        return (canPlace(pos, true) || (getBlock(pos).equals(Blocks.REDSTONE_TORCH) || getBlock(pos).equals(Blocks.REDSTONE_WALL_TORCH)));
    }
    public boolean isTrapped(PlayerEntity entity, TrappedMode trappedMode) {
        int isTrapped = 0;

        for (Direction dir : Direction.values()) {
            if (dir.equals(Direction.DOWN) || dir.equals(Direction.UP)) continue;
            BlockPos pos = entity.getBlockPos().up(getUp(trappedMode)).offset(dir);
            if (!canPlace(pos,false)) isTrapped++;
        }

        return isTrapped == 4;
    }
    protected int getUp(TrappedMode trappedMode) {
        return trappedMode == TrappedMode.Top ? 2 : trappedMode == TrappedMode.Face ? 1 : 0;
    }
}
