package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3d;
import wise.tree.addon.utils.NPCEntity;
import wise.tree.addon.utils.WTModule;
import wise.tree.addon.utils.Wrapper;

public class BurrowEsp extends WTModule implements Wrapper {
    public BurrowEsp(Category category, String name) {
        super(category, name);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("notification-mode").defaultValue(Mode.Tag).build());
    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder().name("ignore-own").defaultValue(true).build());
    private final Setting<Boolean> ignoreFriend = sgGeneral.add(new BoolSetting.Builder().name("ignore-friend").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(()->mode.get().equals(Mode.Render)).build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").visible(() -> mode.get().equals(Mode.Render) && shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder().name("line-color").visible(() -> mode.get().equals(Mode.Render) && shapeMode.get() != ShapeMode.Sides).build());
    private final Setting<SettingColor> lineColorTag = sgGeneral.add(new ColorSetting.Builder().name("line-color").visible(() -> mode.get().equals(Mode.Tag) && shapeMode.get() != ShapeMode.Sides).build());
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").defaultValue(1).sliderRange(0, 2).visible(()->mode.get().equals(Mode.Tag)).build());
    private final Setting<Double> yOffset = sgGeneral.add(new DoubleSetting.Builder().name("y-offset").defaultValue(0.5).sliderRange(0, 1).visible(()->mode.get().equals(Mode.Tag)).build());

    @EventHandler
    private void onRender2d(Render2DEvent event) {
        if (mode.get().equals(Mode.Render)) return;
        for (PlayerEntity entities : mc.world.getPlayers()) {
            if (entities instanceof NPCEntity) continue;
            if (entities == mc.player && ignoreOwn.get()) continue;
            if (Friends.get().isFriend(entities) && ignoreFriend.get()) continue;
            BlockPos pos = entities.getBlockPos();
            if (bu.getBlastResistance(pos) > 599) {
                Vector3d vec = vu.getVector3d(pos);
                vec.set(vec.x, (vec.y - 0.5) + yOffset.get(), vec.z);
                if (NametagUtils.to2D(vec, scale.get())) {
                    NametagUtils.begin(vec);
                    TextRenderer.get().begin(1.0, false, true);
                    TextRenderer.get().render("Burrowed", -TextRenderer.get().getWidth("Burrowed") / 2.0, 0.0, lineColorTag.get());
                    TextRenderer.get().end();
                    NametagUtils.end();
                }
            }
        }
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mode.get().equals(Mode.Tag)) return;
        for (PlayerEntity entities : mc.world.getPlayers()) {
            if (entities instanceof NPCEntity) continue;
            if (entities == mc.player && ignoreOwn.get()) continue;
            if (Friends.get().isFriend(entities) && ignoreFriend.get()) continue;
            BlockPos pos = entities.getBlockPos();
            if (bu.getBlastResistance(pos) > 599) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    public enum Mode {
        Tag,
        Render
    }
}
