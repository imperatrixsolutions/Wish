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

public class Crate {
    private final LinkedHashMap<RewardTier, Double> rewardProbabilityMap = new LinkedHashMap<>();
    private final String name;
    private UUID uuid;
    private AnimationType animationType;
    private final Set<Location> crateLocations = new HashSet<>();
    private final HashMap<Location, Boolean> inUse = new HashMap<>();

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
     */
    public RewardTier generateRewardTier() {
        double randDouble = Math.random();

        for (Map.Entry<RewardTier, Double> rewardProbability : rewardProbabilityMap.entrySet()) {
            if (randDouble <= rewardProbability.getValue()) {
                return rewardProbability.getKey();
            }
        }

        return rewardProbabilityMap.entrySet().iterator().next().getKey();
    }

    /**
     * Generate a random reward tier based on set probability and pity for a player
     *
     * @return Generated RewardTier
     */
    public RewardTier generateRewardTier(GachaPlayer gachaPlayer) {
        double randDouble = Math.random();
        double count = 0.0;

        for (Map.Entry<RewardTier, Double> rewardProbability : rewardProbabilityMap.entrySet()) {
            RewardTier rewardTier = rewardProbability.getKey();
            count += rewardProbability.getValue();

            if (randDouble <= count ||
                    (rewardTier.isPityEnabled() && gachaPlayer.getPity(this, rewardTier) >= rewardTier.getPityLimit() - 1)) {
                return rewardTier;
            }
        }

        return rewardProbabilityMap.entrySet().iterator().next().getKey();
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
        return rewardProbabilityMap.get(rewardTier);
    }

    public Set<Location> getCrateLocations() {
        return crateLocations;
    }

    public String getName() {
        return name;
    }

    public Optional<RewardTier> getRewardTier(String name) {
        return getRewardTiers().stream().filter((r) -> r.getName().equalsIgnoreCase(name)).findFirst();
    }

