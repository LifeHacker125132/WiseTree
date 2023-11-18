package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import wise.tree.addon.utils.ItemUtils;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;

public class PistonPush extends Module implements Wrapper {
    public PistonPush(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(7).sliderRange(0, 9).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<ItemMode> mode = sgGeneral.add(new EnumSetting.Builder<ItemMode>().name("push-activator").defaultValue(ItemMode.Block).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("place-delay").defaultValue(3).sliderRange(0, 20).build());
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(5.5).sliderRange(0, 6).build());

    private final SettingGroup sgOther = settings.createGroup("Other");
    private final Setting<Boolean> cobwebTrap = sgOther.add(new BoolSetting.Builder().name("cobweb-trap").defaultValue(true).build());
    private final Setting<Boolean> holeFill = sgOther.add(new BoolSetting.Builder().name("hole-fill").defaultValue(true).build());

    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final Setting<Boolean> targetNotFound = sgToggle.add(new BoolSetting.Builder().name("target-not-found").defaultValue(true).build());
    private final Setting<Boolean> itemsNotFound = sgToggle.add(new BoolSetting.Builder().name("items-not-found").defaultValue(true).build());
    private final Setting<Boolean> onDeath = sgToggle.add(new BoolSetting.Builder().name("on-death").defaultValue(true).build());
    private final Setting<Boolean> onLeft = sgToggle.add(new BoolSetting.Builder().name("on-left").defaultValue(true).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(false).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides).build());

    public PlayerEntity target;
    public BlockPos pistonPos;
    public BlockPos activatePos;
    public FindItemResult piston, activator, obby;
    public Direction dir;
    public StageAction stageAction;
    public int timer;



    @Override
    public void onActivate() {
        stageAction = StageAction.Piston;
        target = null;
        timer = delay.get();
        dir = null;
        pistonPos = null;
        activatePos = null;
    }

    @EventHandler
    private void onTickEvent(TickEvent.Post event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());

        if (target == null) {
            if (targetNotFound.get()) toggle();
            return;
        }

        obby = ItemUtils.getObby();
        piston = ItemUtils.getPiston();
        activator = getItem(mode.get());

        if (!find()) {
            if (itemsNotFound.get()) toggle();
            return;
        }

        if (!bu.canPlace(target.getBlockPos().up(2))
            || !bu.canPlace(target.getBlockPos().up())) toggle();

        if (pistonPos == null) pistonPos = getPistonPos(target);
        if (pistonPos == null || dir == null) return;

        if (activatePos == null) activatePos = getActivatePos(dir);
        if (activatePos == null) return;

        if (timer <= 0) {
            switch (stageAction) {
                case Piston -> {
                    nu.rotate(getYaw(dir), 0, false, () -> pu.place(pistonPos, piston, false, false, swing.get()));
                    FindItemResult i = ItemUtils.getCobweb();
                    if (cobwebTrap.get() && i.found() && i.isHotbar()) {
                        BlockUtils.place(pistonPos.offset(getNegative(dir), 2), i, true, 50, swing.get(), false);
                    }
                    next(StageAction.Push);
                }

                case Push -> {
                    if (isFullBlock(mode.get())) {
                        BlockUtils.place(activatePos, activator, true, 50, swing.get(), false);
                    } else {
                        if (bu.canPlace(activatePos.down(), true)) BlockUtils.place(activatePos.down(), obby, true, 50, swing.get(), false);
                        nu.rotate(mc.player.getYaw(), 90, false, () -> pu.place(activatePos, activator, false, false, swing.get()));
                    }
                    if (isClickable(mode.get())) {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(vu.getVec3d(activatePos), Direction.DOWN, activatePos, false));
                    }
                    next(StageAction.After);
                }

                case After -> {
                    if (holeFill.get()) {
                        BlockUtils.place(target.getBlockPos(), obby, false, 50, swing.get(), false);
                    }
                    toggle();
                    next(StageAction.Piston);
                }
            }
            timer = delay.get();
        } else {
            timer--;
        }
    }

    private void next(StageAction stageAction) {
        this.stageAction = stageAction;
    }

    @EventHandler
    private void onPacketEvent(PacketEvent.Receive event) {
        if (event.packet instanceof HealthUpdateS2CPacket packet) {
            if (packet.getHealth() <= 0 && onDeath.get()) toggle();
        }
    }

    @EventHandler
    private void onGameLeftEvent(GameLeftEvent event) {
        if (onLeft.get()) toggle();
    }

    private float getYaw(Direction dir) {
        float y = 0.0f;
        switch (dir) {
            case UP,DOWN,SOUTH -> y = 0.0f;
            case NORTH -> y = 180f;
            case EAST -> y = -90f;
            case WEST -> y = 90f;
        }
        return y;
    }

    private boolean find() {
        return (obby.found() && obby.isHotbar()) && (piston.found() && piston.isHotbar()) && (activator.found() && activator.isHotbar());
    }

    private BlockPos getPistonPos(PlayerEntity target) {
        BlockPos t = target.getBlockPos().up();

        ArrayList<PosContainer> dist = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            dist.add(new PosContainer(getDist(t.offset(dir), dir), t.offset(dir), dir));
        }

        return getBest(dist);
    }

    private BlockPos getBest(ArrayList<PosContainer> arrays) {
        double d1 = arrays.get(0).dist;
        double d2 = arrays.get(1).dist;
        double d3 = arrays.get(2).dist;
        double d4 = arrays.get(3).dist;
        double min = Math.min(Math.min(d1, d2), Math.min(d3, d4));
        double r = placeRange.get();
        if (min == d1 && d1 <= r) {
            dir = arrays.get(0).dir;
            return arrays.get(0).pos;
        } else if (min == d2 && d2 <= r) {
            dir = arrays.get(1).dir;
            return arrays.get(1).pos;
        } else if (min == d3 && d3 <= r) {
            dir = arrays.get(2).dir;
            return arrays.get(2).pos;
        } else if (min == d4 && d4 <= r) {
            dir = arrays.get(3).dir;
            return arrays.get(3).pos;
        }
        return null;
    }

    private double getDist(BlockPos pos, Direction dir) {
        return canPush(pos, dir) ?
            PlayerUtils.distanceTo(pos) :
            PlayerUtils.distanceTo(pos) + 1000;
    }

    private boolean intersectsWithEntity(BlockPos pos) {
        return !EntityUtils.intersectsWithEntity(new Box(pos), entity -> entity instanceof TntEntity || entity instanceof PlayerEntity || entity instanceof EndCrystalEntity);
    }

    private Direction getNegative(Direction dir) {
        return switch (dir) {
            case DOWN -> Direction.UP;
            case UP -> Direction.DOWN;
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case WEST -> Direction.EAST;
            case EAST -> Direction.WEST;
        };
    }

    private boolean canPush(BlockPos pos, Direction dir) {
        return canPiston(pos, dir) && canActivator(pos, dir);
    }

    private boolean canPiston(BlockPos pos, Direction dir) {
        return bu.canPlace(pos, false)
            && bu.canPlace(pos.offset(getNegative(dir), 2))
            && intersectsWithEntity(pos);
    }

    private boolean canActivator(BlockPos pistonPos, Direction dir) {
        return true;
    }

    public boolean canActivator(BlockPos pos) {
        return bu.canPlace(pos) && intersectsWithEntity(pos);
    }

    private BlockPos getActivatePos(Direction dir) {
        BlockPos pos = null;
        switch (dir) {
            case SOUTH, NORTH -> {
                double dist1 = dir == Direction.SOUTH ? getActivatorDist(pistonPos.south()) : getActivatorDist(pistonPos.north());
                double dist2 = getActivatorDist(pistonPos.west());
                double dist3 = getActivatorDist(pistonPos.east());
                double dist4 = getActivatorDist(pistonPos.up());
                double min = Math.min(Math.min(dist1, dist2), Math.min(dist3, dist4));
                double r = placeRange.get();
                if (min == dist1 && dist1 <= r) {
                    pos = dir == Direction.SOUTH ? pistonPos.south() : pistonPos.north();
                } else if (min == dist2 && dist2 <= r) {
                    pos = pistonPos.west();
                } else if (min == dist3 && dist3 <= r) {
                    pos = pistonPos.east();
                } else if (min == dist4 && dist4 <= r) {
                    pos = pistonPos.up();
                }
            }
            case EAST, WEST -> {
                double dist1 = dir == Direction.EAST ? getActivatorDist(pistonPos.east()) : getActivatorDist(pistonPos.west());
                double dist2 = getActivatorDist(pistonPos.south());
                double dist3 = getActivatorDist(pistonPos.north());
                double dist4 = getActivatorDist(pistonPos.up());
                double min = Math.min(Math.min(dist1, dist2), Math.min(dist3, dist4));
                double r = placeRange.get();
                if (min == dist1 && dist1 <= r) {
                    pos = dir == Direction.EAST ? pistonPos.east() : pistonPos.west();
                } else if (min == dist2 && dist2 <= r) {
                    pos = pistonPos.south();
                } else if (min == dist3 && dist3 <= r) {
                    pos = pistonPos.north();
                } else if (min == dist4 && dist4 <= r) {
                    pos = pistonPos.up();
                }
            }
        }
        return pos;
    }

    private double getActivatorDist(BlockPos pos) {
        return bu.canPlace(pos) && intersectsWithEntity(pos) ? PlayerUtils.distanceTo(pos) : PlayerUtils.distanceTo(pos) + 1000;
    }

    private FindItemResult getItem(ItemMode itemMode) {
        return switch (itemMode) {
            case Block -> ItemUtils.getRedstoneBlock();
            case Torch -> ItemUtils.getTorch();
            case Lever -> ItemUtils.getLever();
            case Button -> ItemUtils.getButton();
        };
    }

    private boolean isFullBlock(ItemMode itemMode) {
        return itemMode == ItemMode.Block;
    }

    private boolean isClickable(ItemMode itemMode) {
        return switch (itemMode) {
            case Block, Torch -> false;
            case Lever, Button -> true;
        };
    }

    public enum StageAction {
        Piston,
        Push,
        After
    }

    public enum ItemMode {
        Lever,
        Button,
        Torch,
        Block
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null || !render.get()) return;
        if (pistonPos != null) {
            event.renderer.box(pistonPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    private static class PosContainer {
        public double dist;
        public BlockPos pos;
        public Direction dir;

        public PosContainer(double dist, BlockPos pos, Direction dir) {
            this.dist = dist;
            this.pos = pos;
            this.dir = dir;
        }
    }
}
