package wise.tree.addon.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public class VecUtils {
    public Vec3d getVec3d(BlockPos pos) {
        return pos == null ? null : new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public Vector3d getVector3d(BlockPos pos) {
        return pos == null ? null : new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
