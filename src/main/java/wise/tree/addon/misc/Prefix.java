package wise.tree.addon.misc;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import wise.tree.addon.utils.Wrapper;

public class Prefix extends Module implements Wrapper {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<PrefixMode> mode = sgGeneral.add(new EnumSetting.Builder<PrefixMode>().name("prefix-mode").defaultValue(PrefixMode.All).build());
    private final Setting<Boolean> customPrefix = sgGeneral.add(new BoolSetting.Builder().name("custom-prefix").defaultValue(false).build());
    private final Setting<String> prefixText = sgGeneral.add(new StringSetting.Builder().name("text").defaultValue("WiseTree").visible(customPrefix::get).build());
    private final Setting<Boolean> customPrefixColor = sgGeneral.add(new BoolSetting.Builder().name("custom-color").defaultValue(false).build());
    private final Setting<SettingColor> prefixColor = sgGeneral.add(new ColorSetting.Builder().name("color").visible(customPrefixColor::get).build());
    private final Setting<Boolean> customBracketsColor = sgGeneral.add(new BoolSetting.Builder().name("custom-brackets-color").defaultValue(false).build());
    private final Setting<SettingColor> bracketsColor = sgGeneral.add(new ColorSetting.Builder().name("brackets-color").visible(customBracketsColor::get).build());
    private final Setting<Boolean> customBrackets = sgGeneral.add(new BoolSetting.Builder().name("custom-brackets").defaultValue(false).build());
    private final Setting<String> leftBracket = sgGeneral.add(new StringSetting.Builder().name("left-bracket").defaultValue("[").visible(customBrackets::get).build());
    private final Setting<String> rightBracket = sgGeneral.add(new StringSetting.Builder().name("right-bracket").defaultValue("] ").visible(customBrackets::get).build());

    public enum PrefixMode {
        All,
        WiseTree
    }

    public Prefix(Category category, String name) {
        super(category, name, "");
    }

    @Override
    public void onActivate() {
        switch (mode.get()){
            case All -> {
                ChatUtils.registerCustomPrefix("meteordevelopment.meteorclient", this::getPrefix);
                ChatUtils.registerCustomPrefix("wise.tree.addon", this::getPrefix);
            }

            case WiseTree -> ChatUtils.registerCustomPrefix("wise.tree.addon", this::getPrefix);
        }
    }


    public Text getPrefix() {
        MutableText l = MutableText.of(new LiteralTextContent(""));
        MutableText bl = MutableText.of(new LiteralTextContent(""));
        MutableText br = MutableText.of(new LiteralTextContent(""));
        MutableText prefix = MutableText.of(new LiteralTextContent(""));
        String logo, bracketsLeft, bracketsRight;
        if (customPrefix.get()) logo = prefixText.get();
        else logo = "WiseTree";
        if (customPrefixColor.get())
            l.append(Text.literal(logo).setStyle(l.getStyle().withColor(TextColor.fromRgb(prefixColor.get().getPacked()))));
        else {
            l.append(logo);
            l.setStyle(l.getStyle().withFormatting(Formatting.GREEN));
        }
        if (customBrackets.get()) {
            bracketsLeft = leftBracket.get();
            bracketsRight = rightBracket.get();
        } else {
            bracketsLeft = "[";
            bracketsRight = "] ";
        }
        if (customBracketsColor.get()) {
            bl.append(Text.literal(bracketsLeft).setStyle(l.getStyle().withColor(TextColor.fromRgb(bracketsColor.get().getPacked()))));
            br.append(Text.literal(bracketsRight).setStyle(l.getStyle().withColor(TextColor.fromRgb(bracketsColor.get().getPacked()))));
        } else {
            bl.append(bracketsLeft);
            br.append(bracketsRight);
            bl.setStyle(bl.getStyle().withColor(Formatting.GRAY));
            br.setStyle(bl.getStyle().withColor(Formatting.GRAY));
        }
        prefix.append(bl);
        prefix.append(l);
        prefix.append(br);
        return prefix;
    }
}
