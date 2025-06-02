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

    // New field for 50/50 system: Stores Crate UUID -> Boolean (true if next 5-star is guaranteed featured)
    private final HashMap<UUID, Boolean> limitedBannerGuaranteeStatus = new HashMap<>();

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
        // Ensure the crate entry exists in the pityMap
        return pityMap.computeIfAbsent(crate, k -> new HashMap<>());
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
            // Ensure pity doesn't exceed limit - 1, as limit itself means it's guaranteed.
            // Pity is usually "pulls towards next guarantee".
            setPity(crate, rewardTier, Math.min(getPity(crate, rewardTier) + amt, rewardTier.getPityLimit() > 0 ? rewardTier.getPityLimit() -1 : 0));
        }
    }

    /**
     * Increase pity of all pity enabled reward tiers across a crate except for a specific tier (the one that was hit).
     *
     * @param crate The crate containing the reward tiers
     * @param exceptionTier The RewardTier that was just obtained (and whose pity will be reset elsewhere)
     * @param amt The amount to increase pity by for other tiers
     */
    public void increasePity(Crate crate, RewardTier exceptionTier, int amt) {
        for (RewardTier rewardTier : crate.getRewardTiers()) {
            if (!rewardTier.isPityEnabled() || rewardTier.equals(exceptionTier)) { // Use .equals() for object comparison
                continue;
            }
            // Ensure pity doesn't exceed limit - 1
            setPity(crate, rewardTier, Math.min(getPity(crate, rewardTier) + amt, rewardTier.getPityLimit() > 0 ? rewardTier.getPityLimit() - 1 : 0));
        }
    }

    /**
     * Reset the current pity level to the default value (0)
     *
     * @param crate The Crate containing the specific pity map
     * @param rewardTier The RewardTier to reset the pity of
     */
    public void resetPity(Crate crate, RewardTier rewardTier) {
        // Ensure the crate's pity map exists before trying to put into it
        getPityMap(crate).put(rewardTier, 0);
    }

    /**
     * Set the amount of available pulls for a Crate
     *
     * @param crate The Crate to set available pulls for
     * @param count The new pull balance
     */
    public void setAvailablePulls(Crate crate, int count) {
        pullMap.put(crate, Math.max(0, count)); // Ensure pulls don't go negative
    }

    /**
     * Set the current pity level
     *
     * @param crate The Crate to fetch the pity map of
     * @param rewardTier The RewardTier to set the pity of
     * @param pityLevel The new pity level
     */
    public void setPity(Crate crate, RewardTier rewardTier, int pityLevel) {
        // Ensure the crate's pity map exists
        getPityMap(crate).put(rewardTier, Math.max(0, pityLevel)); // Ensure pity doesn't go negative
    }

    // --- New methods for 50/50 System ---

    /**
     * Checks if the next 5-star pull on a specific limited banner crate is guaranteed to be a featured item.
     *
     * @param crate The limited banner Crate to check.
     * @return True if the next 5-star is guaranteed featured, false otherwise (on 50/50 chance or if not applicable).
     */
    public boolean isNext5StarGuaranteedFeatured(Crate crate) {
        if (crate == null || !crate.isLimited5050Banner()) { // Added check for crate type
            return false; // Not applicable for non-limited 50/50 banners
        }
        return limitedBannerGuaranteeStatus.getOrDefault(crate.getUuid(), false);
    }

    /**
     * Sets the 50/50 guarantee status for a specific limited banner crate.
     * This is typically set to true when a player loses a 50/50.
     *
     * @param crate The limited banner Crate.
     * @param guaranteed True if the next 5-star should be a guaranteed featured item, false otherwise.
     */
    public void setNext5StarGuaranteedFeatured(Crate crate, boolean guaranteed) {
        if (crate == null || !crate.isLimited5050Banner()) { // Added check for crate type
            return; // Not applicable
        }
        limitedBannerGuaranteeStatus.put(crate.getUuid(), guaranteed);
    }

    /**
     * Resets the 50/50 guarantee status for a specific limited banner crate.
     * This is typically called when a player obtains the guaranteed featured 5-star,
     * or if they win the 50/50 and the guarantee shouldn't carry over (design dependent).
     *
     * @param crate The limited banner Crate.
     */
    public void resetLimitedBannerGuarantee(Crate crate) {
        if (crate == null || !crate.isLimited5050Banner()) { // Added check for crate type
            return; // Not applicable
        }
        limitedBannerGuaranteeStatus.put(crate.getUuid(), false);
    }

    /**
     * Gets the raw map of limited banner guarantee statuses.
     * Primarily for PlayerCache to save/load this data.
     * @return The map of Crate UUIDs to their guarantee status.
     */
    public HashMap<UUID, Boolean> getLimitedBannerGuaranteeStatusMap() {
        return limitedBannerGuaranteeStatus;
    }

    /**
     * Sets the raw map of limited banner guarantee statuses.
     * Primarily for PlayerCache to load this data.
     * @param statusMap The map of Crate UUIDs to their guarantee status.
     */
    public void setLimitedBannerGuaranteeStatusMap(HashMap<UUID, Boolean> statusMap) {
        if (statusMap != null) {
            this.limitedBannerGuaranteeStatus.clear();
            this.limitedBannerGuaranteeStatus.putAll(statusMap);
        }
    }
}