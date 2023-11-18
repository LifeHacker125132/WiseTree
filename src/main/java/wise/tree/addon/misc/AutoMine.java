package wise.tree.addon.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import wise.tree.addon.events.StartBreakEvent;
import wise.tree.addon.utils.WTModule;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;
import java.util.List;

public class AutoMine extends WTModule implements Wrapper {
    public AutoMine(Category category, String name) {
        super(category, name);
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<ListMode> listMode = sgGeneral.add(new EnumSetting.Builder<ListMode>().name("list-mode").defaultValue(ListMode.Single).build());
    private final Setting<Mode> mineMode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mine-mode").defaultValue(Mode.Normal).build());
    public final Setting<List<Block>> ignoreBlocks = sgGeneral.add(new BlockListSetting.Builder().name("ignore-blocks").build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> always = sgGeneral.add(new BoolSetting.Builder().name("always-mine").defaultValue(true).visible(() -> listMode.get().equals(ListMode.Single)).build());
    private final Setting<Boolean> pauseOnEat = sgGeneral.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").visible(() -> shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder().name("line-color").visible(() -> shapeMode.get() != ShapeMode.Sides).build());

    private final ArrayList<BlockPos> queue = new ArrayList<>();
    private BlockPos currentPos;
    private int prev;

    @Override
    public void onActivate() {
        queue.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (checkNull()) {
            return;
        }

        setTarget();
        clearQueue();

        if (outOfReach(currentPos)) {
            InvUtils.swap(prev, false);
            if (listMode.get().equals(ListMode.Queue) && !queue.isEmpty()) queue.remove(currentPos);
            currentPos = null;
        }

        if (listMode.get().equals(ListMode.Single) && !always.get()) {
            if (currentPos == null) return;
            if (!canBreak(currentPos)) {
                currentPos = null;
            }
        }

        checkBlock();
        if (!canBreak(currentPos)) return;
        if (currentPos != null) {
            Direction dir = getDirection(currentPos);
            prev = mc.player.getInventory().selectedSlot;
            if (!eat()) InvUtils.swap(InvUtils.findFastestTool(mc.world.getBlockState(currentPos)).slot(), false);
            StartBreakEvent event1 = new StartBreakEvent();
            rotate();
            if (mineMode.get().equals(Mode.Normal)) {
                mc.interactionManager.updateBlockBreakingProgress(currentPos, dir);
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, currentPos, dir, 0));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, currentPos, dir, 0));
            }
            MeteorClient.EVENT_BUS.post(event1);
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            if (!eat()) InvUtils.swap(prev, false);
        }

    }

    private void add(BlockPos pos) {
        queue.add(pos);
    }

    private boolean checkNull() {
        return mc.player == null || mc.world == null || mc.interactionManager == null ||
            ((listMode.get().equals(ListMode.Queue) && (queue.isEmpty() || currentPos == null)) || (listMode.get().equals(ListMode.Single) && currentPos == null));
    }

    private void clearQueue() {
        if (!listMode.get().equals(ListMode.Queue)) return;
        if (queue.isEmpty()) return;
        queue.removeIf(p -> !canBreak(p));
    }

    private void setTarget() {
        if (!listMode.get().equals(ListMode.Queue)) return;
        if (queue.isEmpty()) return;
        if (!canBreak(currentPos)) {
            currentPos = queue.get(0);
            queue.remove(currentPos);
        }
    }

    private void checkBlock() {
        if (currentPos == null) return;
        if (ignoreBlocks.get().contains(bu.getBlock(currentPos))) {
            currentPos = null;
        }
    }

    private void rotate() {
        if (!rotate.get() || currentPos == null) return;
        Rotations.rotate(Rotations.getYaw(currentPos), Rotations.getPitch(currentPos));
    }

    private Direction getDirection(BlockPos pos) {
        return mc.player.getY() > pos.getY() ? Direction.DOWN : Direction.UP;
    }

    private boolean outOfReach(BlockPos pos) {
        return PlayerUtils.distanceTo(pos) > mc.interactionManager.getReachDistance();
    }

    private boolean canBreak(BlockPos pos) {
        return pos != null && BlockUtils.canBreak(pos);
    }

    private boolean eat() {
        return pauseOnEat.get() && pu.onEat();
    }

    @EventHandler
    private void onStartBreakEvent(StartBreakEvent event) {

    }

    @EventHandler
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        BlockPos pos = event.blockPos;
        if (!canBreak(pos) || ignoreBlocks.get().contains(bu.getBlock(pos))) return;
        if (listMode.get().equals(ListMode.Queue)) {
            if (currentPos == null) currentPos = pos;
            if (!queue.contains(pos)) add(pos);
        } else {
            currentPos = pos;
            queue.clear();
        }
        mc.interactionManager.cancelBlockBreaking();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        if ((listMode.get().equals(ListMode.Single) && currentPos == null) || (listMode.get().equals(ListMode.Queue) && queue.isEmpty())) return;
        if (listMode.get().equals(ListMode.Single)) {
            if (!canBreak(currentPos)) return;
            event.renderer.box(currentPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        } else {
            for (BlockPos pos : queue) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    public enum ListMode {
        Queue,
        Single
    }

    public enum Mode {
       // Packet,
        Normal
    }
}
