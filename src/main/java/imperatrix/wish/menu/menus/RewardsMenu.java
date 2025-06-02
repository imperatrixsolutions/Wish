package imperatrix.wish.menu.menus;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.menu.Menu;
import imperatrix.wish.menu.MenuManager;
import imperatrix.wish.struct.crate.Crate;
import imperatrix.wish.struct.crate.CrateSession;
import imperatrix.wish.struct.GachaPlayer;
import imperatrix.wish.struct.reward.Reward;
import imperatrix.wish.struct.reward.RewardTier;
import imperatrix.wish.util.ItemBuilder;
import imperatrix.wish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class RewardsMenu extends Menu {
    private final Wish plugin;
    private final HashMap<UUID, ItemStack> offhandSnapshotMap = new HashMap<>();
    private String title = "Rewards Menu";
    private ItemStack backgroundItem = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setDisplayName("&7").build();
    private ItemStack borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7").build();
    private ItemStack nextPageItem = new ItemBuilder(Material.FEATHER).setDisplayName("&aNext Page").build();
    private ItemStack previousPageItem = new ItemBuilder(Material.ARROW).setDisplayName("&cPrevious Page").build();
    private ItemStack backItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("&cBack").build();
    private ItemStack pityItem = new ItemBuilder(Material.NETHER_STAR).setDisplayName("&ePity Tracker").build();
    private ItemStack rateItem = new ItemBuilder(Material.DARK_OAK_SIGN).setDisplayName("&eReward Tier Rates").build();
    private final HashMap<UUID, Integer> pageMap = new HashMap<>();

    public RewardsMenu(Wish plugin) {
        super("rewards");
        this.plugin = plugin;
    }

    @Override
    public void load(ConfigurationSection configurationSection) {
        if (configurationSection == null) {
            return;
        }

        title = Utils.formatString(configurationSection.getString("Title"));
        backgroundItem = Utils.decodeItem(configurationSection.getString("Background-Item",
                "WHITE_STAINED_GLASS_PANE name:&7"));
        borderItem = Utils.decodeItem(configurationSection.getString("Border-Item",
                "GRAY_STAINED_GLASS_PANE name:&7"));
        nextPageItem = Utils.decodeItem(configurationSection.getString("Next-Page-Item",
                "FEATHER name:&aNext Page"));
        previousPageItem = Utils.decodeItem(configurationSection.getString("Previous-Page-Item",
                "ARROW name:&cPrevious Page"));
        backItem = Utils.decodeItem(configurationSection.getString("Back-Item",
                "RED_STAINED_GLASS_PANE name:&cPrevious_Menu lore:&7Click_to_return_to_the_previous_menu"));
        pityItem = Utils.decodeItem(configurationSection.getString("Pity-Item",
                "NETHER_STAR name:&e&lPity_Tracker " +
                        "lore:%pity-list%|&7|&7Each_pull_increases_your_pity_tracker|&7If_you_reach_the_pity_limit,_you're_guaranteed_a_reward"));
        rateItem = Utils.decodeItem(configurationSection.getString("Rate-Item",
                "DARK_OAK_SIGN name:&e&lReward_Tier_Rates " +
                        "lore:%rate-list%"));
    }

    @Override
    public void open(Player player) {
        open(player, 1);
        plugin.getMenuManager().setActiveMenu(player.getUniqueId(), this);
    }

    public void open(Player player, int page) {
        CrateSession crateSession = plugin.getSessionManager().getCrateSession(player.getUniqueId());
        GachaPlayer gachaPlayer = plugin.getPlayerCache().getPlayer(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        int counter = 0;

        if (crateSession == null) {
            player.closeInventory();
            Lang.ERR_UNKNOWN.send(player);
            return;
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, backgroundItem);
        }

        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, borderItem);
        }

        if (page > 1) {
            inventory.setItem(36, previousPageItem);
        }

        for (Reward reward : crateSession.getCrate().getAllRewards()) {
            counter++;

            if (counter < (page - 1) * 36) {
                continue;
            }

            if (counter > 36) {
                inventory.setItem(44, nextPageItem);
                break;
            }

            inventory.setItem(counter - 1, reward.getDisplayItem());
        }
        ItemStack newPityItem = pityItem.clone();
        ItemStack newRateItem = rateItem.clone();
        ItemMeta itemMeta = newPityItem.getItemMeta();

        if (itemMeta != null && itemMeta.getLore() != null) {
            List<String> lore = new ArrayList<>();

            itemMeta.getLore().forEach((l) -> {
                if (l.contains("%pity-list%")) {
                    lore.addAll(getPityList(gachaPlayer, crateSession.getCrate()));
                } else {
                    lore.add(l);
                }
            });

            itemMeta.setLore(lore);
            newPityItem.setItemMeta(itemMeta);
        }

        itemMeta = newRateItem.getItemMeta();
        if (itemMeta != null && itemMeta.getLore() != null) {
            List<String> lore = new ArrayList<>();

            itemMeta.getLore().forEach((l) -> {
                if (l.contains("%rate-list%")) {
                    lore.addAll(getRateList(crateSession.getCrate()));
                } else {
                    lore.add(l);
                }
            });

            itemMeta.setLore(lore);
            newRateItem.setItemMeta(itemMeta);
        }

        inventory.setItem(45, backItem);
        inventory.setItem(49, newPityItem);
        inventory.setItem(53, newRateItem);
        offhandSnapshotMap.put(player.getUniqueId(), player.getInventory().getItemInOffHand());
        player.openInventory(inventory);
        pageMap.put(player.getUniqueId(), page);
    }

    @Override
    public void processClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        MenuManager menuManager = plugin.getMenuManager();

        if (menuManager.isOnCooldown(player.getUniqueId())) {
            return;
        } else {
            menuManager.addCooldown(player.getUniqueId());
        }

        if (e.getSlot() == 45) {
            Optional<Menu> crateMenu = plugin.getMenuManager().getMenu("crate");

            if (crateMenu.isEmpty()) {
                player.closeInventory();
                Lang.ERR_UNKNOWN.send(player);
                return;
            }

            crateMenu.get().open(player);
            return;
        }

        if (e.getSlot() == 52 && Objects.requireNonNull(e.getCurrentItem()).isSimilar(previousPageItem)) {
            open(player, pageMap.getOrDefault(player.getUniqueId(), 2) - 1);
            return;
        }

        if (e.getSlot() == 53 && Objects.requireNonNull(e.getCurrentItem()).isSimilar(nextPageItem)) {
            open(player, pageMap.getOrDefault(player.getUniqueId(), 1) + 1);
        }
    }

    @Override
    public void processClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();

        if (offhandSnapshotMap.containsKey(player.getUniqueId())) {
            player.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
        }

        plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
    }

    private List<String> getPityList(GachaPlayer gachaPlayer, Crate crate) {
        HashMap<RewardTier, Integer> pityMap = gachaPlayer.getPityMap(crate);
        List<String> pityList = new ArrayList<>();

        for (RewardTier rewardTier : crate.getRewardTiers()) {
            if (!rewardTier.isPityEnabled()) {
                continue;
            }

            pityList.add(Lang.PITY_TRACKER_FORMAT.toString(false)
                    .replace("%reward-tier%", rewardTier.getName())
                    .replace("%pity-count%", Integer.toString(pityMap.getOrDefault(rewardTier, 0)))
                    .replace("%pity-limit%", Integer.toString(rewardTier.getPityLimit())));
        }

        return pityList;
    }

    private List<String> getRateList(Crate crate) {
        List<String> rateList = new ArrayList<>();
        DecimalFormat decimalFormat = new DecimalFormat("##.##");

        for (RewardTier rewardTier : crate.getRewardTiers()) {
            if (!rewardTier.isPityEnabled()) {
                continue;
            }

            rateList.add(Lang.TIER_RATE_FORMAT.toString(false)
                    .replace("%reward-tier%", rewardTier.getName())
                    .replace("%rate%", decimalFormat.format(crate.getChance(rewardTier))));
        }

        return rateList;
    }
}
