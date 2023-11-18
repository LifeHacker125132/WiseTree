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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import wise.tree.addon.utils.Wrapper;

public class AutoMinecart extends Module implements Wrapper {
    public AutoMinecart(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(5.5).sliderRange(0, 6).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> trap = sgGeneral.add(new BoolSetting.Builder().name("trap").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private final Setting<RenderMode> render = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").defaultValue(RenderMode.Cube).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(() -> render.get() != RenderMode.None).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Sides).build());

    private FindItemResult m, f, o, p, r;
    private Stage stage;
    private AMMore info;
    private boolean igniting;
    private int delayExplode;



    @Override
    public void onActivate() {
        stage = Stage.PlaceRail;
        info = new AMMore();
        igniting = true;
        delayExplode = delay.get();
        m = null;
        f = null;
        o = null;
        p = null;
        r = null;
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        info.setTarget(TargetUtils.getPlayerTarget(targetRange.get(), priority.get()));
        if (info.target != null) {
            info.setPos(info.target.getBlockPos());

            p = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
            f = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);
            m = InvUtils.findInHotbar(Items.TNT_MINECART);
            o = InvUtils.findInHotbar(Items.OBSIDIAN);
            r = InvUtils.findInHotbar(Items.RAIL);
            if (!m.found() || !f.found() || !o.found() || !p.found() || !r.found()) return;

            if (trap.get() && bu.canPlace(info.pos.up(2), true)) {
                BlockUtils.place(info.pos.up(2), o, rotate.get(), 50, false);
            }

            if (igniting && stage.equals(Stage.PlaceRail) && bu.canPlace(info.pos, false)) {
                delayExplode--;
            } else {
                delayExplode = delay.get();
            }

            switch (stage) {
                case PlaceRail -> {

                    if (delayExplode <= 0) {
                        pu.place(info.pos, r, rotate.get(), packet.get(), swing.get());
                        next(Stage.Minecart);
                    }
                }

                case Minecart -> {
                    igniting = false;
                    int prev = mc.player.getInventory().selectedSlot;
                    InvUtils.swap(m.slot(), false);
                    pu.interact(info.pos, Hand.MAIN_HAND, rotate.get(), packet.get(), swing.get());
                    InvUtils.swap(prev, false);
                    next(Stage.BreakRail);
                }

                case BreakRail -> {
                    int prev = mc.player.getInventory().selectedSlot;
                    InvUtils.swap(p.slot(), false);
                    nu.mine(info.pos, packet.get(), rotate.get(), swing.get());
                    InvUtils.swap(prev, false);
                    next(Stage.Igniting);
                }

                case Igniting -> {
                    if (BlockUtils.place(info.pos, f, rotate.get(), 50, false)) {
                        igniting = true;
                        next(Stage.PlaceRail);
                    }
                }
            }
        }
    }


    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (render.get() == RenderMode.None || info == null || info.target == null || info.pos == null) return;
        switch (render.get()) {
            case Box -> {
                int x = info.pos.getX();
                int y = info.pos.getY();
                int z = info.pos.getZ();
                Box box = new Box(x, y, z, x, y + 0.150, z);
                event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }

            case Cube -> event.renderer.box(info.pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @Override
    public String getInfoString() {
        return stage.name() != null ? "[Stage] -> " + stage.name() : "null";
    }

    private void next(Stage stage) {
        this.stage = stage;
    }

    public enum Stage {
        PlaceRail,
        Minecart,
        BreakRail,
        Igniting
    }

    public enum RenderMode {
        Cube,
        Box,
        None
    }

    private static class AMMore {
        public PlayerEntity target;
        public BlockPos pos;

        public AMMore() {
            target = null;
            pos = null;
        }

        public void setTarget(PlayerEntity target) {
            this.target = target;
        }

        public void setPos(BlockPos pos) {
            this.pos = pos;
        }
    }
}
