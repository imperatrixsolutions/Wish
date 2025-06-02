package imperatrix.wish.cache;

import imperatrix.wish.Wish;
import imperatrix.wish.file.CustomFile;
import imperatrix.wish.struct.crate.Crate;
import imperatrix.wish.struct.GachaPlayer;
import imperatrix.wish.struct.reward.RewardTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerCache {
    private final HashMap<UUID, GachaPlayer> playerCache = new HashMap<>();
    private final Wish plugin;
    private FileConfiguration fileConfiguration; // This is the data.yml configuration

    public PlayerCache(Wish plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a GachaPlayer based on UUID, if none is found, a new one will be created and data will attempt to load
     *
     * @param uuid The GachaPlayer UUID
     * @return GachaPlayer
     */
    public GachaPlayer getPlayer(UUID uuid) {
        if (playerCache.containsKey(uuid)) {
            return playerCache.get(uuid);
        } else {
            GachaPlayer gachaPlayer = new GachaPlayer(uuid);
            // Ensure fileConfiguration is not null before proceeding
            if (fileConfiguration == null) {
                // This might happen if setFile was not called, or data.yml failed to load.
                // Log an error or handle appropriately. For now, we'll return the new gachaPlayer.
                // plugin.getLogger().warning("[Wish] PlayerCache: fileConfiguration is null while trying to load player data for " + uuid);
                playerCache.put(uuid, gachaPlayer); // Cache the new, empty player
                return gachaPlayer;
            }

            ConfigurationSection dataSection = fileConfiguration.getConfigurationSection(uuid.toString());

            if (dataSection != null) {
                // Iterate through each crate UUID stored for the player
                for (String crateUuidString : dataSection.getKeys(false)) {
                    // Skip if this key is for our new combined 50/50 status map,
                    // as that was the old plan. New plan is per-crate.
                    // This check might not be strictly necessary with the new structure but is harmless.
                    if (crateUuidString.equals("LimitedBannerGuarantees")) {
                        continue;
                    }

                    UUID crateUuid;
                    try {
                        crateUuid = UUID.fromString(crateUuidString);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Wish] Invalid Crate UUID string in data.yml for player " + uuid + ": " + crateUuidString);
                        continue;
                    }

                    Optional<Crate> optionalCrate = plugin.getCrateCache().getCrate(crateUuid);
                    if (optionalCrate.isEmpty()) {
                        // This crate might no longer exist in crates.yml, skip its data.
                        // plugin.getLogger().info("[Wish] Crate with UUID " + crateUuidString + " not found in cache while loading player data for " + uuid + ". Skipping.");
                        continue;
                    }
                    Crate crate = optionalCrate.get();
                    ConfigurationSection crateDataSection = dataSection.getConfigurationSection(crateUuidString);

                    if (crateDataSection == null) continue;

                    // Load Pity Map for this crate
                    ConfigurationSection pityMapSection = crateDataSection.getConfigurationSection("Pity-Map");
                    if (pityMapSection != null) {
                        for (String rewardTierName : pityMapSection.getKeys(false)) {
                            Optional<RewardTier> optionalRewardTier = crate.getRewardTier(rewardTierName);
                            if (optionalRewardTier.isEmpty()) {
                                // This reward tier might no longer exist in the crate, skip.
                                // plugin.getLogger().info("[Wish] RewardTier '" + rewardTierName + "' not found in crate '" + crate.getName() + "' while loading player data. Skipping pity.");
                                continue;
                            }
                            RewardTier rewardTier = optionalRewardTier.get();
                            gachaPlayer.setPity(crate, rewardTier, pityMapSection.getInt(rewardTierName, 0));
                        }
                    }

                    // Load Pulls for this crate
                    gachaPlayer.setAvailablePulls(crate, crateDataSection.getInt("Pulls", 0));

                    // New: Load 50/50 Guarantee Status for this crate (if applicable)
                    if (crate.isLimited5050Banner() && crateDataSection.contains("LimitedBannerGuarantee")) {
                        boolean guaranteeStatus = crateDataSection.getBoolean("LimitedBannerGuarantee");
                        gachaPlayer.setNext5StarGuaranteedFeatured(crate, guaranteeStatus);
                    }
                }
            }
            playerCache.put(uuid, gachaPlayer);
            return gachaPlayer;
        }
    }

    /**
     * Save cached data to file
     *
     * @param customFile The CustomFile (data.yml) to save to
     */
    public void saveTo(CustomFile customFile) {
        if (customFile == null || customFile.getConfig() == null) {
            plugin.getLogger().severe("[Wish] PlayerCache: Cannot save player data, CustomFile or its config is null!");
            return;
        }
        FileConfiguration configToSave = customFile.getConfig();

        for (GachaPlayer gachaPlayer : playerCache.values()) {
            String playerUuidString = gachaPlayer.getUuid().toString();

            // Save Pity Map
            for (Map.Entry<Crate, HashMap<RewardTier, Integer>> pityEntry : gachaPlayer.getPityMap().entrySet()) {
                Crate crate = pityEntry.getKey();
                String crateUuidString = crate.getUuid().toString();
                for (Map.Entry<RewardTier, Integer> tierPity : pityEntry.getValue().entrySet()) {
                    configToSave.set(playerUuidString + "." + crateUuidString + ".Pity-Map." + tierPity.getKey().getName(), tierPity.getValue());
                }
            }

            // Save Pull Map
            for (Map.Entry<Crate, Integer> pullEntry : gachaPlayer.getPullMap().entrySet()) {
                Crate crate = pullEntry.getKey();
                String crateUuidString = crate.getUuid().toString();
                configToSave.set(playerUuidString + "." + crateUuidString + ".Pulls", pullEntry.getValue());
            }

            // New: Save 50/50 Guarantee Status Map
            // This map in GachaPlayer stores UUID of Crate -> Boolean status
            for (Map.Entry<UUID, Boolean> guaranteeEntry : gachaPlayer.getLimitedBannerGuaranteeStatusMap().entrySet()) {
                UUID crateUuid = guaranteeEntry.getKey();
                Boolean status = guaranteeEntry.getValue();

                // We only want to save this if the crate is actually a limited 50/50 banner
                // However, GachaPlayer's map might contain entries for crates that were once limited but no longer are.
                // It's safer to just save what's in the map. The Crate object itself (from CrateCache)
                // will determine if it *acts* as a 50/50 banner during runtime based on its current config.
                // Or, ensure GachaPlayer only stores statuses for crates that are *currently* configured as such.
                // For simplicity in saving, we save what's tracked. Loading logic will be selective.
                configToSave.set(playerUuidString + "." + crateUuid.toString() + ".LimitedBannerGuarantee", status);
            }
        }
        customFile.saveConfig();
    }

    /**
     * Set data file configuration to cache from and save to.
     * This should be the FileConfiguration for data.yml.
     *
     * @param fileConfiguration The FileConfiguration containing cacheable data
     */
    public void setFile(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
    }
}