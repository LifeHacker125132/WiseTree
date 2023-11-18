package wise.tree.addon.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import wise.tree.addon.WiseTree;
import wise.tree.addon.utils.NPCEntity;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AutoEz  extends Module {
    private final SettingGroup sgEz = settings.getDefaultGroup();
    private final Setting<Mode> mode = sgEz.add(new EnumSetting.Builder<Mode>().name("message-mode").defaultValue(Mode.Default).build());
    private final Setting<List<String>> messages = sgEz.add(new StringListSetting.Builder().name("messages").defaultValue("ez").visible(()->mode.get().equals(Mode.Random)).build());
    private final Setting<String> message = sgEz.add(new StringSetting.Builder().name("message").defaultValue("ez").visible(()->mode.get().equals(Mode.Default)).build());
    private final Setting<Boolean> ignoreFriends = sgEz.add(new BoolSetting.Builder().name("ignore-friends").defaultValue(true).build());
    private final Setting<Boolean> ignoreOwn = sgEz.add(new BoolSetting.Builder().name("ignore-own").defaultValue(true).build());

    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();

    public AutoEz(Category category, String name) {
        super(category, name, "");
    }



    @Override
    public void onActivate() {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;
        Entity entity = p.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity)) return;
        if ((entity.equals(mc.player) && ignoreOwn.get())
            || (Friends.get().isFriend(((PlayerEntity) entity)) && ignoreFriends.get())) return;
        synchronized (totemPopMap) {
            int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
            totemPopMap.put(entity.getUuid(), ++pops);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        synchronized (totemPopMap) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player instanceof NPCEntity) continue;
                if (!totemPopMap.containsKey(player.getUuid())) continue;
                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPopMap.removeInt(player.getUuid());
                    String text = mode.get().equals(Mode.Random) ? messages.get().get(getRandom(messages.get().size())) : message.get();
                    mc.player.networkHandler.sendChatMessage(text
                        .replace("{name}", player.getEntityName())
                        .replace("{pops}", Integer.toString(pops))
                        .replace("{wt}", WiseTree.NAME + " " + WiseTree.ID));
                    chatIdMap.removeInt(player.getUuid());
                }
            }
        }
    }

    private int getRandom(int max) {
        return new Random().nextInt(max);
    }

    public enum Mode {
        Default,
        Random
    }
}
