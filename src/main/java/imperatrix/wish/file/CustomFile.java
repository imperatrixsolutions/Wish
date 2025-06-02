package imperatrix.wish.file;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class CustomFile {
    private final JavaPlugin plugin;
    private final String fileName;
    private File file = null;
    private FileConfiguration configFile = null;

    public CustomFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public FileConfiguration getConfig() {
        if (configFile == null) {
            reloadConfig();
        }
        return configFile;
    }

    public void reloadConfig() {
        if (configFile == null) {
            file = new File(plugin.getDataFolder(), fileName + ".yml");
        }
        configFile = YamlConfiguration.loadConfiguration(file);
        InputStream inputStream = plugin.getResource(fileName + ".yml");

        if (inputStream != null) {
            Reader defConfigStream = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            configFile.setDefaults(defConfig);
        }
    }

    public void saveConfig() {
        if (configFile == null || file == null) {
            return;
        }
        try {
            getConfig().save(file);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save config to " + configFile);
        }
    }

    public void saveDefaultConfig() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), fileName + ".yml");
        }
        if (!file.exists()) {
            plugin.saveResource(fileName + ".yml", false);
        }
    }
}