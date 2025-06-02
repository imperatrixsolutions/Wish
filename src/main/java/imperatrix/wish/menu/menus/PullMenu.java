package imperatrix.wish.menu.menus;

import imperatrix.wish.Wish;
import imperatrix.wish.cache.GachaConfig;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.menu.Menu;
import imperatrix.wish.menu.MenuManager;
import imperatrix.wish.struct.crate.CrateSession;
import imperatrix.wish.struct.GachaPlayer;
import imperatrix.wish.util.ItemBuilder;
import imperatrix.wish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class PullMenu extends Menu {
    private final Wish plugin;
    private final HashMap<UUID, Integer> pullCountMap = new HashMap<>();
    private final HashMap<UUID, ItemStack> offhandSnapshotMap = new HashMap<>();
    private String title = "Pull Menu";
    private ItemStack backgroundItem = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setDisplayName("&7").build();
    private ItemStack pullCountItem = new ItemBuilder(Material.NETHER_STAR).setDisplayName("&ePull %pull-count%x").build();
    private ItemStack decreasePullCountItem = new ItemBuilder(Material.IRON_INGOT).setDisplayName("&eDecrease Pull Count").build();
    private ItemStack increasePullCountItem = new ItemBuilder(Material.GOLD_INGOT).setDisplayName("&aIncrease Pull Count").build();
    private ItemStack maxPullCountSelectorItem = new ItemBuilder(Material.DIAMOND).setDisplayName("&eMax Pull Count").build();
    private ItemStack pullItem = new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).setDisplayName("&aPull").build();
    private ItemStack backItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("&cBack").build();
    private ItemStack maxPullCountItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("&cMaximum Pull Count Reached").build();
    private ItemStack minPullCountItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("&cMinimum Pull Count Reached").build();

    public PullMenu(Wish plugin) {
        super("pull");
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
        pullCountItem = Utils.decodeItem(configurationSection.getString("Pull-Count-Item",
                "NETHER_STAR name:&6&lPull_%pull-count%x lore:&fPulling_&e%pull-count%_&ftimes|&f|&7Increase_or_decrease_amount_below"));
        decreasePullCountItem = Utils.decodeItem(configurationSection.getString("Decrease-Pull-Count-Item",
                "IRON_INGOT name:&cDecrease_Pull_Count lore:&7Click_to_&clower&7_your_pull_count_by_1"));
        increasePullCountItem = Utils.decodeItem(configurationSection.getString("Increase-Pull-Count-Item",
                "GOLD_INGOT name:&aIncrease_Pull_Count lore:&7Click_to_&craise&7_your_pull_count_by_1"));
        maxPullCountSelectorItem = Utils.decodeItem(configurationSection.getString("Max-Pull-Count-Selector-Item",
                "DIAMOND name:&eMax_Pull_Count lore:&7Click_to_set_your_pull_count_to_the_max"));
        pullItem = Utils.decodeItem(configurationSection.getString("Pull-Item",
                "LIME_STAINED_GLASS_PANE name:&aPull lore:&7Click_to_pull"));
        backItem = Utils.decodeItem(configurationSection.getString("Back-Item",
                "RED_STAINED_GLASS_PANE name:&cPrevious_Menu lore:&7Click_to_return_to_the_previous_menu"));
        maxPullCountItem = Utils.decodeItem(configurationSection.getString("Max-Pull-Count-Item",
                "RED_STAINED_GLASS_PANE name:&cMaximum_Pull_Count_Reached lore:&7Can_no_longer_increase_pull_count"));
        minPullCountItem = Utils.decodeItem(configurationSection.getString("Min-Pull-Count-Item",
                "RED_STAINED_GLASS_PANE name:&cMinimum_Pull_Count_Reached lore:&7Can_no_longer_decrease_pull_count"));
    }

    @Override
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, backgroundItem);
        }
        HashMap<String, String> variableMap = new HashMap<>();

        variableMap.put("%pull-count%", "1");
        ItemBuilder pullCountItemBuilder = new ItemBuilder(pullCountItem.clone()).setVariables(variableMap);

        inventory.setItem(4, pullCountItemBuilder.build());
        inventory.setItem(12, decreasePullCountItem);
        inventory.setItem(13, increasePullCountItem);
        inventory.setItem(14, maxPullCountSelectorItem);
        inventory.setItem(18, backItem);
        inventory.setItem(26, pullItem);
        offhandSnapshotMap.put(player.getUniqueId(), player.getInventory().getItemInOffHand());
        player.openInventory(inventory);
        plugin.getMenuManager().setActiveMenu(player.getUniqueId(), this);
    }

    @Override
    public void processClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        GachaPlayer gachaPlayer = plugin.getPlayerCache().getPlayer(player.getUniqueId());
        CrateSession crateSession = plugin.getSessionManager().getCrateSession(player.getUniqueId());
        MenuManager menuManager = plugin.getMenuManager();
        int pullCount = pullCountMap.getOrDefault(player.getUniqueId(), 1);

        if (crateSession == null) {
            player.closeInventory();
            Lang.ERR_UNKNOWN.send(player);
            return;
        }

        if (menuManager.isOnCooldown(player.getUniqueId())) {
            return;
        } else {
            menuManager.addCooldown(player.getUniqueId());
        }

        if (e.getSlot() == 12 && e.getCurrentItem() != null && e.getCurrentItem().isSimilar(decreasePullCountItem)) {
            if (pullCount == 1) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                e.getInventory().setItem(e.getSlot(), minPullCountItem);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        e.getInventory().setItem(e.getSlot(), decreasePullCountItem);
                    }
                }.runTaskLater(plugin, 20);
                return;
            }

            pullCountMap.put(player.getUniqueId(), --pullCount);
            updatePullCountItem(player, pullCount);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        if (e.getSlot() == 13 && e.getCurrentItem() != null && e.getCurrentItem().isSimilar(increasePullCountItem)) {
            if (pullCount >= gachaPlayer.getAvailablePulls(crateSession.getCrate()) || pullCount >= GachaConfig.MAX_PULLS) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                e.getInventory().setItem(e.getSlot(), maxPullCountItem);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        e.getInventory().setItem(e.getSlot(), increasePullCountItem);
                    }
                }.runTaskLater(plugin, 20);
                return;
            }

            pullCountMap.put(player.getUniqueId(), ++pullCount);
            updatePullCountItem(player, pullCount);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            return;
        }

        if (e.getSlot() == 14 && e.getCurrentItem() != null && e.getCurrentItem().isSimilar(maxPullCountSelectorItem)) {
            pullCount = Math.min(GachaConfig.MAX_PULLS, gachaPlayer.getAvailablePulls(crateSession.getCrate()));
            pullCountMap.put(player.getUniqueId(), pullCount);
            updatePullCountItem(player, pullCount);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }

        if (e.getSlot() == 18) {
            Optional<Menu> crateMenu = plugin.getMenuManager().getMenu("crate");

            if (crateMenu.isEmpty()) {
                player.closeInventory();
                Lang.ERR_UNKNOWN.send(player);
                return;
            }

            crateMenu.get().open(player);
            return;
        }

        if (e.getSlot() == 26) {
            Optional<Menu> crateOpenMenu = plugin.getMenuManager().getMenu("crate-open");

            if (crateOpenMenu.isEmpty()) {
                player.closeInventory();
                Lang.ERR_UNKNOWN.send(player);
                return;
            }

            if (gachaPlayer.getAvailablePulls(crateSession.getCrate()) < pullCount) {
                player.closeInventory();
                Lang.ERR_NOT_ENOUGH_PULLS.send(player);
                return;
            }

            player.closeInventory();
            gachaPlayer.setAvailablePulls(crateSession.getCrate(), gachaPlayer.getAvailablePulls(crateSession.getCrate()) - pullCount);
            plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
            crateSession.getCrate().open(plugin, gachaPlayer, crateSession, pullCount, crateOpenMenu.get());
        }
    }

    @Override
    public void processClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();

        if (offhandSnapshotMap.containsKey(player.getUniqueId())) {
            player.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
        }

        pullCountMap.remove(player.getUniqueId());
        plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
    }

    private void updatePullCountItem(Player player, int pullCount) {
        HashMap<String, String> variableMap = new HashMap<>();

        variableMap.put("%pull-count%", Integer.toString(pullCount));
        ItemBuilder pullCountItemBuilder = new ItemBuilder(pullCountItem.clone()).setVariables(variableMap);

        player.getOpenInventory().getTopInventory().setItem(4, pullCountItemBuilder.build());
    }
}
