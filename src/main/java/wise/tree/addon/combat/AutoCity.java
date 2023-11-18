package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import wise.tree.addon.utils.Wrapper;

public class AutoCity extends Module implements Wrapper {
    public AutoCity(Category category, String name) {
        super(category, name, "");
        instance = this;
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(6).min(0).sliderMax(7).build());
    private final Setting<Double> miningRange = sgGeneral.add(new DoubleSetting.Builder().name("mining-range").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<SwapMode> swapMode = sgGeneral.add(new EnumSetting.Builder<SwapMode>().name("swap-mode").defaultValue(SwapMode.Silent).build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder().name("swap-back").defaultValue(true).visible(()-> swapMode.get().equals(SwapMode.Normal)).build());
    private final Setting<Boolean> clientMine = sgGeneral.add(new BoolSetting.Builder().name("client-mine").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder().name("self-toggle").defaultValue(true).build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(true).build());

    private final SettingGroup sgSupport = settings.createGroup("Support");
    private final Setting<Boolean> support = sgSupport.add(new BoolSetting.Builder().name("support").defaultValue(true).build());
    private final Setting<Boolean> crystal = sgSupport.add(new BoolSetting.Builder().name("crystal").defaultValue(false).visible(support::get).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private final Setting<RenderMode> render = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").defaultValue(RenderMode.Box).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(() -> render.get() != RenderMode.None).build());
    private final Setting<Double> width = sgRender.add(new DoubleSetting.Builder().name("width").defaultValue(0.8).min(0).sliderMax(1.5).visible(() -> render.get() == RenderMode.Box).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").defaultValue(0.8).min(0).sliderMax(1.5).visible(() -> render.get() == RenderMode.Box).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Sides).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Lines).build());

    private final SettingGroup sgMisc = settings.createGroup("Render Misc");
    private final Setting<Boolean> renderProgress = sgMisc.add(new BoolSetting.Builder().name("progress").defaultValue(true).build());
    private final Setting<Double> scaleProgress = sgMisc.add(new DoubleSetting.Builder().name("progress-scale").defaultValue(1.5).sliderRange(0.01, 3).visible(renderProgress::get).build());
    private final Setting<SettingColor> progressColor = sgMisc.add(new ColorSetting.Builder().name("progress-color").visible(renderProgress::get).build());
    private final Setting<Double> ProgressYControl = sgMisc.add(new DoubleSetting.Builder().name("progress-y").defaultValue(0.0).sliderRange(0, 1).visible(renderProgress::get).build());
    private final Setting<Boolean> renderName = sgMisc.add(new BoolSetting.Builder().name("name").defaultValue(true).build());
    private final Setting<Double> scaleName = sgMisc.add(new DoubleSetting.Builder().name("name-scale").defaultValue(1.5).sliderRange(0.01, 3).visible(renderName::get).build());
    private final Setting<SettingColor> nameColor = sgMisc.add(new ColorSetting.Builder().name("name-color").visible(renderName::get).build());
    private final Setting<Double> NameYControl = sgMisc.add(new DoubleSetting.Builder().name("name-y").defaultValue(0.0).sliderRange(0, 1).visible(renderName::get).build());

    public static AutoCity instance;
    private FindItemResult pickaxe, obsidian, endCrystal;
    private BlockPos targetBlock;
    private PlayerEntity target;
    private boolean sentMessage;
    private boolean mining = false;
    private int prevSlot;
    private int timer;

    @Override
    public void onActivate() {
        timer = 0;
        prevSlot = -1;
    }

    @Override
    public void onDeactivate() {
        if (swapBack.get() || swapMode.get().equals(SwapMode.Silent)) InvUtils.swap(prevSlot, false);
        if (!clientMine.get() && targetBlock != null)
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, targetBlock, Direction.DOWN));
        target = null;
        targetBlock = null;
        pickaxe = null;
        mining = false;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        timer++;
        pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE);
        obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        endCrystal = InvUtils.findInHotbar(Items.END_CRYSTAL);

        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            PlayerEntity search = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
            if (search != target) sentMessage = false;
            target = search;
        }

        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = null;
            targetBlock = null;
            if (selfToggle.get()) toggle();
            return;
        }

        if (target != null && !mining) targetBlock = EntityUtils.getCityBlock(target);

        if (targetBlock == null) {
            if (selfToggle.get()) {
                if (debug.get()) warning("No target block found... disabling.");
                toggle();
            }
            target = null;
            return;
        }

        if (PlayerUtils.distanceTo(targetBlock) > miningRange.get()) {
            if (debug.get()) warning("Target block out of reach... disabling.");
            toggle();
            return;
        }

        if (!pickaxe.found()) {
            if (selfToggle.get()) {
                if (debug.get()) warning("No pickaxe found... disabling.");
                toggle();
            }
            return;
        }

        if (!sentMessage) {
            if (debug.get()) info("Attempting to city %s.", target.getEntityName());
            sentMessage = true;
        }

        if (prevSlot == -1) prevSlot = mc.player.getInventory().selectedSlot;
        if (swapMode.get().equals(SwapMode.Normal)) {
            InvUtils.swap(pickaxe.slot(), false);
        } else {
            InvUtils.swap(pickaxe.slot(), false);
            InvUtils.swapBack();
        }
        if (timer>=43) InvUtils.swap(pickaxe.slot(), false);
        if (timer<45) nu.mine(targetBlock, !clientMine.get(), rotate.get(), swing.get());
        if (support.get() && obsidian.found() && bu.getBlastResistance(targetBlock.down()) < 600) BlockUtils.place(targetBlock.down(), obsidian, rotate.get(), 50, swing.get(), false, true);
        if (crystal.get() && timer == 42) crystalSupport(target);
        mining = true;

        if (selfToggle.get() && timer >= 45) {
            InvUtils.swapBack();
            if (debug.get()) info("Finish!");
            prevSlot = -1;
            toggle();
        } else if (timer >= 45) {
            if (debug.get()) info("Finish!");
            mining = false;
            sentMessage = false;
            prevSlot = -1;
            timer = 0;
        }
    }

    private void crystalSupport(PlayerEntity target) {
        if (target == null || !endCrystal.found() || !obsidian.found()) return;
        BlockPos p = target.getBlockPos();
        BlockPos t = targetBlock;
        int prev = mc.player.getInventory().selectedSlot;
        InvUtils.swap(endCrystal.slot(), false);
        if (p.east().equals(t)) {
            int down = !bu.canPlace(t.east()) && !bu.canPlace(t.east().south()) && !bu.canPlace(t.east().north()) && !bu.canPlace(t.south()) && !bu.canPlace(t.north()) ? 1 : 0;
            BlockPos f = t.down(down);
            if (bu.isAir(f.east()) && (bu.canCrystal(f.east().down()))) {
                if (bu.isAir(f.east().down())) {
                    pu.place(f.east().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.east().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.south()) && (bu.canCrystal(f.south().down()))) {
                if (bu.isAir(f.south().down())) {
                    pu.place(f.south().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.south().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.north()) && (bu.canCrystal(f.north().down()))) {
                if (bu.isAir(f.north().down())) {
                    pu.place(f.north().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.north().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.east().south()) && (bu.canCrystal(f.east().south().down()))) {
                if (bu.isAir(f.east().south().down())) {
                    pu.place(f.east().south().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.east().south().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.east().north()) && (bu.canCrystal(f.east().north().down()))) {
                if (bu.isAir(f.east().north().down())) {
                    pu.place(f.east().north().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.east().north().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            }
        } else if (p.west().equals(t)) {
            int down = !bu.canPlace(t.west()) && !bu.canPlace(t.west().south()) && !bu.canPlace(t.west().north()) && !bu.canPlace(t.south()) && !bu.canPlace(t.north()) ? 1 : 0;
            BlockPos f = t.down(down);
            if (bu.isAir(f.west()) && (bu.canCrystal(f.west().down()))) {
                if (bu.isAir(f.west().down())) {
                    pu.place(f.west().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.west().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.south()) && (bu.canCrystal(f.south().down()))) {
                if (bu.isAir(f.south().down())) {
                    pu.place(f.south().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.south().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.north()) && (bu.canCrystal(f.north().down()))) {
                if (bu.isAir(f.north().down())) {
                    pu.place(f.north().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.north().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.west().south()) && (bu.canCrystal(f.west().south().down()))) {
                if (bu.isAir(f.west().south().down())) {
                    pu.place(f.west().south().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.west().south().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.west().north()) && (bu.canCrystal(f.west().north().down()))) {
                if (bu.isAir(f.west().north().down())) {
                    pu.place(f.west().north().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.west().north().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            }
        } else if (p.south().equals(t)) {
            int down = !bu.canPlace(t.south()) && !bu.canPlace(t.south().west()) && !bu.canPlace(t.south().east()) && !bu.canPlace(t.east()) && !bu.canPlace(t.west()) ? 1 : 0;
            BlockPos f = t.down(down);
            if (bu.isAir(f.south()) && (bu.canCrystal(f.south().down()))) {
                if (bu.isAir(f.south().down())) {
                    pu.place(f.south().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.south().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.west()) && (bu.canCrystal(f.west().down()))) {
                if (bu.isAir(f.west().down())) {
                    pu.place(f.west().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.west().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.east()) && (bu.canCrystal(f.east().down()))) {
                if (bu.isAir(f.east().down())) {
                    pu.place(f.east().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.east().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.south().west()) && (bu.canCrystal(f.south().west().down()))) {
                if (bu.isAir(f.south().west().down())) {
                    pu.place(f.south().west().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.south().west().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.south().east()) && (bu.canCrystal(f.south().east().down()))) {
                if (bu.isAir(f.south().east().down())) {
                    pu.place(f.south().east().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.south().east().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            }
        } else if (p.north().equals(t)) {
            int down = !bu.canPlace(t.north()) && !bu.canPlace(t.north().west()) && !bu.canPlace(t.north().east()) && !bu.canPlace(t.east()) && !bu.canPlace(t.west()) ? 1 : 0;
            BlockPos f = t.down(down);
            if (bu.isAir(f.north()) && (bu.canCrystal(f.north().down()))) {
                if (bu.isAir(f.north().down())) {
                    pu.place(f.north().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.north().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.west()) && (bu.canCrystal(f.west().down()))) {
                if (bu.isAir(f.west().down())) {
                    pu.place(f.west().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.west().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.east()) && (bu.canCrystal(f.east().down()))) {
                if (bu.isAir(f.east().down())) {
                    pu.place(f.east().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.east().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.north().west()) && (bu.canCrystal(f.north().west().down()))) {
                if (bu.isAir(f.north().west().down())) {
                    pu.place(f.north().west().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.north().west().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            } else if (bu.isAir(f.north().east()) && (bu.canCrystal(f.north().east().down()))) {
                if (bu.isAir(f.north().east().down())) {
                    pu.place(f.north().east().down(), obsidian, rotate.get(), false, swing.get());
                }
                pu.interact(f.north().east().down(), Hand.MAIN_HAND, rotate.get(), false, swing.get());
            }
        }
        InvUtils.swap(prev, false);
    }

    public boolean equals(BlockPos pos) {
        return isActive() && target != null && targetBlock != null && targetBlock == pos;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (render.get() == RenderMode.None || targetBlock == null) return;

        if (render.get() == RenderMode.Normal) {
            event.renderer.box(targetBlock, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        } else if (render.get() == RenderMode.Box) {
            Vec3d center = vu.getVec3d(targetBlock);
            double w = width.get() / 2;
            double h = height.get() / 2;
            Box box = new Box(center.x + w,center.y + h,center.z + w, center.x - w,center.y - h,center.z - w);
            event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        } else {
            Vec3d v = vu.getVec3d(targetBlock);
            double f = 0.5;
            int t = Math.round(100 * timer) / 45;
            double p = (t/100.0)-f;
            Box box = new Box(v.x + f, v.y - f, v.z + f, v.x - f, v.y + p, v.z - f);
            event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (targetBlock == null || target == null) return;
        Vector3d pos = new Vector3d(targetBlock.getX() + 0.5, targetBlock.getY() + ProgressYControl.get(), targetBlock.getZ() + 0.5);
        Vector3d name = new Vector3d(targetBlock.getX() + 0.5, targetBlock.getY() + NameYControl.get(), targetBlock.getZ() + 0.5);

        if (renderProgress.get() && NametagUtils.to2D(pos, scaleProgress.get())) {
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1.0, false, true);
            String progress = Math.round(100 * timer) / 45 +"%";
            if (Math.round(100 * timer)/45>=99) progress = "Done!";
            TextRenderer.get().render(progress, -TextRenderer.get().getWidth(progress) / 2.0, 0.0, progressColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }

        if (renderName.get() && NametagUtils.to2D(name, scaleName.get())) {
            NametagUtils.begin(name);
            TextRenderer.get().begin(1.0, false, true);
            String nameString = EntityUtils.getName(target);
            TextRenderer.get().render(nameString, -TextRenderer.get().getWidth(nameString) / 2.0, 0.0, nameColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public enum RenderMode {
        None,
        Box,
        Better,
        Normal
    }

    public enum SwapMode {
        Silent,
        Normal
    }
}
