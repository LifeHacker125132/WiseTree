package wise.tree.addon.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wise.tree.addon.render.ItemGlint;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract boolean hasEnchantments();

    @Shadow
    public abstract Item getItem();

    /**
     * @author me
     * @reason o_0
     */
    @Overwrite
    public boolean hasGlint() {
        return (Modules.get().get(ItemGlint.class).isActive() && Modules.get().get(ItemGlint.class).items.get().contains(getItem())) || hasEnchantments();
    }
}
