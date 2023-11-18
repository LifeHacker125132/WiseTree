package wise.tree.addon.combat;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.*;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;
import wise.tree.addon.utils.NPCEntity;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AutoCrystal extends Module implements Wrapper {
    public AutoCrystal(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(10).min(0).sliderMax(16).build());
    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder().name("ignore-terrain").defaultValue(true).build());
    private final Setting<LogicMode> logic = sgGeneral.add(new EnumSetting.Builder<LogicMode>().name("logic").defaultValue(LogicMode.BreakPlace).build());
    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>().name("swap").defaultValue(AutoSwitchMode.Normal).build());
    private final Setting<RotateMode> rotate = sgGeneral.add(new EnumSetting.Builder<RotateMode>().name("rotate").defaultValue(RotateMode.Place).build());

    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final Setting<SphereMode> sphereMode = sgDamage.add(new EnumSetting.Builder<SphereMode>().name("sort-mode").defaultValue(SphereMode.ByDistance).build());
    private final Setting<Integer> width = sgDamage.add(new IntSetting.Builder().name("width").defaultValue(7).min(0).sliderMax(16).visible(() -> !sphereMode.get().equals(SphereMode.ByDistance)).build());
    private final Setting<Integer> height = sgDamage.add(new IntSetting.Builder().name("height").defaultValue(4).min(0).sliderMax(12).visible(() -> !sphereMode.get().equals(SphereMode.ByDistance)).build());
    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder().name("min-target-damage").defaultValue(6).min(0).build());
    private final Setting<Double> maxDamage = sgDamage.add(new DoubleSetting.Builder().name("max-self-damage").defaultValue(6).range(0, 36).sliderMax(36).build());
    private final Setting<Boolean> antiSuicide = sgDamage.add(new BoolSetting.Builder().name("anti-suicide").defaultValue(true).build());

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final Setting<PlaceMode> doPlace = sgPlace.add(new EnumSetting.Builder<PlaceMode>().name("place").defaultValue(PlaceMode.Normal).build());
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("delay").defaultValue(0).min(0).sliderMax(20).visible(() -> !doPlace.get().equals(PlaceMode.None)).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("range").defaultValue(4.5).min(0).visible(() -> !doPlace.get().equals(PlaceMode.None)).sliderMax(6).build());
    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder().name("walls-range").visible(() -> !doPlace.get().equals(PlaceMode.None)).defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Boolean> placement112 = sgPlace.add(new BoolSetting.Builder().name("1.12-placement").visible(() -> !doPlace.get().equals(PlaceMode.None)).defaultValue(false).build());
    private final Setting<SupportMode> support = sgPlace.add(new EnumSetting.Builder<SupportMode>().name("support").visible(() -> !doPlace.get().equals(PlaceMode.None)).defaultValue(SupportMode.Disabled).build());
    private final Setting<Integer> supportDelay = sgPlace.add(new IntSetting.Builder().name("support-delay").defaultValue(1).min(0).visible(() -> support.get() != SupportMode.Disabled && !doPlace.get().equals(PlaceMode.None)).build());

    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final Setting<PlaceMode> doBreak = sgBreak.add(new EnumSetting.Builder<PlaceMode>().name("break").defaultValue(PlaceMode.Normal).build());
    private final Setting<Boolean> fastBreak = sgBreak.add(new BoolSetting.Builder().name("fast-break").visible(() -> !doBreak.get().equals(PlaceMode.None)).defaultValue(true).build());
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("delay").defaultValue(0).visible(() -> !doBreak.get().equals(PlaceMode.None)).min(0).sliderMax(20).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("range").visible(() -> !doBreak.get().equals(PlaceMode.None)).defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder().name("walls-range").visible(() -> !doBreak.get().equals(PlaceMode.None)).defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder().name("break-attempts").visible(() -> !doBreak.get().equals(PlaceMode.None)).defaultValue(2).sliderMin(1).sliderMax(5).build());
    private final Setting<Integer> ticksExisted = sgBreak.add(new IntSetting.Builder().name("ticks-existed").defaultValue(0).min(0).visible(() -> doBreak.get() != PlaceMode.None).build());
    private final Setting<Integer> attackFrequency = sgBreak.add(new IntSetting.Builder().name("attack-frequency").visible(() -> !doBreak.get().equals(PlaceMode.None)).defaultValue(25).min(1).sliderRange(1, 30).build());
    private final Setting<Boolean> onlyBreakOwn = sgBreak.add(new BoolSetting.Builder().name("only-own").visible(() -> !doBreak.get().equals(PlaceMode.None)).defaultValue(false).build());
    private final Setting<Boolean> antiWeakness = sgBreak.add(new BoolSetting.Builder().name("anti-weakness").defaultValue(true).visible(() -> !doBreak.get().equals(PlaceMode.None)).build());

    private final SettingGroup sgFacePlace = settings.createGroup("Face Place");
    private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder().name("face-place").defaultValue(true).build());
    private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder().name("health").defaultValue(8).min(1).sliderMin(1).sliderMax(36).visible(facePlace::get).build());
    private final Setting<Double> facePlaceDurability = sgFacePlace.add(new DoubleSetting.Builder().name("durability").defaultValue(2).min(1).sliderMin(1).sliderMax(100).visible(facePlace::get).build());
    private final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder().name("missing-armor").defaultValue(false).visible(facePlace::get).build());
    private final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder().name("force-face-place").defaultValue(Keybind.none()).build());

    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").defaultValue(true).build());
    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<RenderMode> render = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").defaultValue(RenderMode.Fade).build());
    private final Setting<VidMode> vidMode = sgRender.add(new EnumSetting.Builder<VidMode>().name("render-mode").defaultValue(VidMode.Gradient).visible(() -> render.get() != RenderMode.None).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(() -> render.get() != RenderMode.None).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> render.get() != RenderMode.None && !render.get().equals(RenderMode.Circle)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> render.get() != RenderMode.None).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-two").visible(() -> render.get() != RenderMode.None && vidMode.get().equals(VidMode.Gradient) && !render.get().equals(RenderMode.Circle)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-two").visible(() -> render.get() != RenderMode.None && vidMode.get().equals(VidMode.Gradient) && !render.get().equals(RenderMode.Circle)).build());
    private final Setting<Double> circleScale = sgRender.add(new DoubleSetting.Builder().name("circle-scale").visible(() -> render.get() == RenderMode.Circle).defaultValue(0.5).min(0).sliderMax(1).build());
    private final Setting<Double> circleHeight = sgRender.add(new DoubleSetting.Builder().name("circle-height").visible(() -> render.get() == RenderMode.Circle).defaultValue(0.13).min(0).sliderMax(0.5).build());
    private final Setting<Boolean> dynamicUpdate = sgRender.add(new BoolSetting.Builder().name("dynamic-y").defaultValue(false).visible(() -> render.get() == RenderMode.Circle).build());
    private final Setting<Double> staticY = sgRender.add(new DoubleSetting.Builder().name("circle-y").visible(() -> !dynamicUpdate.get() && render.get() == RenderMode.Circle).defaultValue(0.0).min(0).sliderMax(1).build());
    private final Setting<Integer> smoothFactor = sgRender.add(new IntSetting.Builder().name("smooth-factor").defaultValue(5).min(0).max(20).sliderRange(0, 20).visible(() -> render.get() == RenderMode.Smooth).build());
    private final Setting<Boolean> sFade = sgRender.add(new BoolSetting.Builder().name("fade").defaultValue(false).visible(() -> render.get() == RenderMode.Smooth).build());
    private final Setting<Integer> sFadeTicks = sgRender.add(new IntSetting.Builder().name("fade-ticks").defaultValue(8).min(0).max(20).sliderRange(0, 20).visible(() -> render.get() == RenderMode.Smooth && sFade.get()).build());
    private final Setting<Integer> fadeTicks = sgRender.add(new IntSetting.Builder().name("fade-ticks").defaultValue(8).min(0).max(20).sliderRange(0, 20).visible(() -> render.get() == RenderMode.Fade).build());

    private final SettingGroup sgText = settings.createGroup("Text");
    private final Setting<Boolean> renderDamageText = sgText.add(new BoolSetting.Builder().name("damage").defaultValue(true).build());
    private final Setting<SettingColor> damageTextColor = sgText.add(new ColorSetting.Builder().name("damage-color").visible(renderDamageText::get).build());
    private final Setting<Double> damageTextScale = sgText.add(new DoubleSetting.Builder().name("damage-scale").defaultValue(1.25).min(1).sliderMax(4).visible(renderDamageText::get).build());
    private final Setting<Double> damageTextY = sgText.add(new DoubleSetting.Builder().name("damage-y").defaultValue(0.7).min(0).sliderMax(1).visible(renderDamageText::get).build());
    private final Setting<Boolean> renderNameText = sgText.add(new BoolSetting.Builder().name("name").defaultValue(true).build());
    private final Setting<SettingColor> nameTextColor = sgText.add(new ColorSetting.Builder().name("name-color").visible(renderNameText::get).build());
    private final Setting<Double> nameTextScale = sgText.add(new DoubleSetting.Builder().name("name-scale").defaultValue(1.25).min(1).sliderMax(4).visible(renderNameText::get).build());
    private final Setting<Double> nameTextY = sgText.add(new DoubleSetting.Builder().name("name-y").defaultValue(0.4).min(0).sliderMax(1).visible(renderNameText::get).build());

    private final ExecutorService thread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * (1 + 13 / 3));
    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private final List<PlayerEntity> targets = new ArrayList<>();

    private final Box box = new Box(0, 0, 0, 0, 0, 0);
    private final Vec3d vec3dRayTraceEnd = new Vec3d(0, 0, 0);
    private final Vec3d playerEyePos = new Vec3d(0, 0, 0);
    private final Vec3d vec3d = new Vec3d(0, 0, 0);

    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();
    private final Int2IntMap waitingToExplode = new Int2IntOpenHashMap();
    private final IntSet placedCrystals = new IntOpenHashSet();
    private final IntSet removed = new IntOpenHashSet();

    private final BlockPos.Mutable placingCrystalBlockPos = new BlockPos.Mutable();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private BlockPos renderPos;

    private RaycastContext raycastContext;
    private PlayerEntity bestTarget;
    private Box renderBox = null;

    private int bestTargetTimer;
    private int placingTimer;
    private int ticksPassed;
    private int breakTimer;
    private int placeTimer;
    private int attacks;
    private double bestTargetDamage;
    private double renderDamage;
    private boolean placing;

    private double dynamicY = 0;
    private int offset = 0;


    @Override
    public void onActivate() {
        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        breakTimer = 0;
        placeTimer = 0;
        ticksPassed = 0;
        placing = false;
        dynamicY = 0;
        placingTimer = 0;
        offset = 0;
        attacks = 0;
        bestTargetDamage = 0;
        bestTargetTimer = 0;
        renderPos = null;
        renderDamage = 0.0;
        renderBox = null;
        if (!renderBlocks.isEmpty()) {
            for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
            renderBlocks.clear();
        }
    }

    @Override
    public void onDeactivate() {
        targets.clear();
        placedCrystals.clear();
        offset = 0;
        dynamicY = 0;
        attemptedBreaks.clear();
        waitingToExplode.clear();
        removed.clear();
        bestTarget = null;
        if (!renderBlocks.isEmpty()) {
            for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
            renderBlocks.clear();
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (placing) {
            if (placingTimer > 0) placingTimer--;
            else placing = false;
        }

        if (ticksPassed < 20) ticksPassed++;
        else ticksPassed = 0;
        attacks = 0;

        if (bestTargetTimer > 0) bestTargetTimer--;
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        bestTargetDamage = 0;

        for (IntIterator it = waitingToExplode.keySet().iterator(); it.hasNext();) {
            int id = it.nextInt();
            int ticks = waitingToExplode.get(id);

            if (ticks > 3) {
                it.remove();
                removed.remove(id);
            } else {
                waitingToExplode.put(id, ticks + 1);
            }
        }

        if (PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get())) return;
        ((IVec3d) playerEyePos).set(mc.player.getPos().x, mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getPos().z);
        findTargets();

        if (targets.size() > 0) {
            renderBlocks.forEach(RenderBlock::tick);
            renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
            if (logic.get() == LogicMode.BreakPlace) {
                doBreak();
                doPlace();
            } else if (logic.get() == LogicMode.PlaceBreak) {
                doPlace();
                doBreak();
            } else {
                doPlace();
                doBreak();
                doPlace();
            }
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;
        if (placing && event.entity.getBlockPos().equals(placingCrystalBlockPos)) {
            placing = false;
            placingTimer = 0;
            placedCrystals.add(event.entity.getId());
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity) {
            placedCrystals.remove(event.entity.getId());
            removed.remove(event.entity.getId());
            waitingToExplode.remove(event.entity.getId());
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (rotate.get() == RotateMode.None || !placing) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
        }
    }

    private void doBreak() {
        if (doBreak.get() == PlaceMode.None) return;
        double bestDamage = 0;
        Entity crystal = null;
        for (Entity entity : mc.world.getEntities()) {
            double damage = getBreakDamage(entity, true);
            if (damage > bestDamage) {
                bestDamage = damage;
                crystal = entity;
            }
        }
        if (crystal != null) doBreak(crystal);
    }

    private void doBreak(Entity crystal) {
        if (antiWeakness.get()) {
            StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
            if (weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())) {
                if (!isValidWeaknessItem(mc.player.getMainHandStack())) {
                    if (!InvUtils.swap(InvUtils.findInHotbar(this::isValidWeaknessItem).slot(), false)) return;
                    return;
                }
            }
        }
        if (rotate.get() == RotateMode.Break || rotate.get() == RotateMode.Both) {
            double yaw = Rotations.getYaw(crystal);
            double pitch = Rotations.getPitch(crystal, Target.Feet);
            Rotations.rotate(yaw, pitch, 50, () -> attackCrystal(crystal));
        } else {
            attackCrystal(crystal);
        }

        if (logic.get().equals(LogicMode.Multi)) placeCrystal(new BlockHitResult(vu.getVec3d(crystal.getBlockPos().down()),
                Direction.DOWN,
                crystal.getBlockPos().down(),
                false),
            getBreakDamage(crystal, false),
            null
        );

        breakTimer = breakDelay.get();
        removed.add(crystal.getId());
        attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);
        waitingToExplode.put(crystal.getId(), 0);
    }

    private void attackCrystal(Entity entity) {
        if (fastBreak.get() && ticksExisted.get() <= 0) entity.discard();
        if (doBreak.get() == PlaceMode.Normal) mc.interactionManager.attackEntity(mc.player, entity);
        else mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
        if (hand == null) hand = Hand.MAIN_HAND;
        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        attacks++;
    }

    private double getBreakDamage(Entity entity, boolean checkCrystalAge) {
        if (!(entity instanceof EndCrystalEntity)) return 0;
        if (onlyBreakOwn.get() && !placedCrystals.contains(entity.getId())) return 0;
        if (removed.contains(entity.getId())) return 0;
        if (attemptedBreaks.get(entity.getId()) > breakAttempts.get()) return 0;
        if (checkCrystalAge && entity.age < ticksExisted.get()) return 0;
        if (isOutOfRange(entity.getPos(), entity.getBlockPos(), false)) return 0;
        blockPos.set(entity.getBlockPos()).move(0, -1, 0);
        double selfDamage = DamageUtils.crystalDamage(mc.player, entity.getPos(), false, blockPos, ignoreTerrain.get());
        if (selfDamage > maxDamage.get() || (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player))) return 0;
        double damage = getDamageToTargets(entity.getPos(), blockPos, true);
        boolean facePlaced = (facePlace.get() && shouldFacePlace(entity.getBlockPos()) || forceFacePlace.get().isPressed());
        if (!facePlaced && damage < minDamage.get()) return 0;
        return damage;
    }

    private boolean isValidWeaknessItem(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ToolItem) || itemStack.getItem() instanceof HoeItem) return false;
        ToolMaterial material = ((ToolItem) itemStack.getItem()).getMaterial();
        return material == ToolMaterials.DIAMOND || material == ToolMaterials.NETHERITE;
    }

    private int getWidth() {
        return sphereMode.get() == SphereMode.ByDistance ? (int)Math.ceil(placeRange.get()) : width.get();
    }

    private int getHeight() {
        return sphereMode.get() == SphereMode.ByDistance ? (int)Math.ceil(placeRange.get()) : height.get();
    }

    private void doPlace() {
        if (doPlace.get() == PlaceMode.None || placeTimer > 0) return;
        if (!InvUtils.testInHotbar(Items.END_CRYSTAL)) return;
        if (autoSwitch.get() == AutoSwitchMode.None && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;
        for (Entity entity : mc.world.getEntities()) {if (getBreakDamage(entity, false) > 0) return;}
        AtomicDouble bestDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBlockPos = new AtomicReference<>(new BlockPos.Mutable());
        AtomicBoolean isSupport = new AtomicBoolean(support.get() != SupportMode.Disabled);
        BlockIterator.register(getWidth(), getHeight(), (bp, blockState) -> {
            boolean hasBlock = blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN);
            if (!hasBlock && (!isSupport.get() || !blockState.isReplaceable())) return;
            blockPos.set(bp.getX(), bp.getY() + 1, bp.getZ());
            if (!mc.world.getBlockState(blockPos).isAir()) return;
            if (placement112.get()) {
                blockPos.move(0, 1, 0);
                if (!mc.world.getBlockState(blockPos).isAir()) return;
            }
            ((IVec3d) vec3d).set(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
            blockPos.set(bp).move(0, 1, 0);
            if (isOutOfRange(vec3d, blockPos, true)) return;
            double selfDamage = DamageUtils.crystalDamage(mc.player, vec3d, false, bp, ignoreTerrain.get());
            if (selfDamage > maxDamage.get() || (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player))) return;
            double damage = getDamageToTargets(vec3d, bp, false);
            boolean facePlaced = (facePlace.get() && shouldFacePlace(blockPos)) || (forceFacePlace.get().isPressed());
            if (!facePlaced && damage < minDamage.get()) return;
            double x = bp.getX();
            double y = bp.getY() + 1;
            double z = bp.getZ();
            ((IBox) box).set(x, y, z, x + 1, y + (placement112.get() ? 1 : 2), z + 1);
            if (intersectsWithEntities(box)) return;
            if (damage > bestDamage.get() || (isSupport.get() && hasBlock)) {
                bestDamage.set(damage);
                bestBlockPos.get().set(bp);
            }
            if (hasBlock) isSupport.set(false);
        });

        BlockIterator.after(() -> {
            if (bestDamage.get() == 0) return;
            BlockHitResult result = getPlaceInfo(bestBlockPos.get());
            ((IVec3d) vec3d).set(
                result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 1.0 / 2.0,
                result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 1.0 / 2.0,
                result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 1.0 / 2.0
            );
            if (rotate.get() == RotateMode.Place || rotate.get() == RotateMode.Both) {
                double yaw = Rotations.getYaw(vec3d);
                double pitch = Rotations.getPitch(vec3d);
                Rotations.rotate(yaw, pitch, 50, () -> placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null));
            } else {
                placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null);
            }
            placeTimer += placeDelay.get();
        });
    }

    private BlockHitResult getPlaceInfo(BlockPos blockPos) {
        ((IVec3d) vec3d).set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction side : Direction.values()) {
            ((IVec3d) vec3dRayTraceEnd).set(
                blockPos.getX() + 0.5 + side.getVector().getX() * 0.5,
                blockPos.getY() + 0.5 + side.getVector().getY() * 0.5,
                blockPos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );
            ((IRaycastContext) raycastContext).set(vec3d, vec3dRayTraceEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                return result;
            }
        }
        Direction side = blockPos.getY() > vec3d.y ? Direction.DOWN : Direction.UP;
        return new BlockHitResult(vec3d, side, blockPos, false);
    }

    private void placeCrystal(BlockHitResult result, double damage, BlockPos supportBlock) {
        Item targetItem = supportBlock == null ? Items.END_CRYSTAL : Items.OBSIDIAN;
        FindItemResult item = InvUtils.findInHotbar(targetItem);
        if (!item.found()) return;
        int prevSlot = mc.player.getInventory().selectedSlot;
        if (autoSwitch.get() != AutoSwitchMode.None && !item.isOffhand()) InvUtils.swap(item.slot(), false);
        Hand hand = item.getHand();
        if (hand == null) return;
        if (supportBlock == null) {
            if (doPlace.get() == PlaceMode.Normal) mc.interactionManager.interactBlock(mc.player, hand, result);
            else mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));
            if (renderSwing.get()) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            placingCrystalBlockPos.set(result.getBlockPos()).move(0, 1, 0);
            renderBlocks.add(renderBlockPool.get().set(result.getBlockPos(), render.get() == RenderMode.Smooth ? sFadeTicks.get() : fadeTicks.get()));
            renderPos = result.getBlockPos();
            renderDamage = damage;
            placingTimer = 4;
            placing = true;
        } else {
            BlockUtils.place(supportBlock, item, false, 0, renderSwing.get(), true, false);
            placeTimer += supportDelay.get();
            if (supportDelay.get() == 0) placeCrystal(result, damage, null);
        }
        if (autoSwitch.get() == AutoSwitchMode.Silent) InvUtils.swap(prevSlot, false);
    }

    private boolean shouldFacePlace(BlockPos crystal) {
        for (PlayerEntity target : targets) {
            BlockPos pos = target.getBlockPos();
            if (bu.isTrapped(target, TrappedMode.Face)) continue;
            if (crystal.getY() == pos.getY() + 1 && Math.abs(pos.getX() - crystal.getX()) <= 1 && Math.abs(pos.getZ() - crystal.getZ()) <= 1) {
                if (EntityUtils.getTotalHealth(target) <= facePlaceHealth.get()) return true;
                for (ItemStack itemStack : target.getArmorItems()) {
                    if (itemStack == null || itemStack.isEmpty()) {
                        if (facePlaceArmor.get()) return true;
                    } else {
                        if ((double) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage() * 100 <= facePlaceDurability.get()) return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOutOfRange(Vec3d vec3d, BlockPos blockPos, boolean place) {
        ((IRaycastContext) raycastContext).set(playerEyePos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);
        if (result == null || !result.getBlockPos().equals(blockPos)) return !PlayerUtils.isWithin(vec3d, (place ? placeWallsRange : breakWallsRange).get());
        return !PlayerUtils.isWithin(vec3d, (place ? placeRange : breakRange).get());
    }

    private double getDamageToTargets(Vec3d vec3d, BlockPos obsidianPos, boolean breaking) {
        double damage = 0;

        for (PlayerEntity target : targets) {
            if (breaking && target.hurtTime > 0) continue;
            double dmg = DamageUtils.crystalDamage(target, vec3d, false, obsidianPos, ignoreTerrain.get());
            if (dmg > bestTargetDamage) {
                bestTarget = target;
                bestTargetDamage = dmg;
                bestTargetTimer = 10;
            }

            damage += dmg;
        }

        return damage;
    }

    @Override
    public String getInfoString() {
        return bestTarget != null && bestTargetTimer > 0 ? bestTarget.getGameProfile().getName() : null;
    }

    private void findTargets() {
        targets.clear();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player instanceof NPCEntity) continue;
            if (player.getAbilities().creativeMode || player == mc.player) continue;
            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && PlayerUtils.isWithin(player, targetRange.get())) {
                targets.add(player);
            }
        }
        FakePlayerManager.forEach(fp -> {
            if (!fp.isDead() && fp.isAlive() && Friends.get().shouldAttack(fp) && PlayerUtils.isWithin(fp, targetRange.get())) {
                targets.add(fp);
            }
        });
    }

    private boolean intersectsWithEntities(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !removed.contains(entity.getId()));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() == RenderMode.None || renderPos == null) return;

        switch (render.get()) {
            case Normal -> {
                if (vidMode.get().equals(VidMode.Normal)) {
                    event.renderer.box(renderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                } else {
                    ru.onRenderGradient(event, renderPos, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get());
                }
            }

            case Fade -> {
                renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), sideColor2.get(), lineColor2.get(), shapeMode.get(), vidMode.get()));
            }

            case Smooth -> {
                Box post = new Box(renderPos);
                if (renderBox == null) renderBox = post;
                double x = (post.minX - renderBox.minX) / smoothFactor.get();
                double y = (post.minY - renderBox.minY) / smoothFactor.get();
                double z = (post.minZ - renderBox.minZ) / smoothFactor.get();
                renderBox = new Box(renderBox.minX + x, renderBox.minY + y, renderBox.minZ + z, renderBox.maxX + x, renderBox.maxY + y, renderBox.maxZ + z);
                Vec3d fixedVec = renderBox.getCenter();
                Box box = new Box(fixedVec.x - 0.5, fixedVec.y - 0.5, fixedVec.z - 0.5, fixedVec.x + 0.5, fixedVec.y + 0.5, fixedVec.z + 0.5);
                if (sFade.get()) {
                    renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                    renderBlocks.forEach(renderBlock -> renderBlock.render(box, event, sideColor.get(), lineColor.get(), sideColor2.get(), lineColor2.get(), shapeMode.get(), vidMode.get()));
                } else {
                    if (vidMode.get().equals(VidMode.Normal)) {
                        event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    } else {
                        ru.onRenderGradient(event, renderPos, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get());
                    }
                }
            }

            case Circle -> {
                Vec3d last = null;
                Vec3d last2 = null;
                if (dynamicUpdate.get()) {
                    if (offset == 0) {
                        dynamicY += 0.01;
                    } else if (offset == 1) {
                        dynamicY -= 0.01;
                    }
                    if (dynamicY >= 0.74 && offset == 0) {
                        offset = 1;
                    }
                    if (dynamicY <= 0.04 && offset == 1) {
                        offset = 0;
                    }
                } else {
                    dynamicY = staticY.get();
                }
                for (int i = 0; i < 361; i += 1) {
                    Color color = new Color(lineColor.get().r, lineColor.get().g, lineColor.get().b, lineColor.get().a);
                    Vec3d tp = new Vec3d(renderPos.getX()+0.5, renderPos.getY()+dynamicY, renderPos.getZ()+0.5);
                    double rad = Math.toRadians(i);
                    double sin = Math.sin(rad) * circleScale.get();
                    double cos = Math.cos(rad) * circleScale.get();
                    double col = circleHeight.get();
                    Vec3d circle = new Vec3d(tp.x + sin, tp.y, tp.z + cos);
                    if (last != null) {
                        int diff = 0;
                        for (double offset = 0; offset < col; offset += 0.001) {
                            if (offset == 0.05) {
                                diff = 50;
                            }
                            if (offset == 0.09) {
                                diff = 100;
                            }
                            if (offset == 0.12) {
                                diff = 150;
                            }
                            event.renderer.line(last.x, last.y, last.z, circle.x, circle.y+offset, circle.z,
                                new Color(color.r, color.g, color.b, color.a-diff)
                            );
                        }
                    }
                    if (last2 != null) {
                        int diff = 0;
                        for (double offset = 0; offset < col; offset += 0.001) {
                            if (offset == 0.05) {
                                diff = 50;
                            }
                            if (offset == 0.09) {
                                diff = 100;
                            }
                            if (offset == 0.12) {
                                diff = 150;
                            }
                            event.renderer.line(last2.x, last2.y, last2.z, circle.x, circle.y+offset, circle.z,
                                new Color(color.r, color.g, color.b, color.a-diff)
                            );
                        }
                    }
                    last = circle;
                    last2 = circle;
                }
            }
            case None -> {
            }
        }
    }

    private boolean contains(int _int) {
        String str = Integer.toString(_int);
        return str.contains("5");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (renderPos == null) return;
        Vector3d dmg = vu.getVector3d(renderPos);
        if (render.get() == RenderMode.Smooth) {
            Box post = new Box(renderPos);
            if (renderBox == null) renderBox = post;
            double x = (post.minX - renderBox.minX) / smoothFactor.get();
            double y = (post.minY - renderBox.minY) / smoothFactor.get();
            double z = (post.minZ - renderBox.minZ) / smoothFactor.get();
            renderBox = new Box(renderBox.minX + x, renderBox.minY + y, renderBox.minZ + z, renderBox.maxX + x, renderBox.maxY + y, renderBox.maxZ + z);
            Vec3d fixedVec = renderBox.getCenter();
            dmg.set(fixedVec.x, (fixedVec.y - 0.5) + damageTextY.get(), fixedVec.z);
        } else {
            dmg.set(dmg.x, (dmg.y - 0.5) + damageTextY.get(), dmg.z);
        }

        if (renderDamageText.get() && NametagUtils.to2D(dmg, damageTextScale.get())) {
            NametagUtils.begin(dmg);
            TextRenderer.get().begin(1, false, true);
            String text = String.format("%.1f", renderDamage);
            double w = TextRenderer.get().getWidth(text) / 2;
            TextRenderer.get().render(text, -w, 0, damageTextColor.get(), false);
            TextRenderer.get().end();
            NametagUtils.end();
        }

        Vector3d name = vu.getVector3d(renderPos);
        if (render.get() == RenderMode.Smooth) {
            Box post = new Box(renderPos);
            if (renderBox == null) renderBox = post;
            double x = (post.minX - renderBox.minX) / smoothFactor.get();
            double y = (post.minY - renderBox.minY) / smoothFactor.get();
            double z = (post.minZ - renderBox.minZ) / smoothFactor.get();
            renderBox = new Box(renderBox.minX + x, renderBox.minY + y, renderBox.minZ + z, renderBox.maxX + x, renderBox.maxY + y, renderBox.maxZ + z);
            Vec3d fixedVec = renderBox.getCenter();
            name.set(fixedVec.x, (fixedVec.y - 0.5) + nameTextY.get(), fixedVec.z);
        } else {
            name.set(name.x, (name.y - 0.5) + nameTextY.get(), name.z);
        }

        if (renderNameText.get() && bestTarget != null && NametagUtils.to2D(name, nameTextScale.get())) {
            NametagUtils.begin(name);
            TextRenderer.get().begin(1, false, true);
            String text = bestTarget.getEntityName();
            double w = TextRenderer.get().getWidth(text) / 2;
            TextRenderer.get().render(text, -w, 0, nameTextColor.get(), false);
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public enum RotateMode {
        Break,
        Place,
        Both,
        None
    }

    public enum AutoSwitchMode {
        Normal,
        Silent,
        None
    }

    public enum PlaceMode {
        None,
        Packet,
        Normal
    }

    public enum SupportMode {
        Disabled,
        Accurate,
        Fast
    }

    public enum LogicMode {
        BreakPlace,
        PlaceBreak,
        Multi
    }

    public enum RenderMode {
        Circle,
        Smooth,
        Fade,
        Normal,
        None
    }

    public enum VidMode {
        Gradient,
        Normal
    }

    public enum SphereMode {
        ByDistance,
        ByRadius
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;
        public RenderBlock set(BlockPos blockPos, int tick) {
            pos.set(blockPos);
            ticks = tick;
            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, Color sides2, Color lines2, ShapeMode shapeMode, VidMode mode) {
            int preSideA = sides.a;
            int preLineA = lines.a;
            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;
            if (mode.equals(VidMode.Normal)) {
                event.renderer.box(pos, sides, lines, shapeMode, 0);
            } else {
                ru.onRenderGradient(event, pos, sides, sides2, lines, lines2, shapeMode);
            }
            sides.a = preSideA;
            lines.a = preLineA;
        }

        public void render(Box box, Render3DEvent event, Color sides, Color lines, Color sides2, Color lines2, ShapeMode shapeMode, VidMode mode) {
            int preSideA = sides.a;
            int preLineA = lines.a;
            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;
            if (mode.equals(VidMode.Normal)) {
                event.renderer.box(box, sides, lines, shapeMode, 0);
            } else {
                ru.onRenderGradient(event, pos, sides, sides2, lines, lines2, shapeMode);
            }
            sides.a = preSideA;
            lines.a = preLineA;
        }
    }
}
