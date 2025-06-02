package imperatrix.wish.cache;

import imperatrix.wish.file.CustomFile;
import imperatrix.wish.struct.crate.Crate;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class CrateCache {
    private final List<Crate> crates = new ArrayList<>();

    /**
     * Retrieve a crate based on a location
     *
     * @param location The location to check for
     * @return Optional Crate
     */
    public Optional<Crate> getCrate(Location location) {
        return crates.stream().filter((c) -> c.isCrateLocation(location)).findFirst();
    }

    /**
     * Retrieve a crate based on its name
     *
     * @param name The name to search for
     * @return Optional Crate
     */
    public Optional<Crate> getCrate(String name) {
        return crates.stream().filter((c) -> c.getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Retrieve a crate based on its uuid
     *
     * @param uuid The uuid to search for
     * @return Optional Crate
     */
    public Optional<Crate> getCrate(UUID uuid) {
        return crates.stream().filter((c) -> c.getUuid().equals(uuid)).findFirst();
    }

    /**
     * Retrieve all crates
     *
     * @return List containing all loaded crates
     */
    public List<Crate> getCrates() {
        return crates;
    }

    /**
     * Load all crates from a config file
     *
     * @param config The config to load settings from
     */
    public void loadFrom(FileConfiguration config) {
        ConfigurationSection cratesSection = config.getConfigurationSection("Crates");

        if (cratesSection == null) {
            return;
        }

        for (String crateName : cratesSection.getKeys(false)) {
            Crate crate = new Crate(crateName);
            ConfigurationSection crateSection = cratesSection.getConfigurationSection(crateName);

            assert crateSection != null;
            crate.loadFrom(crateSection);
            crates.add(crate);
        }
    }

    /**
     * Save cached data to file
     *
     * @param customFile The CustomFile to save to
     */
    public void saveTo(CustomFile customFile) {
        for (Crate crate : crates) {
            List<String> locations = new ArrayList<>();

            for (Location location : crate.getCrateLocations()) {
                if (location.getWorld() == null) {
                    continue;
                }

                locations.add(location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
            }

            customFile.getConfig().set("Crates." + crate.getName() + ".Locations", locations);
        }

        customFile.saveConfig();
    }
}
