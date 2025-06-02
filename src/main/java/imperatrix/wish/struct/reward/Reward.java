package imperatrix.wish.struct.reward;

import imperatrix.wish.util.ItemBuilder;
import imperatrix.wish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Reward {
    private final String name;
    private final List<ItemStack> items = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();
    private ItemStack displayItem = new ItemBuilder(Material.AMETHYST_SHARD).setDisplayName("&dReward").build();

    public Reward(String name) {
        this.name = name;
    }

    public List<String> getCommands() {
        return commands;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public String getName() {
        return name;
    }

    /**
     * Give rewards to player
     *
     * @param player The player to give the rewards to
     */
    public void execute(Player player) {
        for (String cmd : commands) {
            cmd = cmd.replace("%player%", player.getName());

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        for (ItemStack item : items) {
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItem(player.getLocation(), item);
            } else {
                player.getInventory().addItem(item);
            }
        }
    }

    public void loadFrom(ConfigurationSection config) {
        config.getStringList("Items").forEach((i) -> items.add(Utils.decodeItem(i)));
        commands.addAll(config.getStringList("Commands"));
        displayItem = Utils.decodeItem(config.getString("Display-Item", "AMETHYST_SHARD 1 name:&d" + name));
    }
}
