package imperatrix.wish.menu.menus;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.menu.Menu;
import imperatrix.wish.menu.MenuManager;
import imperatrix.wish.struct.*;
import imperatrix.wish.struct.crate.Crate;
import imperatrix.wish.struct.crate.CrateOpenPhase;
import imperatrix.wish.struct.crate.CrateSession;
import imperatrix.wish.struct.reward.Reward;
import imperatrix.wish.struct.reward.RewardTier;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public class CrateOpenMenu extends Menu {
    private final Wish plugin;
    private final HashMap<UUID, ItemStack> offhandSnapshotMap = new HashMap<>();
    private String title = "&6&lPull Rewards";
    private ItemStack countdownItem3 = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("&7").build();
    private ItemStack countdownItem2 = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE).setDisplayName("&7").build();
    private ItemStack countdownItem1 = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7").build();

    public CrateOpenMenu(Wish plugin) {
        super("crate-open");
        this.plugin = plugin;
    }

    @Override
    public void load(@Nullable ConfigurationSection configurationSection) {
        if (configurationSection == null) {
            return;
        }

        title = Utils.formatString(configurationSection.getString("Title", "&6&lPull Rewards"));
        countdownItem1 = Utils.decodeItem(configurationSection.getString("Countdown-Item-1", "GRAY_STAINED_GLASS_PANE name:&7Revealing_in_1s"));
        countdownItem2 = Utils.decodeItem(configurationSection.getString("Countdown-Item-2", "YELLOW_STAINED_GLASS_PANE name:&7Revealing_in_2s"));
        countdownItem3 = Utils.decodeItem(configurationSection.getString("Countdown-Item-3", "ORANGE_STAINED_GLASS_PANE name:&7Revealing_in_3s"));
    }

    @Override
    public void open(Player player) {

    }

    public void open(GachaPlayer gachaPlayer, CrateSession crateSession, int pullCount) {
        int rows = Math.min(pullCount % 9 > 0 ? (pullCount / 9) + 1 : pullCount/9, 6);
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        HashMap<Integer, RewardTier> rewardTiers = new HashMap<>();
        HashMap<Integer, Reward> rewards = new HashMap<>();
        Crate crate = crateSession.getCrate();
        Player player = gachaPlayer.getPlayer();

        // Generate rewards and set item covers
        for (int i = 0; i < pullCount; i++) {
            RewardTier rewardTier = crate.generateRewardTier(gachaPlayer);
            Reward reward = rewardTier.generateReward();

            if (rewardTier.isPityEnabled()) {
                gachaPlayer.resetPity(crate, rewardTier);
            }

            rewards.put(i, reward);
            rewardTiers.put(i, rewardTier);
            gachaPlayer.increasePity(crate, rewardTier, 1);
            inventory.setItem(i, countdownItem3);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < pullCount; i++) {
                inventory.setItem(i, countdownItem2);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }, 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < pullCount; i++) {
                inventory.setItem(i, countdownItem1);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }, 40L);

        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                RewardTier rewardTier = rewardTiers.get(counter);

                if (rewardTier == null) {
                    crateSession.setOpenPhase(CrateOpenPhase.COMPLETE);
                    cancel();
                    return;
                }

                inventory.setItem(counter++, rewardTier.getDisplayItem());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, .7f, .7f);
            }
        }.runTaskTimer(plugin, 60L, 7L);


        crateSession.setRewards(rewards);
        crateSession.setOpenPhase(CrateOpenPhase.OPENING);
        offhandSnapshotMap.put(player.getUniqueId(), player.getInventory().getItemInOffHand());
        player.openInventory(inventory);
        plugin.getMenuManager().setActiveMenu(player.getUniqueId(), this);
    }

    @Override
    public void processClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        CrateSession crateSession = plugin.getSessionManager().getCrateSession(player.getUniqueId());
        MenuManager menuManager = plugin.getMenuManager();

        if (crateSession == null) {
            player.closeInventory();
            Lang.ERR_UNKNOWN.send(player);
            plugin.getSessionManager().clearSession(player.getUniqueId());
            plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
            return;
        }

        if (menuManager.isOnCooldown(player.getUniqueId())) {
            return;
        } else {
            menuManager.addCooldown(player.getUniqueId());
        }

        if (crateSession.getOpenPhase() != CrateOpenPhase.COMPLETE || e.getCurrentItem() == null) {
            return;
        }
        Reward reward = crateSession.getReward(e.getSlot());

        if (reward == null) {
            return;
        }

        player.getOpenInventory().getTopInventory().setItem(e.getSlot(), reward.getDisplayItem());
    }

    @Override
    public void processClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        CrateSession crateSession = plugin.getSessionManager().getCrateSession(player.getUniqueId());

        if (offhandSnapshotMap.containsKey(player.getUniqueId())) {
            player.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
        }

        if (crateSession == null) {
            plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
            return;
        }

        if (crateSession.getOpenPhase() == CrateOpenPhase.OPENING) {
            player.openInventory(e.getInventory());
            return;
        }

        for (Reward reward : crateSession.getRewards()) {
            reward.execute(player);
        }

        plugin.getSessionManager().clearSession(player.getUniqueId());
        plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
    }
}