    public Set<RewardTier> getRewardTiers() {
        return rewardProbabilityMap.keySet();
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isCrateLocation(Location location) {
        for (Location crateLocation : crateLocations) {
            if (crateLocation.getBlockX() == location.getBlockX()
                    && crateLocation.getBlockY() == location.getBlockY()
                    && crateLocation.getBlockZ() == location.getBlockZ()
                    && crateLocation.getWorld() == location.getWorld()) {
                return true;
            }
        }

        return false;
    }

    public boolean isCrateLocationInUse(Location location) {
        return inUse.getOrDefault(location, false);
    }

    public void loadFrom(ConfigurationSection config) {
        ConfigurationSection rewardTiers = config.getConfigurationSection("Reward-Tiers");

        this.uuid = UUID.fromString(config.getString("UUID", UUID.randomUUID().toString()));

        try {
            this.animationType = AnimationType.valueOf(config.getString("Animation-Type", "INTERFACE").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.animationType = AnimationType.INTERFACE;
            Bukkit.getLogger().log(Level.WARNING, "[Wish] Invalid animation type specified for crate `" + name + "`");
        }

        // Load reward tiers
        if (rewardTiers != null) {
            for (String rewardTierName : rewardTiers.getKeys(false)) {
                ConfigurationSection rewardTierSection = rewardTiers.getConfigurationSection(rewardTierName);
                RewardTier rewardTier = new RewardTier(rewardTierName);
                double chance = rewardTiers.getDouble(rewardTierName + ".Chance", 50) / 100;

                assert rewardTierSection != null;
                rewardTier.loadFrom(rewardTierSection);
                rewardProbabilityMap.put(rewardTier, chance);
            }

            sortProbabilityMap();
        } else {
            Bukkit.getLogger().log(Level.WARNING, "[Wish] No reward tiers specified for crate `" + name + "`");
        }

        // Load crate locations
        for (String locationString : config.getStringList("Locations")) {
            String[] locationArgs = locationString.split(" ");
            World world = Bukkit.getWorld(locationArgs[0]);
            int x;
            int y;
            int z;

            if (world == null) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Invalid world name specified in crate locations for `" + name + "`: " + locationArgs[0]);
                continue;
            }

            try {
                x = Integer.parseInt(locationArgs[1]);
                y = Integer.parseInt(locationArgs[2]);
                z = Integer.parseInt(locationArgs[3]);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[Wish] Invalid x, y, or z specified in crate locations for `" + name + "`: " + locationString);
                continue;
            }

            crateLocations.add(new Location(world, x, y, z));
        }
    }

    public void open(Wish plugin, GachaPlayer gachaPlayer, CrateSession crateSession, int pullCount, Menu menu) {
        Crate crate = crateSession.getCrate();

        switch (animationType) {
            case NONE -> {
                for (int i = 0; i < pullCount; i++) {
                    RewardTier rewardTier = crate.generateRewardTier(gachaPlayer);
                    Reward reward = rewardTier.generateReward();

                    if (rewardTier.isPityEnabled()) {
                        gachaPlayer.resetPity(crate, rewardTier);
                    }

                    reward.execute(gachaPlayer.getPlayer());
                    gachaPlayer.increasePity(crate, rewardTier, 1);
                }
            }
            case INTERFACE -> {
                if (!(menu instanceof CrateOpenMenu crateOpenMenu)) {
                    Lang.ERR_UNKNOWN.send(gachaPlayer.getPlayer());
                    break;
                }

                crateOpenMenu.open(gachaPlayer, crateSession, pullCount);
            }
            case PHYSICAL -> {
                Location crateLocation = crateSession.getCrateLocation();
                HashMap<Integer, RewardTier> rewardTiers = new HashMap<>();
                HashMap<Integer, Reward> rewards = new HashMap<>();
                HashMap<Integer, Location> endLocationMap = new HashMap<>();
                Location particleStartLoc = crateLocation.clone().add(0.5, 0.8, 0.5);

                crateSession.setOpenPhase(CrateOpenPhase.OPENING);
                setLocationInUse(crateLocation, true);
                new BukkitRunnable() {
                    int counter = 1;

                    @Override
                    public void run() {
                        if (counter > pullCount) {
                            new BukkitRunnable() {
                                int newCounter = 1;

                                @Override
                                public void run() {
                                    RewardTier rewardTier = rewardTiers.get(newCounter);
                                    Reward reward = rewards.get(newCounter);
                                    Location endLoc = endLocationMap.get(newCounter);

                                    if (rewardTier == null || reward == null || endLoc == null || endLoc.getWorld() == null) {
                                        setLocationInUse(crateLocation, false);
                                        crateSession.setOpenPhase(CrateOpenPhase.COMPLETE);
                                        plugin.getSessionManager().clearSession(gachaPlayer.getUuid());
                                        cancel();
                                        return;
                                    }
                                    Particle.DustOptions dustOptions = new Particle.DustOptions(rewardTier.getColor(), 1);

                                    gachaPlayer.getPlayer().playSound(particleStartLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 2.0f);
                                    reward.execute(gachaPlayer.getPlayer());
                                    ParticleUtil.spawnStraightLine(endLoc, particleStartLoc, Particle.REDSTONE, dustOptions, 1);
                                    newCounter++;
                                }
                            }.runTaskTimer(plugin, 60, 7);
                            cancel();
                            return;
                        }
                        RewardTier rewardTier = crate.generateRewardTier(gachaPlayer);
                        Reward reward = rewardTier.generateReward();
                        Particle.DustOptions dustOptions = new Particle.DustOptions(rewardTier.getColor(), 1);
                        double xOffset = new Random().nextDouble(0.4 + (counter * .15)) * (new Random().nextBoolean() ? -1 : 1);
                        double zOffset = new Random().nextDouble(0.4 + (counter * .15)) * (new Random().nextBoolean() ? -1 : 1);
                        Location endLocation = particleStartLoc.clone().add(xOffset, 1.5, zOffset);

                        if (rewardTier.isPityEnabled()) {
                            gachaPlayer.resetPity(crate, rewardTier);
                        }

                        gachaPlayer.increasePity(crate, rewardTier, 1);
                        rewards.put(counter, reward);
                        rewardTiers.put(counter, rewardTier);
                        endLocationMap.put(counter, endLocation);
                        ParticleUtil.spawnCurvedLine(plugin, particleStartLoc, endLocation, Particle.REDSTONE, dustOptions, 1);
                        gachaPlayer.getPlayer().playSound(particleStartLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        counter++;
                    }
                }.runTaskTimer(plugin, 0, 7);

                new BukkitRunnable() {
                    int counter = 1;
                    final Location cloudLoc = particleStartLoc.clone().add(0, 1.5, 0);
                    final List<Location> particleLocations = MathUtil.circle(cloudLoc, 0.5, false);
                    final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.SILVER, 1);

                    @Override
                    public void run() {
                        if (crateSession.getOpenPhase() == CrateOpenPhase.COMPLETE) {
                            cancel();
                            return;
                        }

                        gachaPlayer.getPlayer().playSound(cloudLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.5f);
                        for (Location particleLoc : particleLocations) {
                            if (particleLoc.getWorld() == null) {
                                continue;
                            }

                            particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, dustOptions);
                        }

                        if (counter < pullCount) {
                            particleLocations.addAll(MathUtil.circle(cloudLoc, 0.5 + (counter * .15), true));
                            counter++;
                        }
                    }
                }.runTaskTimer(plugin, 20, 7);
            }
        }
    }

    public void removeLocation(Location location) {
        crateLocations.removeIf(crateLocation -> crateLocation.getBlockX() == location.getBlockX()
                && crateLocation.getBlockY() == location.getBlockY()
                && crateLocation.getBlockZ() == location.getBlockZ()
                && crateLocation.getWorld() == location.getWorld());
    }

    public void setLocationInUse(Location location, boolean inUse) {
        this.inUse.put(location, inUse);
    }

    /**
     * Sort the probability map
     */
    private void sortProbabilityMap() {
        LinkedList<Map.Entry<RewardTier, Double>> probabilityMapList = new LinkedList<>(rewardProbabilityMap.entrySet());

        rewardProbabilityMap.clear();
        probabilityMapList.sort(Map.Entry.comparingByValue());
        probabilityMapList.forEach((e) -> rewardProbabilityMap.put(e.getKey(), e.getValue()));
    }
}