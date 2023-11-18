package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import wise.tree.addon.utils.WTModule;

import java.util.List;
import java.util.Random;

public class HitSound extends WTModule {
    public HitSound(Category category, String name) {
        super(category, name);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> random = sgGeneral.add(new BoolSetting.Builder().name("random").defaultValue(false).build());
    private final Setting<List<SoundEvent>> sounds = sgGeneral.add(new SoundEventListSetting.Builder().name("sounds").description("Sounds to block.").build());
    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder().name("ignore-friends").defaultValue(false).build());

    @EventHandler
    private void onAttackEntityEvent(AttackEntityEvent event) {
        if (sounds.get().isEmpty()) return;
        if (!(event.entity instanceof PlayerEntity p)) return;
        if (ignoreFriends.get() && Friends.get().isFriend(p)) return;
        mc.player.playSound(sounds.get().get(random.get() ? getRandom(sounds.get().size()) : 0), 1f, 1f);
    }

    private int getRandom(int max) {
        return new Random().nextInt(max);
    }
}
