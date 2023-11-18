package wise.tree.addon.mixins;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wise.tree.addon.utils.NPCEntity;

import java.util.List;

@Mixin(CrystalAura.class)
public class CrystalAuraMixin {
    @Shadow
    @Final
    private List<PlayerEntity> targets;
    @Shadow
    @Final
    private Setting<Boolean> smartDelay;
    @Shadow
    @Final
    private Setting<Boolean> predictMovement;
    @Shadow
    @Final
    private Setting<Boolean> ignoreTerrain;
    @Shadow
    private double bestTargetDamage;
    @Shadow
    private PlayerEntity bestTarget;
    @Shadow
    private int bestTargetTimer;

    /**
     * @author
     * @reason
     */
    @Overwrite
    private PlayerEntity getNearestTarget() {
        PlayerEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        for (PlayerEntity target : targets) {
            if (target instanceof NPCEntity) continue;
            double distance = PlayerUtils.squaredDistanceTo(target);
            if (distance < nearestDistance) {
                nearestTarget = target;
                nearestDistance = distance;
            }
        }
        return nearestTarget;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private double getDamageToTargets(Vec3d vec3d, BlockPos obsidianPos, boolean breaking, boolean fast) {
        double damage = 0;
        if (fast) {
            PlayerEntity target = getNearestTarget();
            if (!(smartDelay.get() && breaking && target.hurtTime > 0))
                damage = DamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos, ignoreTerrain.get());
        } else {
            for (PlayerEntity target : targets) {
                if (target instanceof NPCEntity) continue;
                if (smartDelay.get() && breaking && target.hurtTime > 0) continue;
                double dmg = DamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos, ignoreTerrain.get());
                if (dmg > bestTargetDamage) {
                    bestTarget = target;
                    bestTargetDamage = dmg;
                    bestTargetTimer = 10;
                }
                damage += dmg;
            }
        }
        return damage;
    }
}
