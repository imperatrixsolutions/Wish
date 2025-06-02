package imperatrix.wish.cache;

import imperatrix.wish.file.CustomFile;
import org.bukkit.configuration.file.FileConfiguration;

public class GachaConfig {
    public static int MAX_PULLS = 20;

    public static void load(FileConfiguration fileConfiguration) {
        GachaConfig.MAX_PULLS = fileConfiguration.getInt("Max-Pulls", 20);
    }

    public static void validateConfig(ConfigType configType, CustomFile customFile) {
        switch (configType) {
            case MENUS:
                customFile.getConfig().set("Pull-Menu.Max-Pull-Count-Selector-Item", "DIAMOND name:&eMax_Pull_Count lore:&7Click_to_set_your_pull_count_to_the_max");
            case CONFIG, LANG, CRATES:
                break;
        }

        customFile.saveConfig();
    }
}
