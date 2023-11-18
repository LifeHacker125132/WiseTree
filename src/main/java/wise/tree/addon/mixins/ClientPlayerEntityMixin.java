package wise.tree.addon.mixins;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wise.tree.addon.events.SwingHandEvent;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
    @Shadow
    private ClientPlayNetworkHandler networkHandler;

    public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void swingHand(Hand hand) {
        SwingHandEvent event = new SwingHandEvent(hand);
        MeteorClient.EVENT_BUS.post(event);
        if (!event.isCancelled()) {
            super.swingHand(event.getHand());
        }
        networkHandler.sendPacket(new HandSwingC2SPacket(hand));
    }
}
