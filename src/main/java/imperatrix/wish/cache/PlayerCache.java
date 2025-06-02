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
    private FileConfiguration fileConfiguration;

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
            ConfigurationSection dataSection = fileConfiguration.getConfigurationSection(uuid.toString());

            if (dataSection != null) {
                for (String crateUuid : dataSection.getKeys(false)) {
                    Optional<Crate> crate = plugin.getCrateCache().getCrate(UUID.fromString(crateUuid));
                    ConfigurationSection pityMapSection = dataSection.getConfigurationSection(crateUuid + ".Pity-Map");

                    if (crate.isEmpty()) {
                        continue;
                    }

                    if (pityMapSection != null) {
                        for (String rewardTierName : pityMapSection.getKeys(false)) {
                            Optional<RewardTier> rewardTier = crate.get().getRewardTier(rewardTierName);

                            if (rewardTier.isEmpty()) {
                                continue;
                            }

                            gachaPlayer.setPity(crate.get(), rewardTier.get(), pityMapSection.getInt(rewardTierName, 0));
                        }
                    }

                    gachaPlayer.setAvailablePulls(crate.get(), dataSection.getInt(crateUuid + ".Pulls", 0));
                }
            }

            playerCache.put(uuid, gachaPlayer);
            return gachaPlayer;
        }
    }

    /**
     * Save cached data to file
     *
     * @param customFile The CustomFile to save to
     */
    public void saveTo(CustomFile customFile) {
        for (GachaPlayer gachaPlayer : playerCache.values()) {
            for (Map.Entry<Crate, HashMap<RewardTier, Integer>> entry : gachaPlayer.getPityMap().entrySet()) {
                Crate crate = entry.getKey();

                for (Map.Entry<RewardTier, Integer> pityMap : entry.getValue().entrySet()) {
                    customFile.getConfig().set(gachaPlayer.getUuid().toString() + "." + crate.getUuid().toString() + ".Pity-Map." + pityMap.getKey().getName(), pityMap.getValue());
                }
            }

            for (Crate crate : gachaPlayer.getPullMap().keySet()) {
                customFile.getConfig().set(gachaPlayer.getUuid().toString() + "." + crate.getUuid().toString() + ".Pulls", gachaPlayer.getAvailablePulls(crate));
            }
        }

        customFile.saveConfig();
    }

    /**
     * Set data to cache
     *
     * @param fileConfiguration The FileConfiguration containing cacheable data
     */
    public void setFile(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
    }
}
