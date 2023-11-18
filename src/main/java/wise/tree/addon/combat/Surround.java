package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import wise.tree.addon.utils.PlaceUtils;
import wise.tree.addon.utils.SurrRun;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;

public class Surround extends Module implements Wrapper {
    public Surround(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("place-delay").defaultValue(0).min(0).sliderMax(20).max(20).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").defaultValue(false).build());
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder().name("center").defaultValue(true).build());
    private final Setting<Boolean> choruscentring = sgGeneral.add(new BoolSetting.Builder().name("center-on-chorus").visible(center::get).defaultValue(false).build());
    private final Setting<Boolean> onGround = sgGeneral.add(new BoolSetting.Builder().name("only-ground").defaultValue(true).build());

    private final SettingGroup sgPyramid = settings.createGroup("Pyramid");
    private final Setting<PyramidMode> pyramidMode = sgPyramid.add(new EnumSetting.Builder<PyramidMode>().name("pyramid").defaultValue(PyramidMode.Hold).build());
    private final Setting<Keybind> button = sgPyramid.add(new KeybindSetting.Builder().name("pyramid-force-button").defaultValue(Keybind.none()).visible(() -> pyramidMode.get() == PyramidMode.Hold).build());

    private final SettingGroup sgHeadProtect = settings.createGroup("Head-Protect");
    private final Setting<PyramidMode> headprotectMode = sgHeadProtect.add(new EnumSetting.Builder<PyramidMode>().name("head-protect").defaultValue(PyramidMode.Hold).build());
    private final Setting<Keybind> headprotectButton = sgHeadProtect.add(new KeybindSetting.Builder().name("headprotect-force-button").defaultValue(Keybind.none()).visible(() -> headprotectMode.get() == PyramidMode.Hold).build());

    private final SettingGroup sgAntiFall = settings.createGroup("Anti-Fall");
    private final Setting<Boolean> antiFall = sgAntiFall.add(new BoolSetting.Builder().name("anti-fall").defaultValue(false).build());
    private final Setting<Double> distance = sgAntiFall.add(new DoubleSetting.Builder().name("up-distance").defaultValue(0.1).min(0.001).sliderMax(0.30).visible(antiFall::get).build());

    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final Setting<Boolean> toggleMove = sgToggle.add(new BoolSetting.Builder().name("on-move").defaultValue(false).build());
    private final Setting<Boolean> toggleJump = sgToggle.add(new BoolSetting.Builder().name("on-jump").defaultValue(true).build());
    private final Setting<Boolean> toggleYChange = sgToggle.add(new BoolSetting.Builder().name("on-y-change").defaultValue(false).build());
    private final Setting<Boolean> toggleTp = sgToggle.add(new BoolSetting.Builder().name("on-tp").defaultValue(true).build());
    private final Setting<Boolean> toggleLeft = sgToggle.add(new BoolSetting.Builder().name("on-left").defaultValue(true).build());

