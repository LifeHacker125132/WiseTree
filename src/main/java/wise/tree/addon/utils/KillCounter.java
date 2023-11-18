package wise.tree.addon.utils;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;

public class KillCounter {
    private static final ArrayList<PlayerEntity> KILLED = new ArrayList<>();

    public static boolean contains(PlayerEntity entity) {
        return KILLED.contains(entity);
    }

    public static void put(PlayerEntity entity) {
        KILLED.add(entity);
    }

    public static void remove(PlayerEntity entity) {
        KILLED.remove(entity);
    }

    public static int peek() {
        return KILLED.size();
    }

    public static ArrayList<PlayerEntity> get() {
        return KILLED;
    }
}
