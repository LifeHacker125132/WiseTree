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
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.Wrapper;

public class PistonCrystal extends Module implements Wrapper {
    public PistonCrystal(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("place-delay").defaultValue(3).sliderRange(0, 20).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(7).sliderRange(0, 9).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(5.5).sliderRange(0, 6).build());
    private final Setting<Boolean> trap = sgGeneral.add(new BoolSetting.Builder().name("auto-trap").defaultValue(false).build());
    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder().name("anti-suicide").defaultValue(true).build());
    private final Setting<Integer> ticksExisted = sgGeneral.add(new IntSetting.Builder().name("ticks-existed").defaultValue(7).sliderRange(0, 50).build());
    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder().name("only-on-ground").defaultValue(false).build());
    private final Setting<Boolean> onlyHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").defaultValue(true).build());

    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final Setting<Boolean> targetNotFound = sgToggle.add(new BoolSetting.Builder().name("target-not-found").defaultValue(true).build());
    private final Setting<Boolean> itemsNotFound = sgToggle.add(new BoolSetting.Builder().name("items-not-found").defaultValue(true).build());
    private final Setting<Boolean> onDeath = sgToggle.add(new BoolSetting.Builder().name("on-death").defaultValue(true).build());
    private final Setting<Boolean> onLeft = sgToggle.add(new BoolSetting.Builder().name("on-left").defaultValue(true).build());

    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final Setting<Boolean> pauseOnBurrow = sgPause.add(new BoolSetting.Builder().name("on-burrow").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("on-eat").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("on-drink").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("on-mine").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(false).build());
    private final Setting<Boolean> pistonRender = sgRender.add(new BoolSetting.Builder().name("piston-render").defaultValue(true).visible(render::get).build());
    private final Setting<Boolean> redstoneRender = sgRender.add(new BoolSetting.Builder().name("redstone-render").defaultValue(false).visible(render::get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides).build());

    private BlockPos pistonPos;
    private BlockPos activatorPos;
    private BlockPos crystalPos;
    private PlayerEntity target;
    private Direction dir;
    private Stage stage;
    private int timer;

    @Override
    public void onActivate() {
        stage = Stage.Piston;
        timer = delay.get();
        activatorPos = null;
        crystalPos = null;
        pistonPos = null;
        target = null;
        dir = null;
    }



    @EventHandler
    private void onTickEvent(TickEvent.Post event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());

        if (target != null) {
            FindItemResult torch, piston, crystal, obby;
            torch = InvUtils.findInHotbar(Items.REDSTONE_TORCH);
            piston = InvUtils.findInHotbar(Items.PISTON);
            crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
            obby = InvUtils.findInHotbar(Items.OBSIDIAN);

            getPos(target);

            if (!torch.found() || !piston.found() || !crystal.found() || !obby.found()) {
                if (itemsNotFound.get()) toggle();
                return;
            }

            if (onlyGround.get() && !mc.player.isOnGround()) return;
            if (onlyHole.get() && !bu.isTrapped(mc.player, TrappedMode.Feet)) return;
            if (pauseOnBurrow.get() && bu.isBurrowed(target)) return;
            if (antiSuicide.get() && target.getBlockPos().equals(mc.player.getBlockPos())) return;
            if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

            if (pistonPos == null || crystalPos == null || activatorPos == null || dir == null) {
                next(Stage.Piston);
                return;
            }

            if (trap.get() && bu.canPlace(target.getBlockPos().up(2), true))
                BlockUtils.place(target.getBlockPos().up(2), obby, true, 50, false);

            if (timer <= 0) {
                switch (stage) {
                    case Piston -> {
                        if (placeBlock(pistonPos, dir, piston, obby, mc.player.getPitch(), swing.get())) {
                            next(Stage.Crystal);
                        }
                    }

                    case Piston2 -> next(Stage.Crystal);

                    case Crystal -> {
                        if (placeCrystal(crystalPos, crystal, obby, swing.get())) {
                            next(Stage.Torch);
                        }
                    }

                    case Torch -> {
                        if (placeBlock(activatorPos, dir, torch, obby, 90, swing.get())) {
                            next(Stage.Attack);
                        }
                    }

                    case Torch2 -> {
                        next(Stage.Attack);
                    }

                    case Attack -> {
                        for (Entity crystalEntity : mc.world.getEntities()) {
                            if (!(crystalEntity instanceof EndCrystalEntity) || PlayerUtils.distanceTo(crystalEntity) > placeRange.get()) continue;
                            if (crystalEntity.age < ticksExisted.get()) continue;
                            if (pu.hits(crystalEntity, false, swing.get())) {
                                next(Stage.Reset);
                            }
                        }
                    }

                    case Reset -> {
                        if (!bu.canPlace(activatorPos, false) && (bu.getBlock(activatorPos).equals(Blocks.REDSTONE_TORCH) || bu.getBlock(activatorPos).equals(Blocks.REDSTONE_WALL_TORCH)))
                            nu.mine(activatorPos, false, true, swing.get());
                        if (canTorch(activatorPos))
                            next(Stage.Piston);
                    }
                }
                timer = delay.get();
            } else {
                timer--;
            }
        } else if (targetNotFound.get()) {
            toggle();
        }
    }

    private boolean placeBlock(BlockPos pos, Direction dir, FindItemResult block, FindItemResult obby, float pitch, boolean swing) {
        if (pos == null || !block.found() || !obby.found() || dir == null) return false;
        int p = mc.player.getInventory().selectedSlot;
        BlockHitResult h = new BlockHitResult(vu.getVec3d(pos), Direction.DOWN, pos, false);
        InvUtils.swap(block.slot(), false);
        if (bu.canPlace(pos.down(), true)) BlockUtils.place(pos.down(), obby, true, 50, false);
        nu.rotate(getYaw(dir), pitch, false, () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, h));
        pu.swingHand(Hand.MAIN_HAND, !swing);
        InvUtils.swap(p, false);
        return true;
    }

    private boolean placeCrystal(BlockPos pos, FindItemResult crystal, FindItemResult obby, boolean swing) {
        if (pos == null || !crystal.found() || !obby.found()) return false;
        BlockHitResult h = new BlockHitResult(vu.getVec3d(pos.down()), Direction.DOWN, pos.down(), false);
        int p = mc.player.getInventory().selectedSlot;
        if (bu.canPlace(pos.down(), true)) BlockUtils.place(pos.down(), obby, true, 50, false);
        InvUtils.swap(crystal.slot(), false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, h);
        pu.swingHand(Hand.MAIN_HAND, !swing);
        InvUtils.swap(p, false);
        return true;
    }

    private void getPos(PlayerEntity entity) {
        BlockPos f = entity.getBlockPos().up(1);

        PistonPos E = new PistonPos(f.east(2), f.east(2).north(),  f.east(2).south(), Direction.EAST, placeRange.get());
        PistonPos W = new PistonPos(f.west(2), f.west(2).north(),  f.west(2).south(), Direction.WEST, placeRange.get());
        PistonPos S = new PistonPos(f.south(2), f.south(2).west(),  f.south(2).east(), Direction.SOUTH, placeRange.get());
        PistonPos N = new PistonPos(f.north(2), f.north(2).west(),  f.north(2).east(), Direction.NORTH, placeRange.get());

        double[] C = new double[] {
            canCrystal(f.east(), E.dir) ? getDistance(f.east()) : isBadPos(f.east()),
            canCrystal(f.west(), W.dir) ? getDistance(f.west()) : isBadPos(f.west()),
            canCrystal(f.south(), S.dir) ? getDistance(f.south()) : isBadPos(f.south()),
            canCrystal(f.north(), N.dir) ? getDistance(f.north()) : isBadPos(f.north()),
            canCrystal(f.east().up(), E.dir) ? getDistance(f.east().up()) : isBadPos(f.east().up()),
            canCrystal(f.west().up(), W.dir) ? getDistance(f.west().up()) : isBadPos(f.west().up()),
            canCrystal(f.south().up(), S.dir) ? getDistance(f.south().up()) : isBadPos(f.south().up()),
            canCrystal(f.north().up(), N.dir) ? getDistance(f.north().up()) : isBadPos(f.north().up()),
        };

        double m = Math.min(min(C[0], C[1], C[2], C[3]), min(C[4], C[5], C[6], C[7]));
        double r = placeRange.get();

        if (isBestPos(m, C[0], r)) {
            crystalPos = f.east();
            pistonPos = E.getBest(false);
            activatorPos = getTorchPos(pistonPos, E.dir, r);
            dir = E.dir;
        } else if (isBestPos(m, C[1], r)) {
            crystalPos = f.west();
            pistonPos = W.getBest(false);
            activatorPos = getTorchPos(pistonPos, W.dir, r);
            dir = W.dir;
        } else if (isBestPos(m, C[2], r)) {
            crystalPos = f.south();
            pistonPos = S.getBest(false);
            activatorPos = getTorchPos(pistonPos, S.dir, r);
            dir = S.dir;
        } else if (isBestPos(m, C[3], r)) {
            crystalPos = f.north();
            pistonPos = N.getBest(false);
            activatorPos = getTorchPos(pistonPos, N.dir, r);
            dir = N.dir;
        } else if (isBestPos(m, C[4], r)) {
            crystalPos = f.east().up();
            pistonPos = E.getBest(true);
            activatorPos = getTorchPos(pistonPos, E.dir, r);
            dir = E.dir;
        } else if (isBestPos(m, C[5], r)) {
            crystalPos = f.west().up();
            pistonPos = W.getBest(true);
            activatorPos = getTorchPos(pistonPos, W.dir, r);
            dir = W.dir;
        } else if (isBestPos(m, C[6], r)) {
            crystalPos = f.south().up();
            pistonPos = S.getBest(true);
            activatorPos = getTorchPos(pistonPos, S.dir, r);
            dir = S.dir;
        } else if (isBestPos(m, C[7], r)) {
            crystalPos = f.north().up();
            pistonPos = N.getBest(true);
            activatorPos = getTorchPos(pistonPos, N.dir, r);
            dir = N.dir;
        }
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

    private boolean isBestPos(double m, double p, double r) {
        return m == p && p <= r;
    }

    private double min(double d1,double d2,double d3,double d4) {
        return Math.min(Math.min(d1,d2), Math.min(d3,d4));
    }

    private double getDistance(BlockPos pos) {
        return PlayerUtils.distanceTo(pos);
    }

    private double isBadPos(BlockPos pos) {
        return getDistance(pos) + 1000;
    }

    private boolean intersectsWithEntity(BlockPos pos) {
        return !EntityUtils.intersectsWithEntity(new Box(pos), entity -> entity instanceof TntEntity || entity instanceof PlayerEntity);
    }

    private boolean intersectsWithItem(BlockPos pos) {
        return !EntityUtils.intersectsWithEntity(new Box(pos), entity -> entity instanceof TntEntity || entity instanceof PlayerEntity || entity instanceof ItemEntity);
    }

    private boolean canCrystal(BlockPos pos, Direction dir) {
        boolean canCrystal = (intersectsWithItem(pos) && bu.canCrystal(pos.down()) && (
            bu.canPlace(pos, false) ||
            bu.getBlock(pos).equals(Blocks.MOVING_PISTON)
            || bu.getBlock(pos).equals(Blocks.PISTON_HEAD)));
        boolean canPiston = switch (dir) {
            case DOWN,UP -> false;
            case NORTH ->
                canPush(pos.north(), dir)
                    || canPush(pos.north().east(), dir)
                    || canPush(pos.north().west(), dir)
                    || canPush(pos.north().up(), dir)
                    || canPush(pos.north().east().up(), dir)
                    || canPush(pos.north().west().up(), dir);
            case SOUTH ->
                canPush(pos.south(), dir)
                    || canPush(pos.south().east(), dir)
                    || canPush(pos.south().west(), dir)
                    || canPush(pos.south().up(), dir)
                    || canPush(pos.south().east().up(), dir)
                    || canPush(pos.south().west().up(), dir);
            case WEST ->
                canPush(pos.west(), dir)
                    || canPush(pos.west().south(), dir)
                    || canPush(pos.west().north(), dir)
                    || canPush(pos.west().up(), dir)
                    || canPush(pos.west().south().up(), dir)
                    || canPush(pos.west().north().up(), dir);
            case EAST ->
                canPush(pos.east(), dir)
                    || canPush(pos.east().south(), dir)
                    || canPush(pos.east().north(), dir)
                    || canPush(pos.east().up(), dir)
                    || canPush(pos.east().south().up(), dir)
                    || canPush(pos.east().north().up(), dir);
        };

        return canCrystal && canPiston;
    }

    private boolean canPush(BlockPos pos, Direction dir) {
        return canPiston(pos) && intersectsWithEntity(pos) && canPiston(pos.offset(getNegativeDir(dir))) && canTorch(pos, dir);
    }

    private boolean canPiston(BlockPos pos) {
        return (bu.canPlace(pos, false)
            || bu.getBlock(pos).equals(Blocks.MOVING_PISTON)
            || bu.getBlock(pos).equals(Blocks.PISTON_HEAD)
            || bu.getBlock(pos).equals(Blocks.STICKY_PISTON)
            || bu.getBlock(pos).equals(Blocks.PISTON));
    }

    private Direction getNegativeDir(Direction dir) {
        return switch (dir) {
            case DOWN -> Direction.UP;
            case UP -> Direction.DOWN;
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case WEST -> Direction.EAST;
            case EAST -> Direction.WEST;
        };
    }

    private BlockPos getTorchPos(BlockPos pistonPos, Direction dir, double r) {
        if (pistonPos == null || dir == null) return new BlockPos(0,0,0);
        BlockPos pos = null;

        switch (dir) {
            case SOUTH, NORTH -> {
                double t1 = dir == Direction.SOUTH ?
                    canTorch(pistonPos.south()) ? getDistance(pistonPos.south()) : isBadPos(pistonPos.south()) :
                    canTorch(pistonPos.north()) ? getDistance(pistonPos.north()) : isBadPos(pistonPos.north());
                double t2 = canTorch(pistonPos.west()) ? getDistance(pistonPos.west()) : isBadPos(pistonPos.west());
                double t3 = canTorch(pistonPos.east()) ? getDistance(pistonPos.east()) : isBadPos(pistonPos.east());
                double min = min(t1,t2,t3, 10000);
                if (min == t1 && t1 <= r) {
                    pos = dir == Direction.SOUTH ? pistonPos.south() : pistonPos.north();
                } else if (min == t2 && t2 <= r) {
                    pos = pistonPos.west();
                } else if (min == t3 && t3 <= r) {
                    pos = pistonPos.east();
                }
            }

            case EAST, WEST -> {
                double t1 = dir == Direction.EAST ?
                    canTorch(pistonPos.east()) ? getDistance(pistonPos.east()) : isBadPos(pistonPos.east()) :
                    canTorch(pistonPos.west()) ? getDistance(pistonPos.west()) : isBadPos(pistonPos.west());
                double t2 = canTorch(pistonPos.south()) ? getDistance(pistonPos.south()) : isBadPos(pistonPos.south());
                double t3 = canTorch(pistonPos.north()) ? getDistance(pistonPos.north()) : isBadPos(pistonPos.north());
                double min = min(t1,t2,t3, 10000);
                if (min == t1 && t1 <= r) {
                    pos = dir == Direction.EAST ? pistonPos.east() : pistonPos.west();
                } else if (min == t2 && t2 <= r) {
                    pos = pistonPos.south();
                } else if (min == t3 && t3 <= r) {
                    pos = pistonPos.north();
                }
            }
        }

        return pos;
    }

    private boolean canTorch(BlockPos pistonPos, Direction dir) {
        return switch (dir) {
            case SOUTH, NORTH -> dir == Direction.SOUTH ?
                canTorch(pistonPos.south()) : canTorch(pistonPos.north()) || canTorch(pistonPos.east()) || canTorch(pistonPos.west());
            case WEST, EAST -> dir == Direction.WEST ?
                canTorch(pistonPos.west()) : canTorch(pistonPos.east()) || canTorch(pistonPos.north()) || canTorch(pistonPos.south());
            case DOWN, UP -> false;
        };
    }

    private boolean canTorch(BlockPos pos) {
        return bu.canTorch(pos) && intersectsWithEntity(pos) && intersectsWithEntity(pos.down());
    }

    private void next(Stage stage) {
        this.stage = stage;
    }

    @EventHandler
    private void onGameLeftEvent(GameLeftEvent event) {
        if (onLeft.get()) toggle();
    }

    @EventHandler
    private void onPacketEvent(PacketEvent.Receive event) {
        if (event.packet instanceof HealthUpdateS2CPacket packet) {
            if (packet.getHealth() <= 0 && onDeath.get()) toggle();
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get() || pistonPos == null || crystalPos == null || activatorPos == null || dir == null) return;
        VoxelShape crystalShape = bu.getState(crystalPos).getOutlineShape(mc.world, crystalPos);
        VoxelShape pistonShape = bu.getState(pistonPos).getOutlineShape(mc.world, pistonPos);
        VoxelShape redstoneShape = bu.getState(activatorPos).getOutlineShape(mc.world, activatorPos);
        if (!pistonShape.isEmpty() && pistonRender.get()) render(event, pistonPos, pistonShape.getBoundingBox());
        if (!crystalShape.isEmpty() && pistonRender.get()) render(event, crystalPos, crystalShape.getBoundingBox());
        if (!redstoneShape.isEmpty() && redstoneRender.get()) render(event, activatorPos, redstoneShape.getBoundingBox());
    }

    private void render(Render3DEvent event, BlockPos bp, Box box) {
        event.renderer.box(bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    public enum Stage {
        Piston,
        Piston2,
        Crystal,
        Torch,
        Torch2,
        Attack,
        Reset
    }

    public static class PistonPos extends PistonUtils {
        public BlockPos p1, p2, p3, p4, p5, p6;
        public Direction dir, negative;
        public double r;

        public PistonPos(BlockPos p1,BlockPos p2,BlockPos p3, Direction dir, double r) {
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p1.up();
            this.p5 = p2.up();
            this.p6 = p3.up();
            this.dir = dir;
            this.negative = getNegativeDir(dir);
            this.r = r;
        }

        public BlockPos getBest(boolean up) {
            BlockPos best = new BlockPos(0,0,0);
            double d1 = getDistance(p1, up);
            double d2 = getDistance(p2, up);
            double d3 = getDistance(p3, up);
            double d4 = getDistance(p4, up);
            double d5 = getDistance(p5, up);
            double d6 = getDistance(p6, up);
            double min = min(d1,d2,d3,d4,d5,d6);

            if (min == d1 && d1 <= r) {
                best = p1;
            } else if (min == d2 && d2 <= r) {
                best = p2;
            } else if (min == d3 && d3 <= r) {
                best = p3;
            } else if (min == d4 && d4 <= r) {
                best = p4;
            } else if (min == d5 && d5 <= r) {
                best = p5;
            } else if (min == d6 && d6 <= r) {
                best = p6;
            }

            return up ? best.up() : best;
        }

        private double getDistance(BlockPos pos, boolean up) {
            return up ? canPush(pos.up(), this.dir) ? getDistance(pos.up()) : isBadPos(pos.up()) : canPush(pos, this.dir) ? getDistance(pos) : isBadPos(pos);
        }

        private double min(double d1,double d2,double d3,double d4,double d5,double d6) {
            return Math.min(Math.min(Math.min(d1,d2), Math.min(d3,d4)), Math.min(d5, d6));
        }

        @Override
        public double getDistance(BlockPos pos) {
            return super.getDistance(pos);
        }

        @Override
        public boolean canPush(BlockPos pos, Direction dir) {
            return super.canPush(pos, dir);
        }

        @Override
        public boolean canPiston(BlockPos pos) {
            return super.canPiston(pos);
        }

        @Override
        public boolean canTorch(BlockPos pistonPos, Direction dir) {
            return super.canTorch(pistonPos, dir);
        }

        @Override
        public boolean canTorch(BlockPos pos) {
            return super.canTorch(pos);
        }

        @Override
        public boolean intersectsWithEntity(BlockPos pos) {
            return super.intersectsWithEntity(pos);
        }
    }

    public static class DiagonalPos extends PistonUtils {
        public BlockPos offset1, offset2;
        public Direction dir1, dir2;
        public BlockPos pos;
        public double r;

        public DiagonalPos(BlockPos pos, DiagonalDir dir, double r) {
            this.dir1 = getDirection(dir, false);
            this.dir2 = getDirection(dir, true);
            this.pos = pos;
            this.offset1 = pos.offset(dir1);
            this.offset2 = pos.offset(dir2);
            this.r = r;
        }


        public BlockPos getBest(boolean up, boolean two) {
            BlockPos pos = new BlockPos(0,2,4);

//            double do1 = getDistance(offset1) : isBadPos(offset1);

            return pos;
        }

        private Direction getDirection(DiagonalDir dir, boolean two) {
            return switch (dir) {
                case SOUTHEAST -> two ? Direction.EAST : Direction.SOUTH;
                case NORTHEAST ->  two ? Direction.EAST : Direction.NORTH;
                case SOUTHWEST ->  two ? Direction.WEST : Direction.SOUTH;
                case NORTHWEST ->  two ? Direction.WEST : Direction.NORTH;
            };
        }

        private boolean canPush2(BlockPos pos, Direction dir) {
            return false;
        }

        @Override
        public double getDistance(BlockPos pos) {
            return super.getDistance(pos);
        }

        @Override
        public boolean canPiston(BlockPos pos) {
            return super.canPiston(pos);
        }

        @Override
        public boolean canTorch(BlockPos pistonPos, Direction dir) {
            return super.canTorch(pistonPos, dir);
        }

        @Override
        public boolean canTorch(BlockPos pos) {
            return super.canTorch(pos);
        }

        @Override
        public boolean intersectsWithEntity(BlockPos pos) {
            return super.intersectsWithEntity(pos);
        }
    }

    public static class PistonUtils {
        public double getDistance(BlockPos pos) {
            return PlayerUtils.distanceTo(pos);
        }

        public double isBadPos(BlockPos pos) {
            return getDistance(pos) + 1000;
        }

        public boolean canPush(BlockPos pos, Direction dir) {
            return canPiston(pos) && intersectsWithEntity(pos) && canPiston(pos.offset(getNegativeDir(dir))) && canTorch(pos, getNegativeDir(dir));
        }

        public Direction getNegativeDir(Direction dir) {
            return switch (dir) {
                case DOWN -> Direction.UP;
                case UP -> Direction.DOWN;
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case WEST -> Direction.EAST;
                case EAST -> Direction.WEST;
            };
        }

        public boolean canPiston(BlockPos pos) {
            return (bu.canPlace(pos, false)
                || bu.getBlock(pos).equals(Blocks.MOVING_PISTON)
                || bu.getBlock(pos).equals(Blocks.PISTON_HEAD)
                || bu.getBlock(pos).equals(Blocks.STICKY_PISTON)
                || bu.getBlock(pos).equals(Blocks.PISTON));
        }

        public boolean canTorch(BlockPos pistonPos, Direction dir) {
            return switch (dir) {
                case SOUTH, NORTH -> dir == Direction.SOUTH ?
                    canTorch(pistonPos.south()) : canTorch(pistonPos.north()) || canTorch(pistonPos.east()) || canTorch(pistonPos.west());
                case WEST, EAST -> dir == Direction.WEST ?
                    canTorch(pistonPos.west()) : canTorch(pistonPos.east()) || canTorch(pistonPos.north()) || canTorch(pistonPos.south());
                case DOWN, UP -> false;
            };
        }

        public boolean canTorch(BlockPos pos) {
            return bu.canTorch(pos) && intersectsWithEntity(pos) && intersectsWithEntity(pos.down());
        }

        public boolean intersectsWithEntity(BlockPos pos) {
            return !EntityUtils.intersectsWithEntity(new Box(pos), entity -> entity instanceof TntEntity || entity instanceof PlayerEntity);
        }
    }

    public enum DiagonalDir {
        SOUTHEAST,
        NORTHEAST,
        SOUTHWEST,
        NORTHWEST
    }
}
