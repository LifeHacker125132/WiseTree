package wise.tree.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import wise.tree.addon.combat.*;
import wise.tree.addon.misc.*;
import wise.tree.addon.render.*;

public class WiseTree extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String NAME = "WiseTree Hack";
    public static final String ID = "3.6.0";
    public static final Category PVP = new Category("[WT] Combat", Items.END_CRYSTAL.getDefaultStack());
    public static final Category MISC = new Category("[WT] Misc", Items.RED_DYE.getDefaultStack());
    public static final Category RENDER = new Category("[WT] Render", Items.WATER_BUCKET.getDefaultStack());

    @Override
    public void onInitialize() {
        final long initTime = System.currentTimeMillis();

        LOG.info("Start of initialization...");
        LOG.info("init modules...");
        LOG.info(NAME + " " + ID + " started in " + (System.currentTimeMillis() - initTime) + " ms.");

        Modules.get().add(new AntiAnchor(PVP, "[WT] Anti-Anchor"));
        Modules.get().add(new PistonCrystal(PVP, "[WT] Piston-Aura"));
        Modules.get().add(new AutoCrystal(PVP, "[WT] Auto-Crystal"));
        Modules.get().add(new AutoCity(PVP, "[WT] Auto-City"));
        Modules.get().add(new Surround(PVP, "[WT] Surround"));
        Modules.get().add(new BowRelease(PVP, "[WT] Bow-Release"));
        Modules.get().add(new TntAura(PVP, "[WT] Tnt-Aura"));
        Modules.get().add(new BedBomb(PVP, "[WT] Bed-Bomb"));
        Modules.get().add(new AntiBed(PVP, "[WT] Anti-Bed"));
        Modules.get().add(new BedLay(PVP, "[WT] Bed-Lay"));
        Modules.get().add(new Strafe(PVP, "[WT] Strafe"));
        Modules.get().add(new PistonPush(PVP, "[WT] Piston-Push"));
        Modules.get().add(new AutoEz(MISC, "[WT] Auto-Ez"));
        Modules.get().add(new NoBreakReset(MISC, "[WT] No-Break-Reset"));
        Modules.get().add(new BurrowEsp(MISC, "[WT] Burrow-Esp"));

        Modules.get().add(new ChatTweaks(MISC, "[WT] Chat-Tweaks"));
        Modules.get().add(new ArmorAlert(MISC, "[WT] Armor-Alert"));
        Modules.get().add(new ElytraFly(MISC, "[WT] Elytra-Fly"));

        Modules.get().add(new ElytraJump(MISC, "[WT] Elytra-Jump"));
        Modules.get().add(new DeathInfo(MISC, "[WT] Death-Info"));
        Modules.get().add(new ChestTracker(MISC, "[WT] Chest-Tracker"));
        Modules.get().add(new SilentUse(MISC, "[WT] Silent-Use"));
        Modules.get().add(new MultiTask(MISC, "[WT] Multi-Task"));
        Modules.get().add(new NoRotate(MISC, "[WT] No-Rotate"));
        Modules.get().add(new HitSound(MISC, "[WT] Hit-Sounds"));
        Modules.get().add(new AutoDM(MISC, "[WT] Auto-DM"));
        Modules.get().add(new AutoMine(MISC, "[WT] Auto-Mine"));
        Modules.get().add(new Prefix(MISC, "[WT] Prefix"));
        Modules.get().add(new FrameDupe(MISC, "[WT] Frame-Dupe"));

        Modules.get().add(new TpaSpammer(MISC, "[WT] Tpa-Spammer"));
        Modules.get().add(new BedChams(RENDER, "[WT] Bed-Chams"));
        Modules.get().add(new HUE(RENDER, "[WT] HUE"));
        Modules.get().add(new BlockSelection(RENDER, "[WT] Block-Selection"));
        Modules.get().add(new ItemGlint(RENDER, "[WT] Item-Glint"));
        Modules.get().add(new MineProgress(RENDER, "[WT] Mine-Progress"));
        Modules.get().add(new VisualRange(RENDER, "[WT] Visual-Range"));
        Modules.get().add(new NoHurtCam(RENDER, "[WT] No-Hurt-Cam"));
        Modules.get().add(new CustomFov(RENDER, "[WT] Custom-Fov"));
        Modules.get().add(new NoSwing(RENDER, "[WT] No-Swing"));
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(PVP);
        Modules.registerCategory(MISC);
        Modules.registerCategory(RENDER);
    }
    @Override
    public String getPackage() {
        return "wise.tree.addon";
    }

}
