package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;
import java.util.List;

public class TntAura extends Module implements Wrapper {
    public TntAura(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(5.5).sliderRange(0, 6).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("trap-delay").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Integer> tntDelay = sgGeneral.add(new IntSetting.Builder().name("tnt-delay").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Boolean> fullTrap = sgGeneral.add(new BoolSetting.Builder().name("full-trap").defaultValue(false).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> silentIgnite = sgGeneral.add(new BoolSetting.Builder().name("silent-ignite").defaultValue(false).build());
    private final Setting<Boolean> updateSlot = sgGeneral.add(new BoolSetting.Builder().name("update-slot").defaultValue(false).visible(silentIgnite::get).build());

    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final Setting<Boolean> pauseOnBurrow = sgMisc.add(new BoolSetting.Builder().name("pause-on-burrow").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgMisc.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgMisc.add(new BoolSetting.Builder().name("pause-on-drink").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgMisc.add(new BoolSetting.Builder().name("pause-on-mine").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(false).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides).build());

    private final List<BlockPos> trapPos = new ArrayList<>();
    private FindItemResult tnt, obsidian, ignite;
    private PlayerEntity target;
    private BlockPos tntPos;
    private int tntTimer;
    private int timer;



    @Override
    public void onActivate() {
        tntTimer = tntDelay.get();
        timer = delay.get();
    }

    @Override
    public void onDeactivate() {
        tntPos = null;
        trapPos.clear();
        obsidian = null;
        ignite = null;
        tnt = null;
        target = null;
    }

    @EventHandler
    private void onTickPos(TickEvent.Post event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());

        if (target != null) {
            if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

            tnt = InvUtils.findInHotbar(Items.TNT);
            obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
            if (!silentIgnite.get()) {
                ignite = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);
            } else {
                ignite = InvUtils.find(Items.FLINT_AND_STEEL);
            }

            if (!tnt.found() || !obsidian.found() || !ignite.found()) return;

            tntPos = target.getBlockPos().up(2);
            fillTrapPos(target);

            if (pauseOnBurrow.get() && bu.getBlock(target.getBlockPos()).equals(Blocks.OBSIDIAN)
                || bu.getBlock(target.getBlockPos()).equals(Blocks.BEDROCK)
                || bu.getBlock(target.getBlockPos()).equals(Blocks.ENDER_CHEST)) return;

            if (timer <= 0 && trapPos.size() > 0) {
                BlockPos blockPos = trapPos.get(trapPos.size() - 1);

                if (BlockUtils.place(blockPos, obsidian, rotate.get(), 50, true)) {
                    trapPos.remove(blockPos);
                }

                timer = delay.get();
            } else {
                timer--;
            }


            if ((bu.isTrapped(target, TrappedMode.Top) && !bu.canPlace(tntPos.up(), false)) && tntTimer <= 0) {
                if (bu.canPlace(tntPos, true)) {
                    if (rotate.get()) {
                        nu.rotate(Rotations.getYaw(tntPos), Rotations.getPitch(tntPos), false, () -> pu.place(tntPos, tnt, false, packet.get(), swing.get()));
                    } else {
                        pu.place(tntPos, tnt, false, packet.get(), swing.get());
                    }
                }
                tntTimer = tntDelay.get();
            } else {
                tntTimer--;
            }

            if (bu.getBlock(tntPos).equals(Blocks.TNT)) {
                if (!silentIgnite.get()) {
                    int prev = mc.player.getInventory().selectedSlot;
                    InvUtils.swap(ignite.slot(), false);
                    pu.interact(tntPos, Hand.MAIN_HAND, rotate.get(), packet.get(), swing.get());
                    InvUtils.swap(prev, false);
                } else {
                    int main = mc.player.getInventory().selectedSlot;
                    int slot = ignite.slot();
                    InvUtils.move().from(slot).to(main);
                    pu.interact(tntPos, Hand.MAIN_HAND, rotate.get(), packet.get(), swing.get());
                    InvUtils.move().from(main).to(slot);
                    if (updateSlot.get()) nu.updateSlot(main);
                }
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get() || target == null || trapPos.isEmpty() || tntPos == null) return;

        for (BlockPos pos : trapPos) {
            boolean isFirst = pos.equals(trapPos.get(trapPos.size() - 1));
            Color side = isFirst ? sideColor.get().a(100) : sideColor.get();
            Color line = isFirst ? lineColor.get().a(100) : lineColor.get();
            event.renderer.box(pos, side, line, shapeMode.get(), 0);
            if (tntPos != null && bu.getBlock(tntPos).equals(Blocks.AIR) || bu.getBlock(tntPos).equals(Blocks.TNT)) {
                event.renderer.box(tntPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    private void fillTrapPos(PlayerEntity target) {
        trapPos.clear();
        BlockPos targetPos = target.getBlockPos();
        add(targetPos.add(0, 3, 0));
        add(targetPos.add(1, 2, 0));
        add(targetPos.add(-1, 2, 0));
        add(targetPos.add(0, 2, 1));
        add(targetPos.add(0, 2, -1));
        if (fullTrap.get()) {
            add(targetPos.add(1, 1, 0));
            add(targetPos.add(-1, 1, 0));
            add(targetPos.add(0, 1, 1));
            add(targetPos.add(0, 1, -1));
        }
    }

    private void add(BlockPos blockPos) {
        if (!trapPos.contains(blockPos) && BlockUtils.canPlace(blockPos)) trapPos.add(blockPos);
    }
}
