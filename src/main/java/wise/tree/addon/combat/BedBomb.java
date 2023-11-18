package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import org.joml.Vector3d;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class BedBomb extends Module implements Wrapper {
    public BedBomb(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(10).min(0).sliderMax(20).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(5.5).min(0).sliderMax(22).build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(5.5).min(0).sliderMax(8).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").defaultValue(false).build());
    private final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder().name("width").defaultValue(6).min(0).sliderMax(16).build());
    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder().name("height").defaultValue(4).min(0).sliderMax(12).build());
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-target-damage").defaultValue(7).min(0).sliderMax(36).build());
    private final Setting<Double> maxSelf = sgGeneral.add(new DoubleSetting.Builder().name("max-self-damage").defaultValue(7).min(0).sliderMax(36).build());
    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder().name("anti-suicide").defaultValue(false).build());

    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final Setting<Boolean> refill = sgMisc.add(new BoolSetting.Builder().name("refill").defaultValue(false).build());
    private final Setting<Integer> refillSlot = sgMisc.add(new IntSetting.Builder().name("refill-slot").defaultValue(9).range(1, 9).sliderRange(1, 9).visible(refill::get).build());
    private final Setting<Boolean> updateSlot = sgMisc.add(new BoolSetting.Builder().name("update-slot").defaultValue(false).visible(refill::get).build());
    private final Setting<Boolean> pauseOnCraft = sgMisc.add(new BoolSetting.Builder().name("pause-on-craft").defaultValue(false).build());
    private final Setting<Boolean> pauseOnEat = sgMisc.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgMisc.add(new BoolSetting.Builder().name("pause-on-drink").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgMisc.add(new BoolSetting.Builder().name("pause-on-mine").defaultValue(false).build());
    private final Setting<Boolean> antiBedLose = sgMisc.add(new BoolSetting.Builder().name("anti-bed-lose").defaultValue(true).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private final Setting<RenderMode> render = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").defaultValue(RenderMode.Single).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(() -> render.get() != RenderMode.None).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() != RenderMode.None && shapeMode.get() != ShapeMode.Sides).build());
    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade").defaultValue(false).visible(() -> render.get() == RenderMode.Default).build());
    private final Setting<Integer> ticks = sgRender.add(new IntSetting.Builder().name("ticks").defaultValue(8).min(2).sliderMax(20).visible(() -> render.get() == RenderMode.Default && fade.get()).build());

    private final SettingGroup sgText = settings.createGroup("Text");
    private final Setting<Boolean> renderDamage = sgText.add(new BoolSetting.Builder().name("render-damage").defaultValue(false).build());
    private final Setting<Boolean> damageShadow = sgText.add(new BoolSetting.Builder().name("damage-shadow").defaultValue(false).visible(renderDamage::get).build());
    private final Setting<SettingColor> damageColor = sgText.add(new ColorSetting.Builder().name("damage-color").visible(renderDamage::get).build());
    private final Setting<Double> damageScale = sgText.add(new DoubleSetting.Builder().name("damage-scale").defaultValue(1).sliderRange(0.01, 2).visible(renderDamage::get).build());
    private final Setting<Double> damageY = sgText.add(new DoubleSetting.Builder().name("damage-y").defaultValue(0.5).sliderRange(0, 1).visible(renderDamage::get).build());

    private BedBombMore placement;
    private PlayerEntity target;
    private int timer;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();



    @Override
    public void onActivate() {
        timer = 0;
        target = null;
        placement = null;

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
            renderBlocks.clear();
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.world.getDimension().bedWorks()) return;

        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
            placement = null;
            return;
        }

        if (target != null) {
            FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            if (antiBedLose.get() && target.isImmuneToExplosion()) return;
            if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
            if (pauseOnCraft.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) return;
            if (!bed.found()) return;

            renderBlocks.forEach(RenderBlock::tick);
            renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

            if (refill.get()) {
                if (bed.found() && bed.slot() != refillSlot.get() - 1) {
                    InvUtils.move().from(bed.slot()).toHotbar(refillSlot.get() - 1);
                    if (updateSlot.get()) nu.updateSlot(refillSlot.get() - 1);
                }
            }

            if (timer <= 0) {
                placement = getPlacePos(target, width.get(), height.get(), range.get(), minDamage.get(), maxSelf.get(), antiSuicide.get(), false, BlockSortMode.CloseToTarget);
                if (distanceTo(placement.pos) < range.get() + 1) {
                    doBomb(bed, placement.pos, placement.dir, packet.get(), swing.get());
                }
                timer = delay.get();
            } else {
                timer--;
            }
        } else {
            placement = null;
        }
    }

    private void doBomb(FindItemResult bed, BlockPos pos, CardinalDirection dir, boolean packet, boolean swing) {
        if (pos == null || dir == null || target == null) return;
        doBed(bed, pos, dir, true, packet, swing, false);
        doBed(bed, pos, dir, true, packet, swing, true);
    }

    private void doBed(FindItemResult bed, BlockPos pos, CardinalDirection dir, boolean rotateToPos, boolean packet, boolean swing, boolean breakBed) {
        if (pos == null || dir == null) return;
        nu.rotate(getYaw(dir), Rotations.getPitch(pos), false, () -> {
            if (!breakBed && !(bu.getBlock(pos) instanceof BedBlock)) pu.place(pos, bed, rotateToPos, packet, swing);
            else if (breakBed && bu.getBlock(pos) instanceof BedBlock) pu.interact(pos, Hand.MAIN_HAND, rotateToPos, packet, swing);
            if (render.get().equals(RenderMode.Default)) renderBlocks.add(renderBlockPool.get().setBlock(pos, getOffsetPos(pos, dir), ticks.get()));
        });
    }

    public BedBombMore getPlacePos(PlayerEntity target, int xRadius, int yRadius, double range, double minDmg, double maxDmg, boolean antiSuicide, boolean groundOnly, BlockSortMode sortMode) {
        if (target == null) return null;
        BedBombMore placement = new BedBombMore();
        BlockPos.Mutable bedPos = new BlockPos.Mutable();
        BlockPos tPos = target.getBlockPos();
        CardinalDirection bedDir = CardinalDirection.North;
        double bestDamage = 0;
        double currentHP = PlayerUtils.getTotalHealth();
        ArrayList<BlockPos> toCheck = new ArrayList<>();
        if (bu.isTrapped(target, TrappedMode.Feet)) {
            IntStream.rangeClosed(1, yRadius).forEach(i -> {
                BlockPos b = tPos.up(i);
                for (CardinalDirection cd : CardinalDirection.values()) toCheck.add(b.offset(cd.toDirection()));
            });
        } else {
            toCheck.addAll(getSphere(target, xRadius, yRadius, sortMode));
        }
        for (BlockPos p : toCheck) {
            if (p.equals(tPos) || groundOnly && !bu.getState(p.down()).isSolidBlock(mc.world, p.down())) continue;
            BlockPos pos = new BlockPos(p);
            if (!bu.canPlace(pos) || PlayerUtils.distanceTo(pos) > range) continue;
            for (CardinalDirection d : CardinalDirection.values()) {
                BlockPos offset = new BlockPos(p.offset(d.toDirection()));
                if (!bu.canPlace(offset) || groundOnly && !bu.getState(offset.down()).isSolidBlock(mc.world, offset.down())) continue;
                Vec3d bed = vu.getVec3d(offset);
                double targetDMG = DamageUtils.bedDamage(target, bed);
                if (targetDMG < minDmg || targetDMG < bestDamage) continue;
                double selfDMG = DamageUtils.bedDamage(mc.player, bed);
                double postHP = currentHP - selfDMG;
                if (antiSuicide && postHP <= 0 ||  selfDMG > maxDmg) continue;
                bestDamage = targetDMG;
                bedDir = d;
                bedPos.set(pos);
            }
        }

        placement.set(bedPos, bedDir, bestDamage);
        return placement;
    }

    private float getYaw(CardinalDirection dir) {
        return switch (dir) {
            case North -> 180;
            case East -> -90;
            case West -> 90;
            case South -> 0;
        };
    }

    private BlockPos getOffsetPos(BlockPos pos, CardinalDirection dir) {
        return switch (dir) {
            case North -> pos.north(2);
            case East -> pos.east();
            case South -> pos.south();
            case West -> pos.west();
        };
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (render.get() == RenderMode.None || target == null || placement == null) return;
        Box box = null;

        int x = placement.pos.getX();
        int y = placement.pos.getY();
        int z = placement.pos.getZ();

        switch ((int)getYaw(placement.dir)) {
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
            event.renderer.box(placement.pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (placement == null || !renderDamage.get()) return;
        if (!(bu.getBlock(placement.pos) instanceof BedBlock)) return;
        Vector3d pos = vu.getVector3d(placement.pos);
        pos.set(pos.x, (pos.y - 0.5) + damageY.get(), pos.z);

        if (NametagUtils.to2D(pos, damageScale.get())) {
            String text = String.format("%.1f", placement.damage);
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1.0, false, true);
            TextRenderer.get().render(String.valueOf(text), -TextRenderer.get().getWidth(text) / 2.0, 0.0, damageColor.get(), damageShadow.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public static class BedBombMore {
        private BlockHitResult hitResult;
        private CardinalDirection dir;
        private BlockPos pos, offset;
        private double damage;
        private Vec3d vec;

        public BedBombMore() {}
        public void set(BlockPos center, CardinalDirection dirO, double dmg) {
            this.pos = center;
            this.offset = center.offset(dirO.toDirection());
            this.vec = vu.getVec3d(center);
            this.damage = dmg;
            this.dir = dirO;
            this.hitResult = new BlockHitResult(vu.getVec3d(center), Direction.UP, center, true);
        }
    }

    public enum BlockSortMode {CloseToSelf, CloseToTarget}
    public PlayerEntity cachedTarget = null;

    public ArrayList<BlockPos> getSphere(PlayerEntity target, int xRadius, int yRadius, BlockSortMode sortMode) {
        if (target == null) return null;
        cachedTarget = target;

        ArrayList<BlockPos> ar = new ArrayList<>();
        BlockPos tPos = target.getBlockPos();
        BlockPos.Mutable p = new BlockPos.Mutable();

        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    p.set(tPos).move(x, y, z);
                    BlockPos p2 = p.toImmutable();
                    if (bu.getState(p2.down()).isSolidBlock(mc.world, p2.down())) continue;
                    if (MathHelper.sqrt((float) ((tPos.getX() - p2.getX()) * (tPos.getX() - p2.getX()) + (tPos.getZ() - p2.getZ()) * (tPos.getZ() - p2.getZ()))) <= xRadius && MathHelper.sqrt((float) ((tPos.getY() - p2.getY()) * (tPos.getY() - p2.getY()))) <= yRadius)
                    {if (!ar.contains(p2)) ar.add(p2);}
                }
            }
        }

        switch (sortMode) {
            case CloseToSelf -> ar.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
            case CloseToTarget -> ar.sort(Comparator.comparingDouble(this::distanceTo));
        }

        return ar;
    }

    public double distanceTo(BlockPos blockPos) {
        return distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public double distanceTo(double x, double y, double z) {
        if (cachedTarget == null) return 100;
        float f = (float) (cachedTarget.getX() - x);
        float g = (float) (cachedTarget.getY() - y);
        float h = (float) (cachedTarget.getZ() - z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public enum RenderMode {
        Default,
        None,
        Single
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
