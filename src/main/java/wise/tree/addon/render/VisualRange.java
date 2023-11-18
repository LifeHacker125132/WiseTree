package wise.tree.addon.render;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.NPCEntity;
import wise.tree.addon.utils.Wrapper;

public class VisualRange extends Module implements Wrapper {
    public VisualRange(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<EventMode> eventMode = sgGeneral.add(new EnumSetting.Builder<EventMode>().name("event-mode").defaultValue(EventMode.Added).build());
    private final Setting<AlertMode> alertType = sgGeneral.add(new EnumSetting.Builder<AlertMode>().name("alert-mode").defaultValue(AlertMode.Toast).build());
    private final Setting<MessageMode> messageMode = sgGeneral.add(new EnumSetting.Builder<MessageMode>().name("message-mode").defaultValue(MessageMode.Simple).visible(() -> alertType.get().equals(AlertMode.Message)).build());
    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder().name("friends").defaultValue(true).build());
    private final Setting<Boolean> coordinate = sgGeneral.add(new BoolSetting.Builder().name("coordinate").defaultValue(true).build());

    @EventHandler
    private void onAddedEntity(EntityAddedEvent event) {
        if (!(event.entity instanceof PlayerEntity) || eventMode.get().equals(EventMode.Remove) || event.entity.getUuid().equals(mc.player.getUuid())) return;
        if (event.entity instanceof NPCEntity) return;
        if (alertType.get().equals(AlertMode.Message)) {
            info(getInfo((PlayerEntity) event.entity, friends.get(), coordinate.get(), true));
        } else if (alertType.get().equals(AlertMode.Toast)) {
            if (!friends.get() && Friends.get().isFriend((PlayerEntity) event.entity)) return;
            mc.getToastManager().add(new MeteorToast(Items.GOAT_HORN, title, event.entity.getEntityName() + " entered".formatted(Formatting.DARK_GREEN)));
        }
    }

    @EventHandler
    private void onRemoveEntity(EntityRemovedEvent event) {
        if (!(event.entity instanceof PlayerEntity) || eventMode.get().equals(EventMode.Added) || event.entity.getUuid().equals(mc.player.getUuid())) return;
        if (event.entity instanceof NPCEntity) return;
        if (alertType.get().equals(AlertMode.Message)) {
            info(getInfo((PlayerEntity) event.entity, friends.get(), coordinate.get(), false));
        } else if (alertType.get().equals(AlertMode.Toast)) {
            if (!friends.get() && Friends.get().isFriend((PlayerEntity) event.entity)) return;
            mc.getToastManager().add(new MeteorToast(Items.GOAT_HORN, title, event.entity.getEntityName() + " left".formatted(Formatting.DARK_GREEN)));
        }
    }

    private String getInfo(PlayerEntity entity, boolean friends, boolean coords, boolean added) {
        if (entity == null) return null;
        if (entity instanceof NPCEntity) return null;
        int x = (int)entity.getX();
        int y = (int)entity.getY();
        int z = (int)entity.getZ();

        if (friends && Friends.get().isFriend(entity)) return added ? coords
            ? getFormat(true, true) + getName(entity) + getMessage(true) + getPos(x,y,z)
            : getFormat(true, true) + getName(entity) + getMessage(true)
            : coords ? getFormat(true, false) + getName(entity) + getMessage(false) + getPos(x,y,z)
            : getFormat(true, false) + getName(entity) + getMessage(false);
        return added ? coords
            ? getFormat(false, true) + getName(entity) + getMessage(true) + getPos(x,y,z)
            : getFormat(false, true) + getName(entity) + getMessage(true)
            : coords ? getFormat(false, false) + getName(entity) + getMessage(false) + getPos(x,y,z)
            : getFormat(false, false) + getName(entity) + getMessage(false);
    }

    private String getName(PlayerEntity entity) {
        return entity.getEntityName();
    }

    private String getMessage(boolean added) {
        return Formatting.GRAY + switch (messageMode.get()) {
            case Full -> added ? " added your visual range." : " removed your visual range.";
            case Simple -> added ? " added." : " removed.";
            case Small -> added ? " +." : " -.";
            case VerySmall -> ".";
        };
    }

    private Formatting getFormat(int x, int y, int z) {
        int distance = (int)PlayerUtils.distanceTo(new BlockPos(x, y, z));
        return distance >= 128 ? Formatting.GREEN : distance >= 64 ? Formatting.DARK_GREEN : Formatting.RED;
    }

    private String getPos(int x, int y, int z) {
        return getFormat(x,y,z) + " XYZ: " + x + ", " + y + ", " + z + ";";
    }

    private Formatting getFormat(boolean friends, boolean added) {
        return switch (messageMode.get()) {
            case Simple, Full, Small -> friends ? Formatting.GREEN : Formatting.RED;
            case VerySmall -> friends ? added ? Formatting.GREEN : Formatting.DARK_GREEN : added ? Formatting.RED : Formatting.DARK_RED;
        };
    }

    public enum MessageMode {
        Simple,
        Full,
        Small,
        VerySmall
    }

    public enum AlertMode {
        Toast,
        Message
    }

    public enum EventMode {
        Added,
        Remove,
        Both
    }
}
