package wise.tree.addon.render;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import wise.tree.addon.combat.AutoCity;
import wise.tree.addon.utils.Wrapper;

public class MineProgress extends Module implements Wrapper {
    public MineProgress(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("render-mode").defaultValue(Mode.Block).build());
    private final Setting<FillMode> blockMode = sgGeneral.add(new EnumSetting.Builder<FillMode>().name("block-mode").defaultValue(FillMode.Cube).visible(()-> mode.get().equals(Mode.Block)).build());
  ///  private final Setting<CubeMode> offsetMode = sgGeneral.add(new EnumSetting.Builder<CubeMode>().name("offset-mode").defaultValue(CubeMode.ToLess).visible(()->mode.get().equals(Mode.Block)).build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(() -> mode.get().equals(Mode.Block)).build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder().name("line-color").visible(() -> mode.get().equals(Mode.Block)).build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").visible(() -> mode.get().equals(Mode.Block)).build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("color").visible(() -> mode.get().equals(Mode.Tag)).build());
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").visible(() -> mode.get().equals(Mode.Tag)).defaultValue(1).sliderRange(0.01, 2).build());
    private final Setting<Double> y = sgGeneral.add(new DoubleSetting.Builder().name("y-offset").visible(() -> mode.get().equals(Mode.Tag)).defaultValue(0.5).sliderRange(0, 1).build());

    private BlockPos pos;

    @Override
    public void onDeactivate() {
        pos = null;
    }

    @EventHandler
    private void onBlockBreakEvent(StartBreakingBlockEvent event) {
        pos = event.blockPos;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (pos == null || !Utils.canUpdate()) return;
        if (!mc.interactionManager.isBreakingBlock()) return;
        if (AutoCity.instance.equals(pos)) return;
        if (bu.getHardness(pos) < 0) return;
        Vec3d v = vu.getVec3d(pos);
        float r =  ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() * 100;
        double f = 0.5;
        double p = (r/100.0)-f;
        Box box = null;
        double y = bu.getBox(pos).maxY;
        final double min = Math.min(v.y + p, y);
        switch (blockMode.get()) {
            case Box ->  box = new Box(v.x + f, v.y - f, v.z + f, v.x - f, min, v.z - f);
            case Cube -> box = new Box(v.x-p, v.y-p, v.z-p, v.x+p, min, v.z+p);
        }
        if (box != null) event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (pos == null || !Utils.canUpdate()) return;
        if (mode.get().equals(Mode.Tag)) {
            Vector3d vec = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            vec.set(vec.x, (vec.y - 0.5) + y.get(), vec.z);
            if (!mc.interactionManager.isBreakingBlock()) return;
            if (AutoCity.instance.equals(pos)) return;
            if (bu.getHardness(pos) < 0) return;
            if (NametagUtils.to2D(vec, scale.get())) {
                String progress = String.format("%.0f%%", ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() * 100);
                NametagUtils.begin(vec);
                TextRenderer.get().begin(1.0, false, true);
                TextRenderer.get().render(progress, -TextRenderer.get().getWidth(progress) / 2.0, 0.0, color.get());
                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }

    public enum Mode {
        Tag,
        Block
    }

    public enum FillMode {
        Cube,
        Box
    }

    public enum CubeMode {
        ToMore,
        ToLess
    }
}
