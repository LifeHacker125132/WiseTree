package wise.tree.addon.utils;

import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PacketUtils implements Wrapper {
    private boolean swing;

    public void rotate(double yaw, double pitch, boolean onGround) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround((float)yaw, (float)pitch, onGround));
    }

    public void rotate(float yaw, float pitch, boolean onGround) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround));
    }

    public void rotate(double yaw, double pitch, boolean onGround, Runnable runnable) {
        rotate(yaw, pitch, onGround);
        runnable.run();
    }

    public void rotate(float yaw, float pitch, boolean onGround, Runnable runnable) {
        rotate(yaw, pitch, onGround);
        runnable.run();
    }

    public void updateSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public void mine(BlockPos pos, boolean packet, boolean rotate, boolean swing) {
        if (rotate) {
            rotate((float)Rotations.getYaw(pos), (float)Rotations.getPitch(pos), false, () -> {
                if (packet) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
                }
                else BlockUtils.breakBlock(pos, swing);
            });
        } else {
            if (packet) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
            }
            else BlockUtils.breakBlock(pos, swing);
        }
        if (swing) pu.swingHand(Hand.MAIN_HAND, packet);
    }
}
