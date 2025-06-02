package imperatrix.wish.file;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class FileManager {
    private final HashMap<String, CustomFile> fileHashMap = new HashMap<>();
    private final JavaPlugin plugin;

    public FileManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a custom file
     *
     * @param name The name of the custom file, case insensitive
     * @return An existing customfile if in the cache, or a new customfile
     */
    public CustomFile getFile(String name) {
        CustomFile customFile = null;

        for (String key : fileHashMap.keySet()) {
            if (!key.equalsIgnoreCase(name)) {
                continue;
            }

            customFile = fileHashMap.get(name);
        }

        if (customFile == null) {
            customFile = new CustomFile(plugin, name);
            fileHashMap.put(name, customFile);
        }

        return customFile;
    }

    /**
     * Reload all registered files
     */
    public void reloadAllFiles() {
        fileHashMap.values().forEach(CustomFile::reloadConfig);
    }

    /**
     * Save all registered files
     */
    public void saveAllFiles() {
        fileHashMap.values().forEach(CustomFile::saveConfig);
    }
}
