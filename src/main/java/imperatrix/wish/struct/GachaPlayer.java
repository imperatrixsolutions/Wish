package imperatrix.wish.struct;

import imperatrix.wish.struct.crate.Crate;
import imperatrix.wish.struct.reward.RewardTier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

public class GachaPlayer {
    private final HashMap<Crate, HashMap<RewardTier, Integer>> pityMap = new HashMap<>();
    private final HashMap<Crate, Integer> pullMap = new HashMap<>();
    private final UUID uuid;

    public GachaPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Get the amount of pulls available for a crate
     *
     * @param crate The Crate to retrieve available pulls of
     * @return Available pull count as int
     */
    public int getAvailablePulls(Crate crate) {
        return pullMap.getOrDefault(crate, 0);
    }

    /**
     * Retrieve the current pity level
     *
     * @param crate The Crate to fetch the pity map of
     * @param rewardTier The RewardTier to get the pity of
     * @return Current pity level as int, defaults to 0
     */
    public int getPity(Crate crate, RewardTier rewardTier) {
        return getPityMap(crate).getOrDefault(rewardTier, 0);
    }

    /**
     * Retrieve the complete pity map
     *
     * @return HashMap with Crate as the key and specific pity map for the crate as the value
     */
    public HashMap<Crate, HashMap<RewardTier, Integer>> getPityMap() {
        return pityMap;
    }

    /**
     * Retrieve a specific pity map
     *
     * @param crate The crate to fetch the pity map of
     * @return HashMap with RewardTier as the value and current pity level as the value
     */
    @Nonnull
    public HashMap<RewardTier, Integer> getPityMap(Crate crate) {
        if (!pityMap.containsKey(crate)) {
            pityMap.put(crate, new HashMap<>());
        }

        return pityMap.get(crate);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public HashMap<Crate, Integer> getPullMap() {
        return pullMap;
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * Increase pity of all pity enabled reward tiers across a crate
     *
     * @param crate The crate containing the reward tiers
     * @param amt The amount to increase pity by
     */
    public void increasePity(Crate crate, int amt) {
        for (RewardTier rewardTier : crate.getRewardTiers()) {
            if (!rewardTier.isPityEnabled()) {
                continue;
            }

            setPity(crate, rewardTier, Math.min(getPity(crate, rewardTier) + amt, rewardTier.getPityLimit() - 1));
        }
    }

    /**
     * Increase pity of all pity enabled reward tiers across a crate except for a specific crate
     *
     * @param crate The crate containing the reward tiers
     * @param exception The
     * @param amt The amount to increase pity by
     */
    public void increasePity(Crate crate, RewardTier exception, int amt) {
        for (RewardTier rewardTier : crate.getRewardTiers()) {
            if (!rewardTier.isPityEnabled() || rewardTier == exception) {
                continue;
            }

            setPity(crate, rewardTier, Math.min(getPity(crate, rewardTier) + amt, rewardTier.getPityLimit() - 1));
        }
    }

    /**
     * Reset the current pity level to the default value (0)
     *
     * @param crate The Crate containing the specific pity map
     * @param rewardTier The RewardTier to reset the pity of
     */
    public void resetPity(Crate crate, RewardTier rewardTier) {
        getPityMap(crate).put(rewardTier, 0);
    }

    /**
     * Set the amount of available pulls for a Crate
     *
     * @param crate The Crate to set available pulls for
     * @param count The new pull balance
     */
    public void setAvailablePulls(Crate crate, int count) {
        pullMap.put(crate, count);
    }

    /**
     * Set the current pity level
     *
     * @param crate The Crate to fetch the pity map of
     * @param rewardTier The RewardTier to set the pity of
     */
    public void setPity(Crate crate, RewardTier rewardTier, int pityLevel) {
        getPityMap(crate).put(rewardTier, pityLevel);
    }
}