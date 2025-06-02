package imperatrix.wish.struct.crate;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.menu.menus.CrateOpenMenu;
import imperatrix.wish.menu.Menu;
import imperatrix.wish.struct.GachaPlayer;
import imperatrix.wish.struct.reward.Reward;
import imperatrix.wish.struct.reward.RewardTier;
import imperatrix.wish.util.MathUtil;
import imperatrix.wish.util.ParticleUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors; // Added for stream operations

public class Crate {
    private final LinkedHashMap<RewardTier, Double> rewardProbabilityMap = new LinkedHashMap<>();
    private final String name;
    private UUID uuid;
    private AnimationType animationType;
    private final Set<Location> crateLocations = new HashSet<>();
    private final HashMap<Location, Boolean> inUse = new HashMap<>();

    // New fields for 50/50 system
    private boolean isLimited5050Banner = false;
    private List<String> featured5StarRewardNames = new ArrayList<>();
    private String fiveStarTierKeyName = "five-star"; // Default key name for the 5-star tier, can be made configurable
    private RewardTier resolvedFiveStarTier; // To store the actual 5-star tier object for this crate

    public Crate(String name) {
        this.name = name;
    }

    public void addLocation(Location location) {
        crateLocations.add(location);
    }

    /**
     * Generate a random reward tier based on set probability
     *
     * @return Generated RewardTier
     * @deprecated Use generateRewardTier(GachaPlayer player) for pity considerations
     */
    @Deprecated
    public RewardTier generateRewardTier() {
        double randDouble = Math.random();

        for (Map.Entry<RewardTier, Double> rewardProbability : rewardProbabilityMap.entrySet()) {
            if (randDouble <= rewardProbability.getValue()) { // This original logic for sorting might need re-evaluation if chances aren't cumulative
                return rewardProbability.getKey();
            }
        }
        // Fallback if map is empty or chances don't sum up correctly - should be handled by proper config.
        // The original sortProbabilityMap sorts by value, which means lower chances come first.
        // The logic should be: sum chances and check if randDouble falls within a tier's segment.
        // Corrected logic for random selection based on cumulative probability:
        double cumulativeProbability = 0.0;
        for (Map.Entry<RewardTier, Double> entry : rewardProbabilityMap.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randDouble <= cumulativeProbability) {
                return entry.getKey();
            }
        }
        return rewardProbabilityMap.isEmpty() ? null : rewardProbabilityMap.keySet().iterator().next(); // Should ideally not happen
    }


    /**
     * Generate a random reward tier based on set probability and pity for a player
     *
     * @return Generated RewardTier
     */
    public RewardTier generateRewardTier(GachaPlayer gachaPlayer) {
        // Check for hard pity first
        for (RewardTier rt : rewardProbabilityMap.keySet()) {
            if (rt.isPityEnabled() && gachaPlayer.getPity(this, rt) >= rt.getPityLimit() - 1) {
                // If multiple pities are hit, the one with lower original chance (higher rarity) might be prioritized
                // or the first one encountered. For simplicity, let's take the first one hit by pity.
                // This part might need more sophisticated logic if multiple pities can be hit simultaneously.
                // Current logic: guarantees the specific tier if its pity is hit.
                return rt;
            }
        }

        // If no hard pity is hit, roll based on chances
        double randDouble = Math.random();
        double cumulativeProbability = 0.0;
        for (Map.Entry<RewardTier, Double> entry : rewardProbabilityMap.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randDouble <= cumulativeProbability) {
                return entry.getKey();
            }
        }
        // Fallback, should not be reached if probabilities sum to 1.0 (or close for floating point)
        return rewardProbabilityMap.isEmpty() ? null : rewardProbabilityMap.values().iterator().next().getKey();
    }

    // New method for 50/50 logic
    private Reward determineSpecial5StarReward(GachaPlayer gachaPlayer, RewardTier fiveStarTier) {
        if (fiveStarTier == null || featured5StarRewardNames.isEmpty()) {
            // Configuration error, or not the 5-star tier. Fallback to standard generation.
            return fiveStarTier != null ? fiveStarTier.generateReward() : null;
        }

        Reward selectedReward = null;
        Random random = new Random();

        List<Reward> all5StarRewardsInTier = new ArrayList<>(fiveStarTier.getRewards());
        List<Reward> actualFeaturedRewards = all5StarRewardsInTier.stream()
                .filter(r -> featured5StarRewardNames.contains(r.getName()))
                .collect(Collectors.toList());

        if (actualFeaturedRewards.isEmpty()) { // Config error: featured names don't match actual rewards
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' has no actual featured rewards matching Featured-5Star-Reward-Names in its 5-star tier. Giving a random 5-star.");
            return fiveStarTier.generateReward(); // Fallback
        }

        if (gachaPlayer.isNext5StarGuaranteedFeatured(this)) {
            // Player lost previous 50/50, guaranteed featured.
            selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
            gachaPlayer.resetLimitedBannerGuarantee(this);
        } else {
            // Player is on their 50/50 chance
            if (random.nextBoolean()) { // 50% chance to win 50/50 (get featured)
                selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
                // Optional: reset guarantee if they win 50/50, or keep it so next is also 50/50.
                // Most games keep it as 50/50 until a loss. So, no change to guarantee status here.
                // gachaPlayer.resetLimitedBannerGuarantee(this); // Uncomment if winning 50/50 resets it.
            } else { // 50% chance to lose 50/50 (get non-featured 5-star)
                List<Reward> nonFeatured5Stars = all5StarRewardsInTier.stream()
                        .filter(r -> !featured5StarRewardNames.contains(r.getName()))
                        .collect(Collectors.toList());

                if (!nonFeatured5Stars.isEmpty()) {
                    selectedReward = nonFeatured5Stars.get(random.nextInt(nonFeatured5Stars.size()));
                } else {
                    // Fallback: If only featured items exist in the 5-star pool (config error or by design)
                    // In this case, "losing" 50/50 still gives a featured item.
                    selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
                    Bukkit.getLogger().log(Level.INFO, "[Wish] Crate '" + name + "' 50/50 loss resulted in a featured item as no non-featured 5-stars are available.");
                }
                gachaPlayer.setNext5StarGuaranteedFeatured(this, true); // Guarantee for next time
            }
        }
        return selectedReward;
    }


    public LinkedHashSet<Reward> getAllRewards() {
        LinkedHashSet<Reward> rewards = new LinkedHashSet<>();
        getRewardTiers().forEach((r) -> rewards.addAll(r.getRewards()));
        return rewards;
    }

    public AnimationType getAnimationType() {
        return animationType;
    }

    public double getChance(RewardTier rewardTier) {
        return rewardProbabilityMap.getOrDefault(rewardTier, 0.0);
    }

    public Set<Location> getCrateLocations() {
        return crateLocations;
    }

    public String getName() {
        return name;
    }

    public Optional<RewardTier> getRewardTier(String tierName) {
        return getRewardTiers().stream().filter((r) -> r.getName().equalsIgnoreCase(tierName)).findFirst();
    }

    public Set<RewardTier> getRewardTiers() {
        return rewardProbabilityMap.keySet();
    }

    public UUID getUuid() {
        return uuid;
    }

    // Getter for the new 50/50 banner flag
    public boolean isLimited5050Banner() {
        return isLimited5050Banner;
    }

    public boolean isCrateLocation(Location location) {
        for (Location crateLocation : crateLocations) {
            if (crateLocation.getBlockX() == location.getBlockX()
                    && crateLocation.getBlockY() == location.getBlockY()
                    && crateLocation.getBlockZ() == location.getBlockZ()
                    && Objects.equals(crateLocation.getWorld(), location.getWorld())) { // Added Objects.equals for world
                return true;
            }
        }
        return false;
    }

    public boolean isCrateLocationInUse(Location location) {
        return inUse.getOrDefault(location, false);
    }

    public void loadFrom(ConfigurationSection config) {
        ConfigurationSection rewardTiersSection = config.getConfigurationSection("Reward-Tiers");

        this.uuid = UUID.fromString(config.getString("UUID", UUID.randomUUID().toString()));

        try {
            this.animationType = AnimationType.valueOf(config.getString("Animation-Type", "INTERFACE").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.animationType = AnimationType.INTERFACE;
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Invalid animation type specified for crate `" + name + "`, defaulting to INTERFACE.");
        }

        // Load 50/50 specific settings
        this.isLimited5050Banner = config.getBoolean("Is-Limited-5050-Banner", false);
        this.featured5StarRewardNames = config.getStringList("Featured-5Star-Reward-Names");
        this.fiveStarTierKeyName = config.getString("Five-Star-Tier-Name", "five-star"); // Make 5-star tier name configurable

        // Load reward tiers
        if (rewardTiersSection != null) {
            double cumulativeChanceCheck = 0.0;
            for (String rewardTierName : rewardTiersSection.getKeys(false)) {
                ConfigurationSection rewardTierConfig = rewardTiersSection.getConfigurationSection(rewardTierName);
                RewardTier rewardTier = new RewardTier(rewardTierName);
                double chance = rewardTiersSection.getDouble(rewardTierName + ".Chance", 0.0) / 100.0; // Ensure chance is a fraction
                cumulativeChanceCheck += chance;

                if (rewardTierConfig != null) {
                    rewardTier.loadFrom(rewardTierConfig);
                    rewardProbabilityMap.put(rewardTier, chance);
                    if (rewardTierName.equalsIgnoreCase(this.fiveStarTierKeyName)) {
                        this.resolvedFiveStarTier = rewardTier; // Store the 5-star tier object
                    }
                } else {
                    Bukkit.getLogger().log(Level.WARNING, "[Wish] Configuration section for reward tier '" + rewardTierName + "' in crate '" + name + "' is missing.");
                }
            }
            if (Math.abs(cumulativeChanceCheck - 1.0) > 0.001 && !rewardProbabilityMap.isEmpty()) { // Allow small floating point inaccuracies
                Bukkit.getLogger().log(Level.WARNING, "[Wish] Probabilities for reward tiers in crate '" + name + "' do not sum up to 100% (sum: " + (cumulativeChanceCheck * 100) + "%). This may lead to unexpected behavior.");
            }
            sortProbabilityMap(); // Sorts by chance value, ensure this is intended for your selection logic
        } else {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] No reward tiers specified for crate `" + name + "`");
        }


        // Load crate locations
        for (String locationString : config.getStringList("Locations")) {
            String[] locationArgs = locationString.split(" ");
            if (locationArgs.length < 4) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Malformed location string for crate `" + name + "`: " + locationString);
                continue;
            }
            World world = Bukkit.getWorld(locationArgs[0]);
            int x, y, z;

            if (world == null) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Invalid world name specified in crate locations for `" + name + "`: " + locationArgs[0]);
                continue;
            }
            try {
                x = Integer.parseInt(locationArgs[1]);
                y = Integer.parseInt(locationArgs[2]);
                z = Integer.parseInt(locationArgs[3]);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Invalid x, y, or z number format in crate locations for `" + name + "`: " + locationString);
                continue;
            }
            crateLocations.add(new Location(world, x, y, z));
        }
    }

    public void open(Wish plugin, GachaPlayer gachaPlayer, CrateSession crateSession, int pullCount, Menu menu) {
        // Crate crate = crateSession.getCrate(); // 'this' is the crate instance

        switch (animationType) {
            case NONE -> {
                for (int i = 0; i < pullCount; i++) {
                    RewardTier actualHitTier = generateRewardTier(gachaPlayer); // Applies hard pity
                    if (actualHitTier == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] Failed to generate a reward tier for crate " + name);
                        continue;
                    }

                    Reward finalRewardToGive;
                    boolean isDesignatedFiveStar = actualHitTier == this.resolvedFiveStarTier; // Check if it's the specific 5-star tier object

                    if (this.isLimited5050Banner && isDesignatedFiveStar) {
                        finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                    } else {
                        finalRewardToGive = actualHitTier.generateReward();
                    }

                    if (finalRewardToGive == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] Failed to generate a final reward for crate " + name + " tier " + actualHitTier.getName());
                        continue;
                    }

                    finalRewardToGive.execute(gachaPlayer.getPlayer());

                    // Pity update
                    if (actualHitTier.isPityEnabled()) {
                        gachaPlayer.resetPity(this, actualHitTier);
                    }
                    gachaPlayer.increasePity(this, actualHitTier, 1); // Increments pity for all other enabled tiers
                }
            }
            case INTERFACE -> {
                if (!(menu instanceof CrateOpenMenu crateOpenMenu)) {
                    Lang.ERR_UNKNOWN.send(gachaPlayer.getPlayer());
                    break;
                }

                // Pre-generate rewards for INTERFACE type
                List<RewardTier> obtainedRewardTiers = new ArrayList<>();
                List<Reward> obtainedRewards = new ArrayList<>();

                for (int i = 0; i < pullCount; i++) {
                    RewardTier actualHitTier = generateRewardTier(gachaPlayer);
                    if (actualHitTier == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] Failed to generate a reward tier for crate " + name + " (INTERFACE anim pull " + (i+1) + ")");
                        // Add a placeholder or skip? For now, let's skip adding nulls.
                        continue;
                    }

                    Reward finalRewardToGive;
                    boolean isDesignatedFiveStar = actualHitTier == this.resolvedFiveStarTier;

                    if (this.isLimited5050Banner && isDesignatedFiveStar) {
                        finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                    } else {
                        finalRewardToGive = actualHitTier.generateReward();
                    }

                    if (finalRewardToGive == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] Failed to generate a final reward for crate " + name + " tier " + actualHitTier.getName() + " (INTERFACE anim pull " + (i+1) + ")");
                        continue;
                    }

                    obtainedRewardTiers.add(actualHitTier);
                    obtainedRewards.add(finalRewardToGive);

                    // Pity update
                    if (actualHitTier.isPityEnabled()) {
                        gachaPlayer.resetPity(this, actualHitTier);
                    }
                    gachaPlayer.increasePity(this, actualHitTier, 1);
                }
                // IMPORTANT: CrateOpenMenu.open() signature needs to be changed to accept these lists
                crateOpenMenu.open(gachaPlayer, crateSession, pullCount, obtainedRewardTiers, obtainedRewards);
            }
            case PHYSICAL -> {
                Location crateLocation = crateSession.getCrateLocation();
                // Store results of pulls to execute animations and then give rewards
                List<RewardTierWithReward> physicalPullResults = new ArrayList<>();

                for (int i = 0; i < pullCount; i++) {
                    RewardTier actualHitTier = generateRewardTier(gachaPlayer);
                    if (actualHitTier == null) continue; // Error logged inside generateRewardTier if needed

                    Reward finalRewardToGive;
                    boolean isDesignatedFiveStar = actualHitTier == this.resolvedFiveStarTier;

                    if (this.isLimited5050Banner && isDesignatedFiveStar) {
                        finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                    } else {
                        finalRewardToGive = actualHitTier.generateReward();
                    }
                    if (finalRewardToGive == null) continue;


                    physicalPullResults.add(new RewardTierWithReward(actualHitTier, finalRewardToGive));

                    // Pity update
                    if (actualHitTier.isPityEnabled()) {
                        gachaPlayer.resetPity(this, actualHitTier);
                    }
                    gachaPlayer.increasePity(this, actualHitTier, 1);
                }


                // Physical animation logic, using the pre-generated physicalPullResults
                Location particleStartLoc = crateLocation.clone().add(0.5, 0.8, 0.5);
                crateSession.setOpenPhase(CrateOpenPhase.OPENING);
                setLocationInUse(crateLocation, true);

                // BukkitRunnable for individual particle streams
                new BukkitRunnable() {
                    int currentPullIndex = 0;

                    @Override
                    public void run() {
                        if (currentPullIndex >= physicalPullResults.size()) {
                            // All particle streams launched, now start reward giving timer
                            new BukkitRunnable() {
                                int rewardGiveIndex = 0;
                                final HashMap<Integer, Location> endLocationMap = (HashMap<Integer, Location>) crateSession.getMetadata("endLocationMap");


                                @Override
                                public void run() {
                                    if (rewardGiveIndex >= physicalPullResults.size()) {
                                        setLocationInUse(crateLocation, false);
                                        crateSession.setOpenPhase(CrateOpenPhase.COMPLETE);
                                        plugin.getSessionManager().clearSession(gachaPlayer.getUuid());
                                        cancel();
                                        return;
                                    }

                                    RewardTierWithReward result = physicalPullResults.get(rewardGiveIndex);
                                    Location endLoc = endLocationMap != null ? endLocationMap.get(rewardGiveIndex) : particleStartLoc.clone().add(0, 1.5, 0); // Fallback endLoc

                                    if (result.rewardTier() == null || result.reward() == null || endLoc == null || endLoc.getWorld() == null) {
                                        Bukkit.getLogger().log(Level.WARNING, "[Wish] Null data for physical reward giving for crate " + name);
                                        rewardGiveIndex++;
                                        return;
                                    }

                                    Particle.DustOptions dustOptions = new Particle.DustOptions(result.rewardTier().getColor(), 1);
                                    if (gachaPlayer.getPlayer() != null) {
                                        gachaPlayer.getPlayer().playSound(particleStartLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 2.0f);
                                        result.reward().execute(gachaPlayer.getPlayer());
                                    }
                                    ParticleUtil.spawnStraightLine(endLoc, particleStartLoc, Particle.REDSTONE, dustOptions, 1);
                                    rewardGiveIndex++;
                                }
                            }.runTaskTimer(plugin, 60L, 7L * physicalPullResults.size() > 0 ? (long)(20 * 0.7 / physicalPullResults.size()) : 7L); // Adjust timing based on pull count
                            cancel();
                            return;
                        }

                        RewardTierWithReward result = physicalPullResults.get(currentPullIndex);
                        Particle.DustOptions dustOptions = new Particle.DustOptions(result.rewardTier().getColor(), 1);

                        // Calculate end location for this particle stream
                        // This needs to be stored if CrateSession.getMetadata() is how we retrieve it later
                        // For now, let's assume CrateSession can store a map of index to endLocation
                        HashMap<Integer, Location> endLocationMap = (HashMap<Integer, Location>) crateSession.getMetadata("endLocationMap");
                        if (endLocationMap == null) {
                            endLocationMap = new HashMap<>();
                            crateSession.setMetadata("endLocationMap", endLocationMap);
                        }
                        double xOffset = new Random().nextDouble(0.4 + (currentPullIndex * .15)) * (new Random().nextBoolean() ? -1 : 1);
                        double zOffset = new Random().nextDouble(0.4 + (currentPullIndex * .15)) * (new Random().nextBoolean() ? -1 : 1);
                        Location endLocation = particleStartLoc.clone().add(xOffset, 1.5, zOffset);
                        endLocationMap.put(currentPullIndex, endLocation);

                        ParticleUtil.spawnCurvedLine(plugin, particleStartLoc, endLocation, Particle.REDSTONE, dustOptions, 1);
                        if (gachaPlayer.getPlayer() != null) {
                            gachaPlayer.getPlayer().playSound(particleStartLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        }
                        currentPullIndex++;
                    }
                }.runTaskTimer(plugin, 0, 7L); // Stagger particle streams

                // BukkitRunnable for central cloud effect
                new BukkitRunnable() {
                    int cloudEffectCounter = 0; // Changed name to avoid conflict
                    final Location cloudLoc = particleStartLoc.clone().add(0, 1.5, 0);
                    final List<Location> baseParticleLocations = MathUtil.circle(cloudLoc, 0.5, false);
                    List<Location> currentParticleLocations = new ArrayList<>(baseParticleLocations);
                    final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.SILVER, 1);

                    @Override
                    public void run() {
                        if (crateSession.getOpenPhase() == CrateOpenPhase.COMPLETE || gachaPlayer.getPlayer() == null) {
                            cancel();
                            return;
                        }

                        gachaPlayer.getPlayer().playSound(cloudLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.5f);
                        for (Location particleLoc : currentParticleLocations) {
                            if (particleLoc.getWorld() == null) continue;
                            particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, dustOptions);
                        }

                        if (cloudEffectCounter < pullCount -1) { // Expand up to pullCount-1 times
                            currentParticleLocations.addAll(MathUtil.circle(cloudLoc, 0.5 + ((cloudEffectCounter +1) * .15), true));
                            cloudEffectCounter++;
                        }
                    }
                }.runTaskTimer(plugin, 20L, 7L);
            }
        }
    }


    public void removeLocation(Location location) {
        crateLocations.removeIf(crateLocation -> crateLocation.getBlockX() == location.getBlockX()
                && crateLocation.getBlockY() == location.getBlockY()
                && crateLocation.getBlockZ() == location.getBlockZ()
                && Objects.equals(crateLocation.getWorld(), location.getWorld()));
    }

    public void setLocationInUse(Location location, boolean inUse) {
        this.inUse.put(location, inUse);
    }

    /**
     * Sort the probability map. The original implementation sorts by probability value.
     * For weighted random selection, the order doesn't strictly matter as long as
     * cumulative probability is used, but often configs are easier to read if sorted.
     * Genshin-style Gachas usually define fixed percentages, and the iteration order
     * with cumulative sum ensures correctness.
     */
    private void sortProbabilityMap() {
        // Convert to list
        List<Map.Entry<RewardTier, Double>> list = new ArrayList<>(rewardProbabilityMap.entrySet());

        // Sort the list based on the probability (Double value).
        // This might be for display or specific processing order.
        // For the cumulative probability generation, the order of entries with the *same* probability doesn't matter.
        // If you want to prioritize higher rarity (lower chance value) in case of ties or for display, sort accordingly.
        // Sorting by value (chance) ascending:
        list.sort(Map.Entry.comparingByValue());

        // Re-populate the LinkedHashMap to maintain insertion order (which is now sorted order)
        rewardProbabilityMap.clear();
        for (Map.Entry<RewardTier, Double> entry : list) {
            rewardProbabilityMap.put(entry.getKey(), entry.getValue());
        }
    }

    // Helper record for PHYSICAL animation type
    private record RewardTierWithReward(RewardTier rewardTier, Reward reward) {}

}