    private final SettingGroup sgProtect = settings.createGroup("Protect");
    private final Setting<Boolean> protect = sgProtect.add(new BoolSetting.Builder().name("protect").defaultValue(true).build());
    private final Setting<Boolean> safety = sgProtect.add(new BoolSetting.Builder().name("safety").defaultValue(false).visible(protect::get).build());
    private final Setting<Double> range = sgProtect.add(new DoubleSetting.Builder().name("range").defaultValue(3.4).min(0).sliderMin(0).sliderMax(7).visible(protect::get).build());
    private final Setting<Integer> ticksExisted = sgProtect.add(new IntSetting.Builder().name("ticks-existed").defaultValue(0).min(0).sliderMax(25).visible(protect::get).build());
    private final Setting<Boolean> fast = sgProtect.add(new BoolSetting.Builder().name("fast").defaultValue(true).visible(() -> protect.get() && ticksExisted.get() < 1).build());
    private final Setting<Boolean> onEat = sgProtect.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).visible(protect::get).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<RenderMode> render = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").defaultValue(RenderMode.Better).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(()-> render.get() != RenderMode.None).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> sideColorTwo = sgRender.add(new ColorSetting.Builder().name("side-color-two").visible(() -> render.get() == RenderMode.Better && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Sides).build());
    private final Setting<SettingColor> lineColorTwo = sgRender.add(new ColorSetting.Builder().name("line-color-two").visible(() -> render.get() == RenderMode.Better && shapeMode.get() != ShapeMode.Sides).build());

    private final ArrayList<Vec3i> pos = new ArrayList<>() {{
        add(new Vec3i(0, -1, 0));
        add(new Vec3i(1, 0, 0));
        add(new Vec3i(-1, 0, 0));
        add(new Vec3i(0, 0, 1));
        add(new Vec3i(0, 0, -1));
    }};

    private BlockPos startPos;
    private boolean centered;
    private FindItemResult obsidian;
    private boolean bottomPlace;
    private int placeTimer;



    @Override
    public void onActivate() {
        if (center.get() && !onGround.get())  {
            PlayerUtils.centerPlayer();
            centered = true;
        } else if (center.get() && onGround.get() && !mc.player.isOnGround())
            centered = false;
        else if (center.get() && onGround.get() && mc.player.isOnGround()) {
            PlayerUtils.centerPlayer();
            centered = true;
        }

        placeTimer = 0;
        bottomPlace = false;
        startPos = mc.player.getBlockPos();
    }

    @Override
    public void onDeactivate() {
        startPos = null;
        centered = false;
        placeTimer = 0;
        bottomPlace = false;
    }

    @EventHandler
    private void onBreakBlock(BreakBlockEvent event) {
        if (antiFall.get() && event.blockPos.equals(mc.player.getBlockPos().down())) {
            bottomPlace = true;
            mc.player.setVelocity(0, distance.get(), 0);
        } else {
            bottomPlace = false;
        }
    }
    @EventHandler
    private void onFinishUsingItem2(FinishUsingItemEvent event) throws InterruptedException {
        if (event.itemStack.getItem() instanceof ChorusFruitItem && choruscentring.get())
            SurrRun.taskAfter(PlayerUtils::centerPlayer, 350);
    }

    @EventHandler
    private void onTickPost(TickEvent.Pre event) {
        if (toggleMove.get() && mc.player.getBlockPos() != startPos) toggle();
        if (toggleJump.get() && mc.player.input.jumping) toggle();
        if (toggleYChange.get() && mc.player.prevY != mc.player.getY()) toggle();
        obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found()) toggle();
        if (onGround.get() && (!mc.player.isOnGround() && !bottomPlace)) return;

        for (Vec3i vec3i : pos) {
            BlockPos main = mc.player.getBlockPos();
            BlockPos place = main.add(vec3i);
            if (!centered) {
                PlayerUtils.centerPlayer();
                centered = true;
            }

            if (placeTimer >= delay.get() && bu.canPlace(place, false) && obsidian.found()) {
                PlaceUtils.place(place, obsidian, rotate.get(), packet.get(), swing.get());
                placeTimer = 0;
            } else {
                placeTimer++;
            }
        }

        if (bu.isTrapped(mc.player, TrappedMode.Feet) && pyramidMode.get() != PyramidMode.None) {
            for (BlockPos pos : getPyramidPos(mc.player)) {
                if (!bu.canPlace(pos,true)) continue;
                if (pyramidMode.get().equals(PyramidMode.Hold) && !button.get().isPressed()) continue;
                for (BlockPos p : getPyramidPos(mc.player)) {
                    pu.place(p, obsidian, rotate.get(), packet.get(), swing.get());
                }
            }
        }

        if (bu.isTrapped(mc.player, TrappedMode.Feet) && headprotectMode.get() != PyramidMode.None) {
            for (BlockPos pos : getHeadProtectPos(mc.player)) {
                if (!bu.canPlace(pos, true)) continue;
                if (headprotectMode.get().equals(PyramidMode.Hold) && !headprotectButton.get().isPressed()) continue;
                for (BlockPos p : getHeadProtectPos(mc.player)) {
                    pu.place(p, obsidian, rotate.get(), packet.get(), swing.get());
                }
            }
        }

        if (protect.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (Modules.get().isActive(PistonCrystal.class) && bu.isTrapped(mc.player, TrappedMode.Feet)) continue;
                if (!(entity instanceof EndCrystalEntity)) continue;
                if (PlayerUtils.distanceTo(entity) > range.get()) continue;
                if (entity.age < ticksExisted.get()) continue;
                if (onEat.get() && pu.onEat()) return;
                pu.hit(entity, rotate.get(), packet.get(), swing.get());
                if (fast.get()) entity.discard();
                if (safety.get()) pu.place(entity.getBlockPos(), obsidian, rotate.get(), packet.get(), swing.get());
            }
        }
    }

    private ArrayList<BlockPos> getPyramidPos(PlayerEntity entity) {
        ArrayList<BlockPos> p = new ArrayList<>();
        BlockPos pos = entity.getBlockPos();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == Direction.UP) continue;
            BlockPos offset = pos.offset(dir);
            if (!bu.getBlock(offset).equals(Blocks.BEDROCK)) {
                p.add(pos.offset(dir, 2));
                if (dir == Direction.EAST || dir == Direction.WEST) {
                    p.add(pos.offset(dir).north());
                    p.add(pos.offset(dir).south());
                }
                if (dir == Direction.SOUTH || dir == Direction.NORTH) {
                    p.add(pos.offset(dir).west());
                    p.add(pos.offset(dir).east());
                }
            }
        }

        return p;
    }

    private ArrayList<BlockPos> getHeadProtectPos(PlayerEntity e) {
        ArrayList<BlockPos> p = new ArrayList<>();
        BlockPos pos = e.getBlockPos().up();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == Direction.UP) continue;
            BlockPos offset = pos.offset(dir);
            if (!bu.getBlock(offset).equals(Blocks.BEDROCK))
                p.add(pos.offset(dir).up());
        }

        if (!bu.getBlock(pos.up()).equals(Blocks.BEDROCK)) {
            p.add(pos.up(2));
        }

        return p;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && toggleTp.get()) toggle();
    }

    @EventHandler
    private void onGameLeftEvent(GameLeftEvent event) {
        if (toggleLeft.get()) toggle();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (render.get().equals(RenderMode.None) || (obsidian == null || !obsidian.found() || !obsidian.isHotbar())) return;
        if (onGround.get() && (!mc.player.isOnGround() && !bottomPlace)) return;
        for (Vec3i vec3i : pos) {
            BlockPos main = mc.player.getBlockPos();
            BlockPos place = main.add(vec3i);
            switch (render.get()) {
                case Better -> ru.onRenderGradient(event, place, sideColor.get(), sideColorTwo.get(), lineColor.get(), lineColorTwo.get(), shapeMode.get());
                case Normal -> event.renderer.box(place, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    public enum RenderMode {
        Better,
        Normal,
        None
    }

    public enum PyramidMode {
        None,
        Hold,
        Always
    }
}
