package imperatrix.wish.struct.crate;

import imperatrix.wish.struct.reward.Reward;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.Collection; // Added this import
import java.util.HashMap;
import java.util.UUID; // Added this import

public class CrateSession {
    private final UUID playerUuid;
    private Crate crate;
    private Location crateLocation;
    private HashMap<Integer, Reward> rewards = new HashMap<>(); // Initialized to prevent NullPointerException if accessed before set
    private CrateOpenPhase openPhase = CrateOpenPhase.INACTIVE;

    // New field for storing end locations during physical animations
    private HashMap<Integer, Location> physicalAnimationEndLocationMap;

    public CrateSession(UUID playerUuid, Crate crate, Location crateLocation) {
        this.playerUuid = playerUuid;
        this.crate = crate;
        this.crateLocation = crateLocation;
    }

    public void clearRewards() {
        rewards.clear();
    }

    public Crate getCrate() {
        return crate;
    }

    public Location getCrateLocation() {
        return crateLocation;
    }

    public CrateOpenPhase getOpenPhase() {
        return openPhase;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @Nullable
    public Reward getReward(int slotNumber) {
        return rewards.get(slotNumber);
    }

    public Collection<Reward> getRewards() {
        return rewards.values();
    }

    public void setCrate(Crate crate) {
        this.crate = crate;
    }

    public void setCrateLocation(Location crateLocation) {
        this.crateLocation = crateLocation;
    }

    public void setOpenPhase(CrateOpenPhase openPhase) {
        this.openPhase = openPhase;
    }

    public void setRewards(HashMap<Integer, Reward> rewards) {
        this.rewards = rewards;
    }

    // --- Methods for Physical Animation End Location Map ---

    /**
     * Gets the map storing end locations for particles during physical animations.
     * Key is typically an index (e.g., pull number), Value is the Location.
     * @return The map of end locations, or null if not set.
     */
    public HashMap<Integer, Location> getPhysicalAnimationEndLocationMap() {
        return physicalAnimationEndLocationMap;
    }

    /**
     * Sets the map storing end locations for particles during physical animations.
     * @param map The map of end locations.
     */
    public void setPhysicalAnimationEndLocationMap(HashMap<Integer, Location> map) {
        this.physicalAnimationEndLocationMap = map;
    }

    /**
     * Clears the stored physical animation end location map.
     * Should be called when the session ends or this data is no longer needed
     * to free up memory.
     */
    public void clearPhysicalAnimationData() {
        if (this.physicalAnimationEndLocationMap != null) {
            this.physicalAnimationEndLocationMap.clear();
        }
        this.physicalAnimationEndLocationMap = null;
    }
}