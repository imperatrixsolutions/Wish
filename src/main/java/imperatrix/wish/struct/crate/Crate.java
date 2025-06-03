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

    // Fields for 50/50 system (typically for character/general limited banners)
    private boolean isLimited5050Banner = false;
    private List<String> featured5StarRewardNames = new ArrayList<>();
    private String fiveStarTierKeyName = "five-star"; // Default key name for 50/50 banner's high tier
    private RewardTier resolvedFiveStarTier;

    // New Fields for Guaranteed Featured Weapon Banner system
    private boolean isGuaranteedFeaturedWeaponBanner = false;
    private List<String> featuredWeaponNames = new ArrayList<>();
    private String highestWeaponTierKeyName = "five-star-weapon"; // Default key name for weapon banner's high tier
    private RewardTier resolvedHighestWeaponTier;


    public Crate(String name) {
        this.name = name;
    }

    public void addLocation(Location location) {
        crateLocations.add(location);
    }

    public RewardTier generateRewardTier(GachaPlayer gachaPlayer) {
        List<RewardTier> tiersByPityPriority = new ArrayList<>(rewardProbabilityMap.keySet());
        for (RewardTier rt : tiersByPityPriority) {
            if (rt.isPityEnabled() && gachaPlayer.getPity(this, rt) >= rt.getPityLimit() - 1) {
                return rt;
            }
        }
        double randDouble = Math.random();
        double cumulativeProbability = 0.0;
        for (Map.Entry<RewardTier, Double> entry : rewardProbabilityMap.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randDouble <= cumulativeProbability) {
                return entry.getKey();
            }
        }
        if (!rewardProbabilityMap.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' reward tier probabilities might not sum to 1.0 or an issue occurred. Falling back to first configured tier.");
            return rewardProbabilityMap.keySet().iterator().next();
        }
        Bukkit.getLogger().log(Level.SEVERE, "[Wish] Reward probability map is empty for crate: " + this.name + ". Cannot generate a reward tier.");
        return null;
    }

    private Reward determineSpecial5StarReward(GachaPlayer gachaPlayer, RewardTier fiveStarTier) {
        // (This is your existing 50/50 logic for character banners - kept as is)
        if (fiveStarTier == null || !fiveStarTier.getName().equalsIgnoreCase(this.fiveStarTierKeyName) || featured5StarRewardNames.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Attempted 50/50 logic on non-designated tier or missing 50/50 configuration for crate: " + name);
            return fiveStarTier != null ? fiveStarTier.generateReward() : null;
        }
        Reward selectedReward;
        Random random = new Random();
        List<Reward> allRewardsInTier = new ArrayList<>(fiveStarTier.getRewards());
        List<Reward> actualFeaturedRewards = allRewardsInTier.stream()
                .filter(r -> featured5StarRewardNames.contains(r.getName()))
                .collect(Collectors.toList());

        if (actualFeaturedRewards.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' (50/50 Banner) has Featured-5Star-Reward-Names defined, but no matching rewards found in its '" + fiveStarTierKeyName + "' tier. Giving a random reward from the tier.");
            return fiveStarTier.generateReward();
        }

        if (gachaPlayer.isNext5StarGuaranteedFeatured(this)) {
            selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
            gachaPlayer.resetLimitedBannerGuarantee(this);
        } else {
            if (random.nextBoolean()) {
                selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
            } else {
                List<Reward> nonFeaturedRewards = allRewardsInTier.stream()
                        .filter(r -> !featured5StarRewardNames.contains(r.getName()))
                        .collect(Collectors.toList());
                if (!nonFeaturedRewards.isEmpty()) {
                    selectedReward = nonFeaturedRewards.get(random.nextInt(nonFeaturedRewards.size()));
                } else {
                    selectedReward = actualFeaturedRewards.get(random.nextInt(actualFeaturedRewards.size()));
                }
                gachaPlayer.setNext5StarGuaranteedFeatured(this, true);
            }
        }
        return selectedReward;
    }

    // New method for Guaranteed Featured Weapon logic
    private Reward determineGuaranteedFeaturedWeapon(RewardTier weaponTier) {
        if (weaponTier == null || !weaponTier.getName().equalsIgnoreCase(this.highestWeaponTierKeyName) || featuredWeaponNames.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Attempted to get guaranteed featured weapon for crate '" + name + "' but configuration is missing (tier name mismatch or no featured weapon names). Tier found: " + (weaponTier != null ? weaponTier.getName() : "null") + ", Expected: " + this.highestWeaponTierKeyName);
            return weaponTier != null ? weaponTier.generateReward() : null; // Fallback
        }

        List<Reward> allRewardsInTier = new ArrayList<>(weaponTier.getRewards());
        List<Reward> actualFeaturedWeapons = allRewardsInTier.stream()
                .filter(r -> featuredWeaponNames.contains(r.getName()))
                .collect(Collectors.toList());

        if (actualFeaturedWeapons.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' (Guaranteed Weapon Banner) has Featured-Weapon-Names defined, but no matching rewards found in its '" + highestWeaponTierKeyName + "' tier. Giving a random reward from the tier as fallback.");
            return weaponTier.generateReward(); // Fallback
        }

        // If multiple featured weapons are listed (uncommon for this banner type), pick one randomly.
        // Usually, there's only one.
        Random random = new Random();
        return actualFeaturedWeapons.get(random.nextInt(actualFeaturedWeapons.size()));
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

    public boolean isLimited5050Banner() { // For character/general limited banners
        return isLimited5050Banner;
    }

    public boolean isGuaranteedFeaturedWeaponBanner() { // For weapon banners
        return isGuaranteedFeaturedWeaponBanner;
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
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Invalid animation type for '" + name + "'. Defaulting to INTERFACE.");
        }

        // Load 50/50 specific settings (for character/general limited banners)
        this.isLimited5050Banner = config.getBoolean("Is-Limited-5050-Banner", false);
        this.featured5StarRewardNames = config.getStringList("Featured-5Star-Reward-Names");
        this.fiveStarTierKeyName = config.getString("Five-Star-Tier-Name", "five-star");

        // Load Guaranteed Featured Weapon Banner settings
        this.isGuaranteedFeaturedWeaponBanner = config.getBoolean("Is-Guaranteed-Featured-Weapon-Banner", false);
        this.featuredWeaponNames = config.getStringList("Featured-Weapon-Names");
        this.highestWeaponTierKeyName = config.getString("Highest-Weapon-Tier-Name", "five-star-weapon");

        ConfigurationSection rewardTiersSection = config.getConfigurationSection("Reward-Tiers");
        if (rewardTiersSection != null) {
            double cumulativeChanceCheck = 0.0;
            for (String rewardTierNameKey : rewardTiersSection.getKeys(false)) {
                ConfigurationSection rewardTierConfig = rewardTiersSection.getConfigurationSection(rewardTierNameKey);
                if (rewardTierConfig == null) {
                    Bukkit.getLogger().log(Level.WARNING, "[Wish] Missing config for reward tier '" + rewardTierNameKey + "' in crate '" + name + "'.");
                    continue;
                }
                RewardTier rewardTier = new RewardTier(rewardTierNameKey);
                rewardTier.loadFrom(rewardTierConfig);
                double chance = rewardTiersSection.getDouble(rewardTierNameKey + ".Chance", 0.0) / 100.0;
                cumulativeChanceCheck += chance;
                rewardProbabilityMap.put(rewardTier, chance);

                if (rewardTierNameKey.equalsIgnoreCase(this.fiveStarTierKeyName)) {
                    this.resolvedFiveStarTier = rewardTier;
                }
                if (rewardTierNameKey.equalsIgnoreCase(this.highestWeaponTierKeyName)) {
                    this.resolvedHighestWeaponTier = rewardTier;
                }
            }
            if (Math.abs(cumulativeChanceCheck - 1.0) > 0.001 && !rewardProbabilityMap.isEmpty()) {
                Bukkit.getLogger().log(Level.WARNING, "[Wish] Probabilities for reward tiers in crate '" + name + "' do not sum to 100% (sum: " + String.format("%.2f", cumulativeChanceCheck * 100) + "%).");
            }
            sortProbabilityMap();
            // Resolve tiers again after sorting if not found by exact key name match
            if (this.resolvedFiveStarTier == null && this.isLimited5050Banner) {
                getRewardTier(this.fiveStarTierKeyName).ifPresent(tier -> this.resolvedFiveStarTier = tier);
                if (this.resolvedFiveStarTier == null) Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' is 50/50 banner but couldn't resolve tier: '" + this.fiveStarTierKeyName + "'.");
            }
            if (this.resolvedHighestWeaponTier == null && this.isGuaranteedFeaturedWeaponBanner) {
                getRewardTier(this.highestWeaponTierKeyName).ifPresent(tier -> this.resolvedHighestWeaponTier = tier);
                if (this.resolvedHighestWeaponTier == null) Bukkit.getLogger().log(Level.WARNING, "[Wish] Crate '" + name + "' is Guaranteed Weapon Banner but couldn't resolve tier: '" + this.highestWeaponTierKeyName + "'.");
            }
        } else {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] No reward tiers for crate `" + name + "`");
        }

        // Load crate locations (same as before)
        for (String locationString : config.getStringList("Locations")) {
            String[] locationArgs = locationString.split(" ");
            if (locationArgs.length < 4) { /* ... error log ... */ continue; }
            World world = Bukkit.getWorld(locationArgs[0]);
            if (world == null) { /* ... error log ... */ continue; }
            try {
                crateLocations.add(new Location(world, Integer.parseInt(locationArgs[1]), Integer.parseInt(locationArgs[2]), Integer.parseInt(locationArgs[3])));
            } catch (NumberFormatException e) { /* ... error log ... */ }
        }
    }

    public void open(Wish plugin, GachaPlayer gachaPlayer, CrateSession crateSession, int pullCount, Menu menu) {
        Player player = gachaPlayer.getPlayer();
        if (player == null || !player.isOnline()) { /* ... error log ... */ return; }

        // --- Pre-generate all rewards for INTERFACE and PHYSICAL types ---
        List<RewardTier> obtainedRewardTiersList = new ArrayList<>();
        List<Reward> obtainedRewardsList = new ArrayList<>();
        if (animationType == AnimationType.INTERFACE || animationType == AnimationType.PHYSICAL) {
            for (int i = 0; i < pullCount; i++) {
                RewardTier actualHitTier = generateRewardTier(gachaPlayer);
                if (actualHitTier == null) { /* ... error log ... */ continue; }

                Reward finalRewardToGive;
                boolean isTheDesignatedCharBannerHighTier = actualHitTier == this.resolvedFiveStarTier && this.resolvedFiveStarTier != null;
                boolean isTheDesignatedWeaponBannerHighTier = actualHitTier == this.resolvedHighestWeaponTier && this.resolvedHighestWeaponTier != null;

                if (this.isGuaranteedFeaturedWeaponBanner && isTheDesignatedWeaponBannerHighTier) {
                    finalRewardToGive = determineGuaranteedFeaturedWeapon(actualHitTier);
                } else if (this.isLimited5050Banner && isTheDesignatedCharBannerHighTier) {
                    finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                } else {
                    finalRewardToGive = actualHitTier.generateReward();
                }

                if (finalRewardToGive == null) { /* ... error log ... */ continue; }

                obtainedRewardTiersList.add(actualHitTier);
                obtainedRewardsList.add(finalRewardToGive);

                if (actualHitTier.isPityEnabled()) {
                    gachaPlayer.resetPity(this, actualHitTier);
                }
                gachaPlayer.increasePity(this, actualHitTier, 1);
            }
        }
        // --- End of Pre-generation ---


        switch (animationType) {
            case NONE -> {
                for (int i = 0; i < pullCount; i++) {
                    RewardTier actualHitTier = generateRewardTier(gachaPlayer);
                    if (actualHitTier == null) { /* ... error log ... */ continue; }
                    Reward finalRewardToGive;
                    boolean isTheDesignatedCharBannerHighTier = actualHitTier == this.resolvedFiveStarTier && this.resolvedFiveStarTier != null;
                    boolean isTheDesignatedWeaponBannerHighTier = actualHitTier == this.resolvedHighestWeaponTier && this.resolvedHighestWeaponTier != null;

                    if (this.isGuaranteedFeaturedWeaponBanner && isTheDesignatedWeaponBannerHighTier) {
                        finalRewardToGive = determineGuaranteedFeaturedWeapon(actualHitTier);
                    } else if (this.isLimited5050Banner && isTheDesignatedCharBannerHighTier) {
                        finalRewardToGive = determineSpecial5StarReward(gachaPlayer, actualHitTier);
                    } else {
                        finalRewardToGive = actualHitTier.generateReward();
                    }
                    if (finalRewardToGive == null) { /* ... error log ... */ continue; }

                    finalRewardToGive.execute(player);
                    if (actualHitTier.isPityEnabled()) {
                        gachaPlayer.resetPity(this, actualHitTier);
                    }
                    gachaPlayer.increasePity(this, actualHitTier, 1);
                }
            }
            case INTERFACE -> {
                if (!(menu instanceof CrateOpenMenu crateOpenMenu)) { /* ... error log ... */ return; }
                // Uses pre-generated obtainedRewardTiersList and obtainedRewardsList
                crateOpenMenu.open(gachaPlayer, crateSession, pullCount, obtainedRewardTiersList, obtainedRewardsList);
            }
            case PHYSICAL -> {
                Location crateLocation = crateSession.getCrateLocation();
                if (crateLocation == null || crateLocation.getWorld() == null) { /* ... error log ... */ return; }

                // Uses pre-generated obtainedRewardTiersList and obtainedRewardsList
                // Reconstruct RewardTierWithReward list for the animation logic
                List<RewardTierWithReward> physicalPullResults = new ArrayList<>();
                for (int i = 0; i < obtainedRewardsList.size(); i++) {
                    physicalPullResults.add(new RewardTierWithReward(obtainedRewardTiersList.get(i), obtainedRewardsList.get(i)));
                }

                Location particleStartLoc = crateLocation.clone().add(0.5, 0.8, 0.5);
                crateSession.setOpenPhase(CrateOpenPhase.OPENING);
                setLocationInUse(crateLocation, true);

                HashMap<Integer, Location> endLocationMap = crateSession.getPhysicalAnimationEndLocationMap();
                if (endLocationMap == null) {
                    endLocationMap = new HashMap<>();
                    crateSession.setPhysicalAnimationEndLocationMap(endLocationMap);
                }
                endLocationMap.clear();
                final HashMap<Integer, Location> finalEndLocationMap = endLocationMap;

                new BukkitRunnable() { // Particle stream launcher
                    int currentPullIndex = 0;
                    @Override
                    public void run() {
                        if (currentPullIndex >= physicalPullResults.size() || player == null || !player.isOnline()) {
                            new BukkitRunnable() { // Reward giver
                                int rewardGiveIndex = 0;
                                @Override
                                public void run() {
                                    if (rewardGiveIndex >= physicalPullResults.size() || player == null || !player.isOnline()) {
                                        setLocationInUse(crateLocation, false);
                                        crateSession.setOpenPhase(CrateOpenPhase.COMPLETE);
                                        if(crateSession.getPhysicalAnimationEndLocationMap() != null) crateSession.clearPhysicalAnimationData(); // Use the clear method
                                        plugin.getSessionManager().clearSession(gachaPlayer.getUuid());
                                        cancel();
                                        return;
                                    }
                                    RewardTierWithReward result = physicalPullResults.get(rewardGiveIndex);
                                    Location endLoc = finalEndLocationMap.get(rewardGiveIndex);
                                    if (endLoc == null) endLoc = particleStartLoc.clone().add(0,1.5,0);

                                    if (result.rewardTier() == null || result.reward() == null || endLoc.getWorld() == null) { /* ... error log ... */ rewardGiveIndex++; return; }

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
                        finalEndLocationMap.put(currentPullIndex, endLocation);
                        ParticleUtil.spawnCurvedLine(plugin, particleStartLoc, endLocation, Particle.REDSTONE, dustOptions, 1);
                        player.playSound(particleStartLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        currentPullIndex++;
                    }
                }.runTaskTimer(plugin, 0, 7L);

                new BukkitRunnable() { // Central cloud effect (same as before)
                    // ... (cloud effect logic, ensure it uses player and checks !player.isOnline()) ...
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
        crateLocations.removeIf(crateLoc -> crateLoc.getBlockX() == location.getBlockX()
                && crateLoc.getBlockY() == location.getBlockY()
                && crateLoc.getBlockZ() == location.getBlockZ()
                && Objects.equals(crateLoc.getWorld(), location.getWorld()));
    }

    public void setLocationInUse(Location location, boolean inUse) {
        this.inUse.put(location, inUse);
    }

    private void sortProbabilityMap() {
        List<Map.Entry<RewardTier, Double>> list = new ArrayList<>(rewardProbabilityMap.entrySet());
        list.sort(Map.Entry.comparingByValue());
        rewardProbabilityMap.clear();
        for (Map.Entry<RewardTier, Double> entry : list) {
            rewardProbabilityMap.put(entry.getKey(), entry.getValue());
        }
    }

    private record RewardTierWithReward(RewardTier rewardTier, Reward reward) {}
}