package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.joml.Vector3d;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BedLay extends Module implements Wrapper {
    public BedLay(Category category, String name)  {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(5.5).sliderRange(0, 6).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").defaultValue(SortPriority.LowestHealth).build());

    private final SettingGroup sgPlacing = settings.createGroup("Placing");
    private final Setting<PlaceMode> placeMode = sgPlacing.add(new EnumSetting.Builder<PlaceMode>().name("place-mode").defaultValue(PlaceMode.Inwards).build());
    private final Setting<Integer> delay = sgPlacing.add(new IntSetting.Builder().name("delay").defaultValue(10).min(0).sliderMax(20).build());
    private final Setting<Boolean> trap = sgPlacing.add(new BoolSetting.Builder().name("trap").defaultValue(false).build());
    private final Setting<Boolean> packet = sgPlacing.add(new BoolSetting.Builder().name("packet").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgPlacing.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());

    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final Setting<Double> minTargetDMG = sgDamage.add(new DoubleSetting.Builder().name("min-target-damage").defaultValue(7).range(0, 36).sliderMax(36).build());
    private final Setting<Double> maxSelfDMG = sgDamage.add(new DoubleSetting.Builder().name("max-self-damage").defaultValue(7).range(0, 36).sliderMax(36).build());
    private final Setting<Boolean> antiSuicide = sgDamage.add(new BoolSetting.Builder().name("anti-suicide").defaultValue(false).build());

    private final SettingGroup sgRefill = settings.createGroup("Refill");
    private final Setting<Boolean> refill = sgRefill.add(new BoolSetting.Builder().name("refill").defaultValue(false).build());
    private final Setting<Integer> refillSlot = sgRefill.add(new IntSetting.Builder().name("refill-slot").defaultValue(9).range(1, 9).sliderRange(1, 9).visible(refill::get).build());
    private final Setting<Boolean> updateSlot = sgRefill.add(new BoolSetting.Builder().name("update-slot").defaultValue(false).visible(refill::get).build());

    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final Setting<Boolean> antiBedLose = sgMisc.add(new BoolSetting.Builder().name("anti-bed-lose").defaultValue(true).build());
    private final Setting<Boolean> debugBed = sgMisc.add(new BoolSetting.Builder().name("debug-bed").defaultValue(true).build());
    private final Setting<Boolean> autoSwap = sgMisc.add(new BoolSetting.Builder().name("auto-swap").defaultValue(true).build());

    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final Setting<Boolean> pauseOnCraft = sgPause.add(new BoolSetting.Builder().name("on-craft").defaultValue(true).build());
    private final Setting<Boolean> pauseOnBurrow = sgPause.add(new BoolSetting.Builder().name("on-burrow").defaultValue(false).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("on-eat").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("on-drink").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("on-mine").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private final Setting<RenderMode> render = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").defaultValue(RenderMode.Single).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(() -> render.get() != RenderMode.None).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Sides).build());
    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade").defaultValue(false).visible(() -> render.get() == RenderMode.Default).build());
    private final Setting<Integer> ticks = sgRender.add(new IntSetting.Builder().name("ticks").defaultValue(8).min(2).sliderMax(20).visible(() -> render.get() == RenderMode.Default && fade.get()).build());

    private final SettingGroup sgText = settings.createGroup("Text");
    private final Setting<Boolean> renderName = sgText.add(new BoolSetting.Builder().name("render-name").defaultValue(false).build());
    private final Setting<SettingColor> nameColor = sgText.add(new ColorSetting.Builder().name("color").visible(renderName::get).build());
    private final Setting<Double> nameScale = sgText.add(new DoubleSetting.Builder().name("scale").defaultValue(1).sliderRange(0.01, 2).visible(renderName::get).build());
    private final Setting<Double> nameY = sgText.add(new DoubleSetting.Builder().name("y").defaultValue(0.5).sliderRange(0, 1).visible(renderName::get).build());

    private PlayerEntity target;
    private FindItemResult bed;
    private BlockPos finalPos;
    private String name;

    private float yaw;
    private int timer;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();



    @Override
    public void onActivate() {
        timer = delay.get();
        yaw = 0;

        //for fixing bug 0_0
        if (debugBed.get()) {
            for (BlockEntity entity : Utils.blockEntities()) {
                if (!(entity instanceof BedBlockEntity) || PlayerUtils.distanceTo(entity.getPos()) > targetRange.get()) continue;
                BlockPos pos = entity.getPos();
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(vu.getVec3d(pos), Direction.DOWN, pos, false));
            }
        }

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
            renderBlocks.clear();
        }
    }

    @Override
    public void onDeactivate() {
        finalPos = null;
        target = null;
        bed = null;

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
            renderBlocks.clear();
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (mc.world.getDimension().bedWorks()) return;
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());

        if (target != null) {
            if (antiBedLose.get() && target.isImmuneToExplosion()) return;
            if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
            if (pauseOnCraft.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) return;
            bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            if (!bed.found()) return;

            if (antiSuicide.get() && target.getBlockPos().equals(mc.player.getBlockPos())) return;
            if (pauseOnBurrow.get() && bu.getBlastResistance(target.getBlockPos()) >= 600) return;

            renderBlocks.forEach(RenderBlock::tick);
            renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

            name = target.getEntityName();

            if (refill.get()) {
                if (bed.found() && bed.slot() != refillSlot.get() - 1) {
                    InvUtils.move().from(bed.slot()).toHotbar(refillSlot.get() - 1);
                    if (updateSlot.get()) nu.updateSlot(refillSlot.get() - 1);
                }
            }

            if (finalPos == null) getPos();
            if (finalPos == null) return;

            //bruh
            if ((!bu.getBlock(finalPos).equals(Blocks.AIR) && !(bu.getBlock(finalPos) instanceof BedBlock))
                || (target.prevX != target.getX()) || (target.prevY != target.getY()) || (target.prevZ != target.getZ())
                || DamageUtils.bedDamage(target, vu.getVec3d(getDirPos(finalPos, yaw))) < minTargetDMG.get()) {
                getPos();
            }

            if (trap.get() && BlockUtils.canPlace(target.getBlockPos().up(2)) && InvUtils.findInHotbar(Items.OBSIDIAN).found()) {
                pu.place(target.getBlockPos().up(2), InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), packet.get(), swing.get());
                if (swing.get()) pu.swingHand(Hand.MAIN_HAND, packet.get());
            }

            if (!bu.getState(finalPos).isReplaceable() && !(bu.getBlock(finalPos) instanceof BedBlock)) return;

            if (timer <= 0) {
                breaking(finalPos);
                timer = delay.get();
            } else {
                placing(finalPos);
                timer--;
            }
        } else {
            finalPos = null;
        }
    }

    private void placing(BlockPos pos) {
        if (pos == null || !bed.found()) return;
        if (bed.getHand() == null && !autoSwap.get()) return;
        if (bu.getBlock(pos) instanceof BedBlock) return;
        if (rotate.get()) nu.rotate(yaw, (float) Rotations.getPitch(pos), false);
        pu.place(pos, bed, false, false, swing.get());
        if (render.get().equals(RenderMode.Default)) renderBlocks.add(renderBlockPool.get().setBlock(pos, getDirPos(pos, yaw), ticks.get()));
    }

    private void breaking(BlockPos pos) {
        if (pos == null || !bed.found()) return;
        if (!(bu.getBlock(pos) instanceof BedBlock)) return;
        pu.interact(pos, bed.getHand(), true, packet.get(), swing.get());
        nu.rotate(yaw, (float) Rotations.getPitch(pos), false, () -> pu.place(pos, bed, false, packet.get(), swing.get()));
        if (render.get().equals(RenderMode.Default)) renderBlocks.add(renderBlockPool.get().setBlock(pos, getDirPos(pos, yaw), ticks.get()));
    }

    private void getPos() {
        if (target == null) return;
        int i = !bu.isTrapped(target, TrappedMode.Feet) ? 0 : !bu.isTrapped(target, TrappedMode.Face) ? 1 : 2;
        BlockPos p = target.getBlockPos().up(i);
        double selfDMG = DamageUtils.bedDamage(mc.player, vu.getVec3d(p));
        double targetDMG = DamageUtils.bedDamage(target, vu.getVec3d(p));
        double e = bu.canPlace(p.east()) ? PlayerUtils.distanceTo(p.east()) : 999.0+PlayerUtils.distanceTo(p.east());
        double w = bu.canPlace(p.west()) ? PlayerUtils.distanceTo(p.west()) : 999.0+PlayerUtils.distanceTo(p.west());
        double s = bu.canPlace(p.south()) ? PlayerUtils.distanceTo(p.south()) : 999.0+PlayerUtils.distanceTo(p.south());
        double n = bu.canPlace(p.north()) ? PlayerUtils.distanceTo(p.north()) : 999.0+PlayerUtils.distanceTo(p.north());
        double m = min(e,w,s,n);

        if (placeMode.get().equals(PlaceMode.Inwards)) {
            if (bu.canBed(p.north(), p) && pu.isWithinRange(p.north(), targetRange.get()) && m == n && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.north();
                yaw = 0;
            } else if (bu.canBed(p.south(), p) && pu.isWithinRange(p.south(), targetRange.get()) && m == s && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.south();
                yaw = 180;
            } else if (bu.canBed(p.east(), p) && pu.isWithinRange(p.east(), targetRange.get()) && m == e && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.east();
                yaw = 90;
            } else if (bu.canBed(p.west(), p) && pu.isWithinRange(p.west(), targetRange.get()) && m == w && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.west();
                yaw = -90;
            }
        } else if (placeMode.get().equals(PlaceMode.AroundBETA) && i != 2) {
            if (finalPos != null && (bu.getBlock(finalPos) instanceof BedBlock || bu.canPlace(finalPos, false))) return;
            if (bu.canBed(p.north(), p) && pu.isWithinRange(p.north(), targetRange.get()) && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                if (bu.canBed(p.north(2), p) && pu.isWithinRange(p.north(2), targetRange.get())) {
                    finalPos = p.north(2);
                    yaw = 0;
                } else if (bu.canBed(p.north().east(), p) && pu.isWithinRange(p.north().east(), targetRange.get())) {
                    finalPos = p.north().east();
                    yaw = 90;
                } else if (bu.canBed(p.north().west(), p) && pu.isWithinRange(p.north().west(), targetRange.get())) {
                    finalPos = p.north().west();
                    yaw = -90;
                }
            } else if (bu.canBed(p.south(), p) && pu.isWithinRange(p.south(), targetRange.get()) && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                if (bu.canBed(p.south(2), p) && pu.isWithinRange(p.south(2), targetRange.get())) {
                    finalPos = p.south(2);
                    yaw = 180;
                } else if (bu.canBed(p.south().east(), p) && pu.isWithinRange(p.south().east(), targetRange.get())) {
                    finalPos = p.south().east();
                    yaw = 90;
                } else if (bu.canBed(p.south().west(), p) && pu.isWithinRange(p.south().west(), targetRange.get())) {
                    finalPos = p.south().west();
                    yaw = -90;
                }
            } else if (bu.canBed(p.east(), p) && pu.isWithinRange(p.east(), targetRange.get()) && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                if (bu.canBed(p.east(2), p) && pu.isWithinRange(p.east(2), targetRange.get())) {
                    finalPos = p.east(2);
                    yaw = 90;
                } else if (bu.canBed(p.east().north(), p) && pu.isWithinRange(p.east().north(), targetRange.get())) {
                    finalPos = p.east().north();
                    yaw = 0;
                } else if (bu.canBed(p.east().south(), p) && pu.isWithinRange(p.east().south(), targetRange.get())) {
                    finalPos = p.east().south();
                    yaw = 180;
                }
            } else if (bu.canBed(p.west(), p) && pu.isWithinRange(p.west(), targetRange.get()) && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                if (bu.canBed(p.west(2), p) && pu.isWithinRange(p.west(2), targetRange.get())) {
                    finalPos = p.west(2);
                    yaw = -90;
                } else if (bu.canBed(p.west().north(), p) && pu.isWithinRange(p.west().north(), targetRange.get())) {
                    finalPos = p.west().north();
                    yaw = 0;
                } else if (bu.canBed(p.west().south(), p) && pu.isWithinRange(p.west().south(), targetRange.get())) {
                    finalPos = p.west().south();
                    yaw = 180;
                }
            }
        } else if (placeMode.get().equals(PlaceMode.Inwards) && i == 2) {
            if (bu.canBed(p.north(), p) && pu.isWithinRange(p.north(), targetRange.get()) && m == n && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.north();
                yaw = 0;
            } else if (bu.canBed(p.south(), p) && pu.isWithinRange(p.south(), targetRange.get()) && m == s && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.south();
                yaw = 180;
            } else if (bu.canBed(p.east(), p) && pu.isWithinRange(p.east(), targetRange.get()) && m == e && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.east();
                yaw = 90;
            } else if (bu.canBed(p.west(), p) && pu.isWithinRange(p.west(), targetRange.get()) && m == w && selfDMG < maxSelfDMG.get() && targetDMG > minTargetDMG.get()) {
                finalPos = p.west();
                yaw = -90;
            }
        }
    }

    private BlockPos getDirPos(BlockPos pos, float yaw) {
        BlockPos poses = null;

        switch ((int)yaw) {
            case 90 -> poses = pos.west();
            case -90 -> poses = pos.east();
            case 180 -> poses = pos.north();
            case 0 -> poses = pos.south();
        }

        return poses;
    }

    private double min(double d1,double d2,double d3,double d4) {
        return Math.min(Math.min(d1, d2), Math.min(d3,d4));
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (render.get() == RenderMode.None || finalPos == null || target == null) return;
        if (!(bu.getBlock(finalPos) instanceof BedBlock)) return;

        Box box = null;

        int x = finalPos.getX();
        int y = finalPos.getY();
        int z = finalPos.getZ();

        switch ((int)yaw) {
            case 0 -> box = new Box(x, y, z, x + 1, y + 0.6, z + 2);
            case 180 -> box = new Box(x, y, z - 1, x + 1, y + 0.6, z + 1);
            case 90 -> box = new Box(x - 1, y, z, x + 1, y + 0.6, z + 1);
            case -90 -> box = new Box(x, y, z, x + 2, y + 0.6, z + 1);
        }

        if (render.get() == RenderMode.Default) {
            if (!fade.get()) {
                event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            } else {
                if (!renderBlocks.isEmpty()) {
                    renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
                    renderBlocks.forEach(block -> block.renderBedForm(event, shapeMode.get()));
                }
            }
        }

        if (render.get() == RenderMode.Single) {
            event.renderer.box(finalPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (finalPos == null || !renderName.get()) return;
        if (!(bu.getBlock(finalPos) instanceof BedBlock)) return;
        Vector3d pos = vu.getVector3d(finalPos);
        pos.set(pos.x, (pos.y - 0.5) + nameY.get(), pos.z);

        if (NametagUtils.to2D(pos, nameScale.get())) {
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1.0, false, true);
            TextRenderer.get().render(name, -TextRenderer.get().getWidth(name) / 2.0, 0.0, nameColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    @Override
    public String getInfoString() {
        return target != null ? name : null;
    }

    public enum RenderMode {
        Default,
        None,
        Single

    }

    public enum PlaceMode {
        AroundBETA,
        Inwards
    }

    public class RenderBlock {
        public int ticks;

        public double offset;
        public BlockPos.Mutable head = new BlockPos.Mutable();
        public BlockPos.Mutable feet = new BlockPos.Mutable();
        private Color sidesTop;
        private Color sidesBottom;
        private Color linesTop;

        private Color linesBottom;

        public RenderBlock setBlock(BlockPos head, BlockPos feet, int tick) {
            this.head.set(head);
            this.feet.set(feet);
            ticks = tick;

            sidesTop = sideColor.get();
            sidesBottom = sideColor.get();
            linesTop = lineColor.get();
            linesBottom = lineColor.get();

            offset = 1;
            return this;
        }


        public void tick() {
            ticks--;
        }

        public void renderBedForm(Render3DEvent event, ShapeMode shapeMode) {
            if (sidesTop == null || sidesBottom == null || linesTop == null || linesBottom == null || head == null || feet == null) return;

            int preSideTopA = sidesTop.a;
            int preSideBottomA = sidesBottom.a;
            int preLineTopA = linesTop.a;
            int preLineBottomA = linesBottom.a;

            sidesTop.a *= (double) ticks / 8;
            sidesBottom.a *= (double) ticks / 8;
            linesTop.a *= (double) ticks / 8;
            linesBottom.a *= (double) ticks / 8;

            double x = head.getX();
            double y = head.getY();
            double z = head.getZ();

            double px3 = 1.875 / 10;
            double px8 = 5.62 / 10;

            double px16 = 1;
            double px32 = 2;

            Direction dir = Direction.EAST;

            if (feet.equals(head.west())) dir = Direction.WEST;
            if (feet.equals(head.east())) dir = Direction.EAST;
            if (feet.equals(head.south())) dir = Direction.SOUTH;
            if (feet.equals(head.north())) dir = Direction.NORTH;

            if (dir == Direction.NORTH) z -= 1;
            else if (dir == Direction.WEST) x -= 1;

            // Lines

            if (shapeMode.lines()) {
                if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                    // Edges

                    renderEdgeLines(x, y, z, px3, 1, event);
                    renderEdgeLines(x, y, z + px32 - px3, px3, 2, event);
                    renderEdgeLines(x + px16 - px3, y, z, px3, 3, event);
                    renderEdgeLines(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                    // High Lines

                    event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x, y + px3, z + px32, x, y + px8, z + px32, linesBottom, linesTop);
                    event.renderer.line(x + px16, y + px3, z, x + px16, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x + px16, y + px3, z + px32, x + px16, y + px8, z + px32, linesBottom, linesTop);

                    // Connections

                    event.renderer.line(x + px3, y + px3, z, x + px16 - px3, y + px3, z, linesBottom);
                    event.renderer.line(x + px3, y + px3, z + px32, x + px16 - px3, y + px3, z + px32, linesBottom);

                    event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px32 - px3, linesBottom);
                    event.renderer.line(x + px16, y + px3, z + px3, x + px16, y + px3, z + px32 - px3, linesBottom);

                    // Top

                    event.renderer.line(x, y + px8, z, x + px16, y + px8, z, linesTop);
                    event.renderer.line(x, y + px8, z + px32, x + px16, y + px8, z + px32, linesTop);
                    event.renderer.line(x, y + px8, z, x , y + px8, z + px32, linesTop);
                    event.renderer.line(x + px16, y + px8, z, x + px16, y + px8, z + px32, linesTop);
                } else {
                    // Edges

                    renderEdgeLines(x, y, z, px3, 1, event);
                    renderEdgeLines(x, y, z + px16 - px3, px3, 2, event);
                    renderEdgeLines(x + px32 - px3, y, z, px3, 3, event);
                    renderEdgeLines(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                    // High Lines

                    event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x + px32, y + px3, z, x + px32, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x, y + px3, z + px16, x, y + px8, z + px16, linesBottom, linesTop);
                    event.renderer.line(x + px32, y + px3, z + px16, x + px32, y + px8, z + px16, linesBottom, linesTop);

                    // Connections

                    event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px16 - px3, linesBottom);
                    event.renderer.line(x + px32, y + px3, z + px3, x + px32, y + px3, z + px16 - px3, linesBottom);

                    event.renderer.line(x + px3, y + px3, z, x + px32 - px3, y + px3, z, linesBottom);
                    event.renderer.line(x + px3, y + px3, z + px16, x + px32 - px3, y + px3, z + px16, linesBottom);

                    // Top

                    event.renderer.line(x, y + px8, z, x, y + px8, z + px16, linesTop);
                    event.renderer.line(x + px32, y + px8, z, x + px32, y + px8, z + px16, linesTop);
                    event.renderer.line(x, y + px8, z, x + px32 , y + px8, z, linesTop);
                    event.renderer.line(x, y + px8, z + px16, x + px32, y + px8, z + px16, linesTop);
                }
            }

            // Sides

            if (shapeMode.sides()) {
                if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                    // Horizontal

                    // Bottom


                    sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px16 - px3, y, z, x + px16, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x, y, z + px32 - px3, x + px3, z + px32, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px16 - px3, y, z + px32 - px3, x + px16, z + px32, event, sidesBottom, sidesBottom);


                    // Middle & Top


                    sideHorizontal(x + px3, y + px3, z, x + px16 - px3, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px3, y + px3, z + px32 - px3, x + px16 - px3, z + px32, event, sidesBottom, sidesBottom);

                    sideHorizontal(x, y + px3, z + px3, x + px16, z + px32 - px3, event, sidesBottom, sidesBottom);


                    sideHorizontal(x, y + px8, z, x + px16, z + px32, event, sidesTop, sidesTop);

                    // Vertical

                    // Edges

                    renderEdgeSides(x, y, z, px3, 1, event);
                    renderEdgeSides(x, y, z + px32 - px3, px3, 2, event);
                    renderEdgeSides(x + px16 - px3, y, z, px3, 3, event);
                    renderEdgeSides(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                    // Sides

                    sideVertical(x, y + px3, z, x + px16, y + px8, z, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z + px32, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z, x, y + px8, z + px32, event, sidesBottom, sidesTop);
                    sideVertical(x + px16, y + px3, z, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                } else {
                    // Horizontal

                    // Bottom


                    sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x, y, z + px16 - px3, x + px3, z + px16, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px32 - px3, y, z, x + px32, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px32 - px3, y, z + px16 - px3, x + px32, z + px16, event, sidesBottom, sidesBottom);


                    // Middle & Top


                    sideHorizontal(x, y + px3, z + px3, x + px3, z + px16 - px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px32 - px3, y + px3, z + px3, x + px32, z + px16 - px3, event, sidesBottom, sidesBottom);

                    sideHorizontal(x + px3, y + px3, z, x + px32 - px3, z + px16, event, sidesBottom, sidesBottom);


                    sideHorizontal(x, y + px8, z, x + px32, z + px16, event, sidesTop, sidesTop);

                    // Vertical

                    // Edges

                    renderEdgeSides(x, y, z, px3, 1, event);
                    renderEdgeSides(x + px32 - px3, y, z, px3, 3, event);
                    renderEdgeSides(x, y, z + px16 - px3, px3, 2, event);
                    renderEdgeSides(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                    // Sides

                    sideVertical(x, y + px3, z, x, y + px8, z + px16, event, sidesBottom, sidesTop);
                    sideVertical(x + px32, y + px3, z, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z, x + px32, y + px8, z, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z + px16, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                }
            }

            // Resetting the Colors

            sidesTop.a = preSideTopA;
            sidesBottom.a = preSideBottomA;
            linesTop.a = preLineTopA;
            linesBottom.a = preLineBottomA;
        }

        // Render Utils

        private void renderEdgeLines(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            // Horizontal

            if (edge != 2 && edge != 4) event.renderer.line(x, y, z, x + px3, y, z, linesBottom);
            if (edge != 3 && edge != 4) event.renderer.line(x, y, z, x, y, z + px3, linesBottom);

            if (edge != 1 && edge != 2) event.renderer.line(x + px3, y, z, x + px3, y, z + px3, linesBottom);
            if (edge != 1 && edge != 3) event.renderer.line(x, y, z + px3, x + px3, y, z + px3, linesBottom);

            // Vertical

            if (edge != 4) event.renderer.line(x, y, z, x, y + px3, z, linesBottom);
            if (edge != 2) event.renderer.line(x + px3, y, z, x + px3, y + px3, z, linesBottom);
            if (edge != 3) event.renderer.line(x, y, z + px3, x, y + px3, z + px3, linesBottom);
            if (edge != 1) event.renderer.line(x + px3, y, z + px3, x + px3, y + px3, z + px3, linesBottom);
        }

        private void renderEdgeSides(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            // Horizontal

            if (edge != 4 && edge != 2) sideVertical(x, y, z, x + px3, y + px3, z, event, sidesBottom, sidesBottom);
            if (edge != 4 && edge != 3) sideVertical(x, y, z, x, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 2) sideVertical(x + px3, y, z, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 3) sideVertical(x, y, z + px3, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
        }

        public void sideHorizontal(double x1, double y, double z1, double x2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, event, bottomSideColor, topSideColor);
        }

        public void sideVertical(double x1, double y1, double z1, double x2, double y2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, event, bottomSideColor, topSideColor);
        }
        private void side(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            event.renderer.quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, topSideColor, topSideColor, bottomSideColor, bottomSideColor);
        }

    }
}
