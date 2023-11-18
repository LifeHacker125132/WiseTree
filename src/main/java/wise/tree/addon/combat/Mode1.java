package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;
import java.util.List;

public class Mode1 extends Module implements Wrapper {
    public Mode1(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<PlaceMeta> meta = sgGeneral.add(new EnumSetting.Builder<PlaceMeta>().name("place-method").defaultValue(PlaceMeta.Normal).build());
    private final Setting<PlaceMode> mode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>().name("place-mode").defaultValue(PlaceMode.Full).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("place-delay").defaultValue(1).sliderRange(0, 15).max(15).min(0).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").defaultValue(false).build());
    private final Setting<Boolean> onHole = sgGeneral.add(new BoolSetting.Builder().name("only-hole").defaultValue(true).build());
    private final Setting<Boolean> onGround = sgGeneral.add(new BoolSetting.Builder().name("only-ground").defaultValue(true).build());
    private final Setting<Boolean> antiCev = sgGeneral.add(new BoolSetting.Builder().name("anti-cev").defaultValue(false).visible(() -> mode.get() != PlaceMode.Face).build());

    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final Setting<Boolean> toggleMove = sgToggle.add(new BoolSetting.Builder().name("on-move").defaultValue(false).build());
    private final Setting<Boolean> toggleJump = sgToggle.add(new BoolSetting.Builder().name("on-jump").defaultValue(true).build());
    private final Setting<Boolean> toggleYChange = sgToggle.add(new BoolSetting.Builder().name("on-y-change").defaultValue(false).build());
    private final Setting<Boolean> toggleTp = sgToggle.add(new BoolSetting.Builder().name("on-tp").defaultValue(true).build());
    private final Setting<Boolean> toggleLeft = sgToggle.add(new BoolSetting.Builder().name("on-left").defaultValue(true).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides).build());

    private final List<BlockPos> posList = new ArrayList<>();
    private BlockPos startPos;
    private int placeTimer;

    @Override
    public void onActivate() {
        posList.clear();
        placeTimer = 0;
        startPos = mc.player.getBlockPos();
    }

    @Override
    public void onDeactivate() {
        posList.clear();
        startPos = null;
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (toggleMove.get() && mc.player.getBlockPos() != startPos) toggle();
        if (toggleJump.get() && mc.player.input.jumping) toggle();
        if (toggleYChange.get() && mc.player.prevY != mc.player.getY()) toggle();
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found()) toggle();
        if (onGround.get() && !mc.player.isOnGround()) return;
        if (onHole.get() && !bu.isTrapped(mc.player, TrappedMode.Feet)) return;

        fillPosList();
        if (placeTimer >= delay.get()) {
            if (meta.get().equals(PlaceMeta.Normal) && posList.size() > 0) {
                BlockPos blockPos = posList.get(posList.size() - 1);
                if (pu.placed(blockPos, obsidian, rotate.get(), packet.get(), swing.get())) {
                    posList.remove(blockPos);
                }
            } else {
                for (BlockPos pos : posList) {
                    if (bu.canPlace(pos, true)) {
                        pu.place(pos, obsidian, rotate.get(), packet.get(), swing.get());
                    }
                }
            }
            placeTimer = 0;
        } else {
            placeTimer++;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || posList.isEmpty()) return;
        for (BlockPos pos : posList)
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Receive event) {
        if (!toggleTp.get()) return;
        if (event.packet instanceof TeleportConfirmC2SPacket) toggle();
    }

    @EventHandler
    private void onGameLeftEvent(GameLeftEvent event) {
        if (toggleLeft.get()) toggle();
    }

    private void fillPosList() {
        BlockPos mainPos = mc.player.getBlockPos();
        switch (mode.get()) {
            case Top -> add(mainPos.up(2));

            case Face, Full -> {
                if (mode.get() == PlaceMode.Full) add(mainPos.up(2));
                add(mainPos.add(1, 1, 0));
                add(mainPos.add(-1, 1, 0));
                add(mainPos.add(0, 1, 1));
                add(mainPos.add(0, 1, -1));
            }
        }

        if (mode.get() != PlaceMode.Face && antiCev.get()) {
            add(mainPos.up(3));
        }
    }
    @EventHandler

    private void add(BlockPos blockPos) {
        if (!posList.contains(blockPos) && BlockUtils.canPlace(blockPos)) posList.add(blockPos);
    }
}
