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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Crate {
    private final LinkedHashMap<RewardTier, Double> rewardProbabilityMap = new LinkedHashMap<>();
    private final String name;
    private UUID uuid;
    private AnimationType animationType;
    private final Set<Location> crateLocations = new HashSet<>();
    private final HashMap<Location, Boolean> inUse = new HashMap<>();

    // Fields for 50/50 system
    private boolean isLimited5050Banner = false;
    private List<String> featured5StarRewardNames = new ArrayList<>();
    private String fiveStarTierKeyName = "five-star"; // Default key name, configurable
    private RewardTier resolvedFiveStarTier; // Stores the actual 5-star tier object

    public Crate(String name) {
        this.name = name;
    }

    public void addLocation(Location location) {
        crateLocations.add(location);
    }

    /**
     * Generate a random reward tier based on set probability and pity for a player
     *
     * @return Generated RewardTier, or null if an error occurs
     */
    public RewardTier generateRewardTier(GachaPlayer gachaPlayer) {
        // Check for hard pity first
        List<RewardTier> tiersByPityPriority = new ArrayList<>(rewardProbabilityMap.keySet());
        // Optional: Sort tiersByPityPriority if a specific order for checking multiple met pities is desired.
        // For instance, prioritize higher rarity tiers if their pity is met simultaneously.
        // Example: tiersByPityPriority.sort(Comparator.comparingDouble(rt -> rewardProbabilityMap.get(rt))); // Ascending chance (higher rarity first)

        for (RewardTier rt : tiersByPityPriority) {
            if (rt.isPityEnabled() && gachaPlayer.getPity(this, rt) >= rt.getPityLimit() - 1) {
                return rt; // Pity hit for this tier
            }
        }

        // If no hard pity is hit, roll based on chances using cumulative probability
        double randDouble = Math.random();
        double cumulativeProbability = 0.0;
        // Ensure rewardProbabilityMap is iterated in a consistent order if chances are very specific
        // (LinkedHashMap maintains insertion order, which is sorted by chance in sortProbabilityMap)
        for (Map.Entry<RewardTier, Double> entry : rewardProbabilityMap.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randDouble <= cumulativeProbability) {
                return entry.getKey();
            }
        }

        // Fallback if probabilities don't sum to 1.0 or map is empty
        if (!rewardProbabilityMap.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' reward tier probabilities might not sum to 1.0 or an issue occurred. Falling back to first configured tier.");
            return rewardProbabilityMap.keySet().iterator().next();
        }

        Bukkit.getLogger().log(Level.SEVERE, "[Wish] Reward probability map is empty for crate: " + this.name + ". Cannot generate a reward tier.");
        return null;
    }

    // Method for 50/50 logic
    private Reward determineSpecial5StarReward(GachaPlayer gachaPlayer, RewardTier fiveStarTier) {
        if (fiveStarTier == null || !fiveStarTier.getName().equalsIgnoreCase(this.fiveStarTierKeyName) || featured5StarRewardNames.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Attempted 50/50 logic on non-5star tier or missing configuration for crate: " + name);
            return fiveStarTier != null ? fiveStarTier.generateReward() : null; // Fallback to standard generation if it's a tier
        }

        Reward selectedReward;
        Random random = new Random();

        List<Reward> allRewardsInTier = new ArrayList<>(fiveStarTier.getRewards());
        List<Reward> actualFeaturedRewards = allRewardsInTier.stream()
                .filter(r -> featured5StarRewardNames.contains(r.getName()))
                .collect(Collectors.toList());

        if (actualFeaturedRewards.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' (50/50 banner) has Featured-5Star-Reward-Names defined, but no matching rewards found in the '" + fiveStarTierKeyName + "' tier. Giving a random reward from the tier.");
            return fiveStarTier.generateReward(); // Fallback
        }

        if (gachaPlayer.isNext5StarGuaranteedFeatured(this)) {
            selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
            gachaPlayer.resetLimitedBannerGuarantee(this);
        } else {
            if (random.nextBoolean()) { // 50% chance to win 50/50 (get featured)
                selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
                // gachaPlayer.resetLimitedBannerGuarantee(this); // Typically, winning 50/50 means next is also 50/50. No status change.
            } else { // 50% chance to lose 50/50 (get non-featured 5-star)
                List<Reward> nonFeaturedRewards = allRewardsInTier.stream()
                        .filter(r -> !featured5StarRewardNames.contains(r.getName()))
                        .collect(Collectors.toList());

                if (!nonFeaturedRewards.isEmpty()) {
                    selectedReward = nonFeaturedRewards.get(random.nextInt(nonFeaturedRewards.size()));
                } else {
                    // Fallback: If by some misconfiguration only featured items exist in the 5-star pool
                    Bukkit.getLogger().log(Level.INFO, "[Wish] Crate '" + name + "' 50/50 loss resulted in a featured item as no non-featured rewards are available in its '" + fiveStarTierKeyName + "' tier.");
                    selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
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

    public Optional<RewardTier> getRewardTier(String tierNameLookup) {
        return getRewardTiers().stream().filter((r) -> r.getName().equalsIgnoreCase(tierNameLookup)).findFirst();
    }

    public Set<RewardTier> getRewardTiers() {
        return rewardProbabilityMap.keySet();
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isLimited5050Banner() {
        return isLimited5050Banner;
    }

    public boolean isCrateLocation(Location location) {
        for (Location crateLocation : crateLocations) {
            if (crateLocation.getBlockX() == location.getBlockX()
                    && crateLocation.getBlockY() == location.getBlockY()
                    && crateLocation.getBlockZ() == location.getBlockZ()
                    && Objects.equals(crateLocation.getWorld(), location.getWorld())) {
                return true;
            }
        }
        return false;
    }

    public boolean isCrateLocationInUse(Location location) {
        return inUse.getOrDefault(location, false);
    }

    public void loadFrom(ConfigurationSection config) {
        this.uuid = UUID.fromString(config.getString("UUID", UUID.randomUUID().toString()));

        try {
            this.animationType = AnimationType.valueOf(config.getString("Animation-Type", "INTERFACE").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.animationType = AnimationType.INTERFACE;
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Invalid animation type for crate '" + name + "'. Defaulting to INTERFACE.");
        }

        this.isLimited5050Banner = config.getBoolean("Is-Limited-5050-Banner", false);
        this.featured5StarRewardNames = config.getStringList("Featured-5Star-Reward-Names");
        this.fiveStarTierKeyName = config.getString("Five-Star-Tier-Name", "five-star"); // Load the key name for the 5-star tier

        ConfigurationSection rewardTiersSection = config.getConfigurationSection("Reward-Tiers");
        if (rewardTiersSection != null) {
            double cumulativeChanceCheck = 0.0;
            for (String rewardTierNameKey : rewardTiersSection.getKeys(false)) {
                ConfigurationSection rewardTierConfig = rewardTiersSection.getConfigurationSection(rewardTierNameKey);
                if (rewardTierConfig == null) {
                    Bukkit.getLogger().log(Level.WARNING, "[Wish] Missing configuration section for reward tier '" + rewardTierNameKey + "' in crate '" + name + "'.");
                    continue;
                }
                RewardTier rewardTier = new RewardTier(rewardTierNameKey); // Use the key as the name
                rewardTier.loadFrom(rewardTierConfig); // Load before getting chance

                double chance = rewardTiersSection.getDouble(rewardTierNameKey + ".Chance", 0.0) / 100.0;
                cumulativeChanceCheck += chance;

                rewardProbabilityMap.put(rewardTier, chance);

                if (rewardTierNameKey.equalsIgnoreCase(this.fiveStarTierKeyName)) {
                    this.resolvedFiveStarTier = rewardTier;
                }
            }
            if (Math.abs(cumulativeChanceCheck - 1.0) > 0.001 && !rewardProbabilityMap.isEmpty()) {
                Bukkit.getLogger().log(Level.WARNING, "[Wish] Probabilities for reward tiers in crate '" + name + "' do not sum to 100% (sum: " + String.format("%.2f", cumulativeChanceCheck * 100) + "%). This may lead to unexpected behavior.");
            }
            sortProbabilityMap();
            // Resolve the five star tier again after sorting if it wasn't found by exact key name during initial loop
            if (this.resolvedFiveStarTier == null && this.isLimited5050Banner) {
                Optional<RewardTier> foundTier = getRewardTier(this.fiveStarTierKeyName);
                if (foundTier.isPresent()) {
                    this.resolvedFiveStarTier = foundTier.get();
                } else {
                    Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' is a 50/50 banner but could not resolve the five-star tier named: '" + this.fiveStarTierKeyName + "'. 50/50 logic may fail.");
                }
            }

        } else {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] No reward tiers specified for crate `" + name + "`");
        }

        for (String locationString : config.getStringList("Locations")) {
            String[] locationArgs = locationString.split(" ");
            if (locationArgs.length < 4) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Malformed location string for `" + name + "`: " + locationString);
                continue;
            }
            World world = Bukkit.getWorld(locationArgs[0]);
            if (world == null) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Invalid world in location for `" + name + "`: " + locationArgs[0]);
                continue;
            }
            try {
                crateLocations.add(new Location(world, Integer.parseInt(locationArgs[1]), Integer.parseInt(locationArgs[2]), Integer.parseInt(locationArgs[3])));
            } catch (NumberFormatException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Invalid number in location for `" + name + "`: " + locationString);
            }
        }
    }

    public void open(Wish plugin, GachaPlayer gachaPlayer, CrateSession crateSession, int pullCount, Menu menu) {
        Player player = gachaPlayer.getPlayer();
        if (player == null || !player.isOnline()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Attempted to open crate for offline or null player: " + gachaPlayer.getUuid());
            return;
        }

        switch (animationType) {
            case NONE -> {
                for (int i = 0; i < pullCount; i++) {
                    RewardTier actualHitTier = generateRewardTier(gachaPlayer);
                    if (actualHitTier == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] CRITICAL: Failed to generate a reward tier for crate " + name + " during NONE animation. Pull " + (i + 1));
                        continue;
                    }

                    Reward finalRewardToGive;
                    boolean isTheDesignatedFiveStarTier = actualHitTier == this.resolvedFiveStarTier && this.resolvedFiveStarTier != null;

                    if (this.isLimited5050Banner && isTheDesignatedFiveStarTier) {
                        finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                    } else {
                        finalRewardToGive = actualHitTier.generateReward();
                    }

                    if (finalRewardToGive == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] CRITICAL: Failed to generate a final reward for crate " + name + " from tier " + actualHitTier.getName() + ". Pull " + (i + 1));
                        continue;
                    }

                    finalRewardToGive.execute(player);

                    if (actualHitTier.isPityEnabled()) {
                        gachaPlayer.resetPity(this, actualHitTier);
                    }
                    gachaPlayer.increasePity(this, actualHitTier, 1);
                }
            }
            case INTERFACE -> {
                if (!(menu instanceof CrateOpenMenu crateOpenMenu)) {
                    Lang.ERR_UNKNOWN.send(player);
                    Bukkit.getLogger().log(Level.SEVERE, "[Wish] Menu provided for INTERFACE animation for crate " + name + " is not a CrateOpenMenu instance.");
                    return;
                }

                List<RewardTier> obtainedRewardTiers = new ArrayList<>();
                List<Reward> obtainedRewards = new ArrayList<>();

                for (int i = 0; i < pullCount; i++) {
                    RewardTier actualHitTier = generateRewardTier(gachaPlayer);
                    if (actualHitTier == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] CRITICAL: Failed to generate a reward tier for crate " + name + " (INTERFACE anim). Pull " + (i + 1));
                        continue;
                    }

                    Reward finalRewardToGive;
                    boolean isTheDesignatedFiveStarTier = actualHitTier == this.resolvedFiveStarTier && this.resolvedFiveStarTier != null;

                    if (this.isLimited5050Banner && isTheDesignatedFiveStarTier) {
                        finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                    } else {
                        finalRewardToGive = actualHitTier.generateReward();
                    }

                    if (finalRewardToGive == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] CRITICAL: Failed to generate a final reward for crate " + name + " from tier " + actualHitTier.getName() + " (INTERFACE anim). Pull " + (i + 1));
                        continue;
                    }

                    obtainedRewardTiers.add(actualHitTier);
                    obtainedRewards.add(finalRewardToGive);

                    if (actualHitTier.isPityEnabled()) {
                        gachaPlayer.resetPity(this, actualHitTier);
                    }
                    gachaPlayer.increasePity(this, actualHitTier, 1);
                }
                // This call now REQUIRES CrateOpenMenu.java to have the updated open() signature
                crateOpenMenu.open(gachaPlayer, crateSession, pullCount, obtainedRewardTiers, obtainedRewards);
            }
            case PHYSICAL -> {
                Location crateLocation = crateSession.getCrateLocation();
                if (crateLocation == null || crateLocation.getWorld() == null) {
                    Bukkit.getLogger().log(Level.SEVERE, "[Wish] Crate location or world is null for PHYSICAL animation for crate " + name);
                    Lang.ERR_UNKNOWN.send(player);
                    return;
                }
                List<RewardTierWithReward> physicalPullResults = new ArrayList<>();

                for (int i = 0; i < pullCount; i++) {
                    RewardTier actualHitTier = generateRewardTier(gachaPlayer);
                    if (actualHitTier == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] CRITICAL: Failed to generate a reward tier for crate " + name + " (PHYSICAL anim). Pull " + (i + 1));
                        continue;
                    }

                    Reward finalRewardToGive;
                    boolean isTheDesignatedFiveStarTier = actualHitTier == this.resolvedFiveStarTier && this.resolvedFiveStarTier != null;

                    if (this.isLimited5050Banner && isTheDesignatedFiveStarTier) {
                        finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                    } else {
                        finalRewardToGive = actualHitTier.generateReward();
                    }

                    if (finalRewardToGive == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Wish] CRITICAL: Failed to generate a final reward for crate " + name + " from tier " + actualHitTier.getName() + " (PHYSICAL anim). Pull " + (i + 1));
                        continue;
                    }

                    physicalPullResults.add(new RewardTierWithReward(actualHitTier, finalRewardToGive));

                    if (actualHitTier.isPityEnabled()) {
                        gachaPlayer.resetPity(this, actualHitTier);
                    }
                    gachaPlayer.increasePity(this, actualHitTier, 1);
                }

                Location particleStartLoc = crateLocation.clone().add(0.5, 0.8, 0.5);
                crateSession.setOpenPhase(CrateOpenPhase.OPENING);
                setLocationInUse(crateLocation, true);

                // Initialize or get the endLocationMap from CrateSession
                HashMap<Integer, Location> endLocationMap = crateSession.getPhysicalAnimationEndLocationMap();
                if (endLocationMap == null) {
                    endLocationMap = new HashMap<>();
                    crateSession.setPhysicalAnimationEndLocationMap(endLocationMap);
                }
                // Clear any old data from a previous session if it wasn't cleared
                endLocationMap.clear();


                final HashMap<Integer, Location> finalEndLocationMap = endLocationMap; // For use in Runnable

                new BukkitRunnable() { // Particle stream launcher
                    int currentPullIndex = 0;
                    @Override
                    public void run() {
                        if (currentPullIndex >= physicalPullResults.size() || player == null || !player.isOnline()) {
                            // All streams launched, now schedule reward giving
                            new BukkitRunnable() {
                                int rewardGiveIndex = 0;
                                @Override
                                public void run() {
                                    if (rewardGiveIndex >= physicalPullResults.size() || player == null || !player.isOnline()) {
                                        setLocationInUse(crateLocation, false);
                                        crateSession.setOpenPhase(CrateOpenPhase.COMPLETE);
                                        if(crateSession.getPhysicalAnimationEndLocationMap() != null) crateSession.getPhysicalAnimationEndLocationMap().clear(); // Clean up map
                                        plugin.getSessionManager().clearSession(gachaPlayer.getUuid());
                                        cancel();
                                        return;
                                    }

                                    RewardTierWithReward result = physicalPullResults.get(rewardGiveIndex);
                                    Location endLoc = finalEndLocationMap.get(rewardGiveIndex);
                                    // Fallback if map somehow doesn't have the key, though it should
                                    if (endLoc == null) endLoc = particleStartLoc.clone().add(0,1.5,0);


                                    if (result.rewardTier() == null || result.reward() == null || endLoc.getWorld() == null) {
                                        Bukkit.getLogger().log(Level.WARNING, "[Wish] Null data for physical reward giving (idx " + rewardGiveIndex + ") for crate " + name);
                                        rewardGiveIndex++;
                                        return;
                                    }

                                    Particle.DustOptions dustOptions = new Particle.DustOptions(result.rewardTier().getColor(), 1);
                                    player.playSound(particleStartLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 2.0f);
                                    result.reward().execute(player);
                                    ParticleUtil.spawnStraightLine(endLoc, particleStartLoc, Particle.REDSTONE, dustOptions, 1);
                                    rewardGiveIndex++;
                                }
                            }.runTaskTimer(plugin, 60L, Math.max(7L, (long)(20 * 0.7 / Math.max(1, physicalPullResults.size()))));
                            cancel();
                            return;
                        }

                        RewardTierWithReward result = physicalPullResults.get(currentPullIndex);
                        Particle.DustOptions dustOptions = new Particle.DustOptions(result.rewardTier().getColor(), 1);

                        double xOffset = new Random().nextDouble(0.4 + (currentPullIndex * .15)) * (new Random().nextBoolean() ? -1 : 1);
                        double zOffset = new Random().nextDouble(0.4 + (currentPullIndex * .15)) * (new Random().nextBoolean() ? -1 : 1);
                        Location endLocation = particleStartLoc.clone().add(xOffset, 1.5, zOffset);
                        finalEndLocationMap.put(currentPullIndex, endLocation); // Store for the reward giver

                        ParticleUtil.spawnCurvedLine(plugin, particleStartLoc, endLocation, Particle.REDSTONE, dustOptions, 1);
                        player.playSound(particleStartLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        currentPullIndex++;
                    }
                }.runTaskTimer(plugin, 0, 7L);

                new BukkitRunnable() { // Central cloud effect
                    int cloudEffectCounter = 0;
                    final Location cloudLoc = particleStartLoc.clone().add(0, 1.5, 0);
                    List<Location> currentParticleLocations = new ArrayList<>(MathUtil.circle(cloudLoc, 0.5, false));
                    final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.SILVER, 1);

                    @Override
                    public void run() {
                        if (crateSession.getOpenPhase() == CrateOpenPhase.COMPLETE || player == null || !player.isOnline()) {
                            cancel();
                            return;
                        }

                        player.playSound(cloudLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.5f);
                        for (Location particleLoc : currentParticleLocations) {
                            if (particleLoc.getWorld() == null) continue;
                            particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, dustOptions);
                        }

                        if (cloudEffectCounter < pullCount - 1) {
                            currentParticleLocations.addAll(MathUtil.circle(cloudLoc, 0.5 + ((cloudEffectCounter + 1) * .15), true));
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

    private void sortProbabilityMap() {
        List<Map.Entry<RewardTier, Double>> list = new ArrayList<>(rewardProbabilityMap.entrySet());
        list.sort(Map.Entry.comparingByValue()); // Sorts by chance, ascending (lower chance first)
        rewardProbabilityMap.clear();
        for (Map.Entry<RewardTier, Double> entry : list) {
            rewardProbabilityMap.put(entry.getKey(), entry.getValue());
        }
    }

    private record RewardTierWithReward(RewardTier rewardTier, Reward reward) {}
}