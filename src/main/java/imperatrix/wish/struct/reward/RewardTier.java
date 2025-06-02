package imperatrix.wish.struct.reward;

import imperatrix.wish.util.ItemBuilder;
import imperatrix.wish.util.Utils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class RewardTier {
    private HashMap<Reward, Double> rewardProbabilityMap = new HashMap<>();

    private final String name;

    private int pityLimit = 0;

    private boolean pityEnabled = false;

    private ItemStack displayItem = (new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE)).setDisplayName("&7Reward Tier").build();

    private Color color = Color.SILVER;

    public RewardTier(String name) {
        this.name = name;
    }

    public Reward generateReward() {
        double randDouble = Math.random();
        double count = 0.0D;
        for (Map.Entry<Reward, Double> rewardProbability : this.rewardProbabilityMap.entrySet()) {
            count += ((Double)rewardProbability.getValue()).doubleValue();
            if (randDouble <= count)
                return rewardProbability.getKey();
        }
        return (Reward)((Map.Entry)this.rewardProbabilityMap.entrySet().iterator().next()).getKey();
    }

    public Color getColor() {
        return this.color;
    }

    public ItemStack getDisplayItem() {
        return this.displayItem;
    }

    public String getName() {
        return this.name;
    }

    public int getPityLimit() {
        return this.pityLimit;
    }

    public Set<Reward> getRewards() {
        return this.rewardProbabilityMap.keySet();
    }

    public boolean isPityEnabled() {
        return this.pityEnabled;
    }

    public void loadFrom(ConfigurationSection config) {
        ConfigurationSection rewards = config.getConfigurationSection("Rewards");
        this.pityEnabled = Boolean.parseBoolean(config.getString("Pity", "false"));
        this.pityLimit = config.getInt("Pity-Limit", 0);
        this.displayItem = Utils.decodeItem(config.getString("Display-Item", "WHITE_STAINED_GLASS_PANE name:&7" + this.name));
        this.color = Color.fromRGB(config.getInt("Color.R", 255), config.getInt("Color.G", 255), config.getInt("Color.B", 255));
        if (rewards != null) {
            for (String rewardName : rewards.getKeys(false)) {
                ConfigurationSection rewardsSection = rewards.getConfigurationSection(rewardName);
                Reward reward = new Reward(rewardName);
                double chance = rewards.getDouble(rewardName + ".Chance", 10.0D) / 100.0D;
                assert rewardsSection != null;
                reward.loadFrom(rewardsSection);
                this.rewardProbabilityMap.put(reward, Double.valueOf(chance));
            }
            sortProbabilityMap();
        } else {
            Bukkit.getLogger().log(Level.WARNING, "[GachaCrates] No rewards specified for reward tier `" + this.name + "`");
        }
    }

    private void sortProbabilityMap() {
        List<Map.Entry<Reward, Double>> probabilityMapList = new LinkedList<>(this.rewardProbabilityMap.entrySet());
        probabilityMapList.sort((Comparator)Map.Entry.comparingByValue());
        this.rewardProbabilityMap = (HashMap<Reward, Double>)probabilityMapList.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));
    }
}
