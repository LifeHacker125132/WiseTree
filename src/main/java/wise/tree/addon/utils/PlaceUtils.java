package wise.tree.addon.utils;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlaceUtils implements Wrapper {
    private static Direction getDirection() {
        return Direction.DOWN;
    }

    private static Vec3d getVec3d(BlockPos pos) {
        return new Vec3d(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5);
    }

    private static BlockHitResult getHitResult(BlockPos pos) {
        return new BlockHitResult(getVec3d(pos), getDirection(), pos, false);
    }

    private static BlockHitResult getHitResult(BlockPos pos, boolean inside) {
        return new BlockHitResult(getVec3d(pos), getDirection(), pos, inside);
    }

    public static boolean place(BlockPos pos, FindItemResult item) {
        return place(pos, item, true, false, true, false);
    }

    public static boolean place(BlockPos pos, FindItemResult item, boolean rotate) {
        return place(pos, item, rotate, false, true, false);
    }

    public static boolean place(BlockPos pos, FindItemResult item, boolean rotate, boolean packet) {
        return place(pos, item, rotate, packet, true, false);
    }

    public static boolean place(BlockPos pos, FindItemResult item, boolean rotate, boolean packet, boolean swing) {
        return place(pos, item, rotate, packet, swing, false);
    }

    public static boolean place(BlockPos pos, FindItemResult item, boolean rotate, boolean packet, boolean swing, boolean inside) {
        if (pos == null || !item.found()) return false;
        float y = (float) Rotations.getYaw(getVec3d(pos));
        float p = (float) Rotations.getPitch(getVec3d(pos));
        final float prevYaw = mc.player.getYaw();
        final float prevPitch = mc.player.getPitch();
        final int prevSlot = mc.player.getInventory().selectedSlot;
        if (!item.isOffhand()) {
            InvUtils.swap(item.slot(), false);
        }
        if (rotate) {
            if (packet) {
                nu.rotate(y, p, false);
            } else {
                mc.player.setYaw(y);
                mc.player.setPitch(p);
            }
        }
        Hand hand = item.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        BlockHitResult hitResult = getHitResult(pos, inside);
        if (packet) mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
        else mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        if (swing) {
            if (packet) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            else mc.player.swingHand(hand);
        }
        if (rotate) {
            if (packet) {
                nu.rotate(prevYaw, prevPitch, false);
            } else {
                mc.player.setYaw(prevYaw);
                mc.player.setPitch(prevPitch);
            }
        }
        if (!item.isOffhand()) {
            InvUtils.swap(prevSlot, false);
        }
        return true;
    }
}
