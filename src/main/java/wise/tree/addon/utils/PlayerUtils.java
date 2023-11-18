package wise.tree.addon.utils;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerUtils implements Wrapper {
    public void swingHand(Hand hand, boolean packet) {
        if (packet) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        } else {
            mc.player.swingHand(hand);
        }
    }

    public boolean isHelm(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.getItem() == Items.NETHERITE_HELMET) return true;
        if (itemStack.getItem() == Items.DIAMOND_HELMET) return true;
        if (itemStack.getItem() == Items.GOLDEN_HELMET) return true;
        if (itemStack.getItem() == Items.IRON_HELMET) return true;
        if (itemStack.getItem() == Items.CHAINMAIL_HELMET) return true;
        return itemStack.getItem() == Items.LEATHER_HELMET;
    }

    public boolean isChest(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.getItem() == Items.NETHERITE_CHESTPLATE) return true;
        if (itemStack.getItem() == Items.DIAMOND_CHESTPLATE) return true;
        if (itemStack.getItem() == Items.GOLDEN_CHESTPLATE) return true;
        if (itemStack.getItem() == Items.IRON_CHESTPLATE) return true;
        if (itemStack.getItem() == Items.CHAINMAIL_CHESTPLATE) return true;
        return itemStack.getItem() == Items.LEATHER_CHESTPLATE;
    }

    public boolean isLegs(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.getItem() == Items.NETHERITE_LEGGINGS) return true;
        if (itemStack.getItem() == Items.DIAMOND_LEGGINGS) return true;
        if (itemStack.getItem() == Items.GOLDEN_LEGGINGS) return true;
        if (itemStack.getItem() == Items.IRON_LEGGINGS) return true;
        if (itemStack.getItem() == Items.CHAINMAIL_LEGGINGS) return true;
        return itemStack.getItem() == Items.LEATHER_LEGGINGS;
    }

    public boolean isBoots(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.getItem() == Items.NETHERITE_BOOTS) return true;
        if (itemStack.getItem() == Items.DIAMOND_BOOTS) return true;
        if (itemStack.getItem() == Items.GOLDEN_BOOTS) return true;
        if (itemStack.getItem() == Items.IRON_BOOTS) return true;
        if (itemStack.getItem() == Items.CHAINMAIL_BOOTS) return true;
        return itemStack.getItem() == Items.LEATHER_BOOTS;
    }

    public boolean checkThreshold(ItemStack i, double threshold) {
        return getDamage(i) <= threshold;
    }
    public double getDamage(ItemStack i) {return (((double) (i.getMaxDamage() - i.getDamage()) / i.getMaxDamage()) * 100);}


    public boolean onEat() {
        return isFood() && mc.player.isUsingItem();
    }

    public boolean isFood() {
        return mc.player.getMainHandStack().isFood() || mc.player.getOffHandStack().isFood();
    }

    public void interact(BlockPos pos, Hand hand, boolean rotate, boolean packet, boolean swing) {
        if (pos == null) return;
        if (rotate) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
        BlockHitResult result = new BlockHitResult(vu.getVec3d(pos), Direction.DOWN, pos, packet);
        if (packet) mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand == null ? Hand.MAIN_HAND : hand, result, 0));
        else mc.interactionManager.interactBlock(mc.player, hand == null ? Hand.MAIN_HAND : hand, result);
        if (swing) swingHand(hand == null ? Hand.MAIN_HAND : hand, packet);
    }

    public void place(BlockPos pos, FindItemResult item, boolean rotate, boolean packet, boolean swing) {
        if (pos == null) return;
        Hand hand = item.getHand() != null ? item.getHand() : Hand.MAIN_HAND;
        int prevSlot = mc.player.getInventory().selectedSlot;
        InvUtils.swap(item.slot(), false);
        if (rotate) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
        BlockHitResult result = new BlockHitResult(vu.getVec3d(pos), Direction.DOWN, pos, packet);
        if (packet) mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));
        else mc.interactionManager.interactBlock(mc.player, hand, result);
        if (swing) swingHand(hand, packet);
        InvUtils.swap(prevSlot, false);
    }

    public boolean placed(BlockPos pos, FindItemResult item, boolean rotate, boolean packet, boolean swing) {
        if (pos == null) return false;
        Hand hand = item.getHand() != null ? item.getHand() : Hand.MAIN_HAND;
        int prevSlot = mc.player.getInventory().selectedSlot;
        InvUtils.swap(item.slot(), false);
        if (rotate) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
        BlockHitResult result = new BlockHitResult(vu.getVec3d(pos), Direction.DOWN, pos, packet);
        if (packet) mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));
        else mc.interactionManager.interactBlock(mc.player, hand, result);
        if (swing) swingHand(hand, packet);
        InvUtils.swap(prevSlot, false);
        return true;
    }

    public boolean isWithinRange(BlockPos block, double range) {
        return mc.player.getBlockPos().isWithinDistance(block, range);
    }

    public void hit(Entity entity, boolean rotate, boolean packet, boolean swing) {
        if (rotate) nu.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), false, () -> hit(entity, packet, swing));
        else hit(entity, packet, swing);
    }

    public void hit(Entity entity, boolean packet, boolean swing) {
        if (packet) mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false));
        else mc.interactionManager.attackEntity(mc.player, entity);
        if (swing) swingHand(Hand.MAIN_HAND, packet);
    }


    public boolean hits(Entity entity, boolean packet, boolean swing) {
        if (packet) mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false));
        else mc.interactionManager.attackEntity(mc.player, entity);
        if (swing) swingHand(Hand.MAIN_HAND, packet);
        return true;
    }

    public boolean isElytra(PlayerEntity e) {
        if (e == null) return false;
        return e.getInventory().getArmorStack(2).getItem().equals(Items.ELYTRA) && e.isFallFlying();
    }
}
