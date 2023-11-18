package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import wise.tree.addon.utils.NPCEntity;
import wise.tree.addon.utils.WTModule;

import java.util.List;
import java.util.Random;

public class Mode2 extends WTModule {
    public Mode2(Category category, String name) {
        super(category, name);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(25).sliderRange(0, 100).build());
    private final Setting<List<String>> message = sgGeneral.add(new StringListSetting.Builder().name("messages").build());
    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder().name("ignore-friends").defaultValue(true).build());
    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder().name("anti-spam").defaultValue(true).build());

    private int wait = delay.get();

    @EventHandler
    private void onTickEvent(TickEvent.Pre event) {
        if (wait > 0) {
            wait--;
        }
    }

    @EventHandler
    private void onAttackEntityEvent(AttackEntityEvent event) {
        if (event.entity instanceof NPCEntity) return;
        if (message.get().isEmpty()) return;
        if (!(event.entity instanceof PlayerEntity player)) return;
        if (wait <= 0 && isTarget(player)) {
            String mess = (message.get().get(getRandom(message.get().size()))) + (bypass.get() ? " : "  + new Random().nextInt(999) : "");
            mc.player.networkHandler.sendChatMessage(mess.replace("{name}", event.entity.getEntityName()));
            wait = delay.get();
        }
    }


    private int getRandom(int max) {
        return new Random().nextInt(max);
    }

    private boolean isTarget(Entity entity) {
        return !ignoreFriends.get() || !Friends.get().isFriend((PlayerEntity) entity);
    }
}
