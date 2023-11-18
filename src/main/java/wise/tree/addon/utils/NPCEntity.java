package wise.tree.addon.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class NPCEntity extends OtherClientPlayerEntity {
    public NPCEntity(PlayerEntity player, String name, float health, boolean copyInv) {
        super(mc.world, new GameProfile(player.getUuid(), name));
        copyPositionAndRotation(player);
        prevYaw = getYaw();
        prevPitch = getPitch();
        headYaw = player.headYaw;
        prevHeadYaw = headYaw;
        bodyYaw = player.bodyYaw;
        prevBodyYaw = bodyYaw;
        Byte playerModel = player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
        dataTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
        getAttributes().setFrom(player.getAttributes());
        setPose(player.getPose());
        capeX = getX();
        capeY = getY();
        capeZ = getZ();
        if (health <= 20) {
            setHealth(health);
        } else {
            setHealth(health);
            setAbsorptionAmount(health - 20);
        }
        if (copyInv) getInventory().clone(player.getInventory());
    }




}
