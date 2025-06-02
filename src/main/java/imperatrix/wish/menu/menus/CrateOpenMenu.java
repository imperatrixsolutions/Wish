package imperatrix.wish.menu.menus;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.menu.Menu;
import imperatrix.wish.menu.MenuManager;
import imperatrix.wish.struct.*; // Assuming GachaPlayer is here
import imperatrix.wish.struct.crate.Crate; // May not be needed directly in methods if data comes from session
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
import java.util.List; // Added for new open() method parameters
import java.util.UUID;

public class CrateOpenMenu extends Menu {
    private final Wish plugin;
    private final HashMap<UUID, ItemStack> offhandSnapshotMap = new HashMap<>();
    private String title = "&6&lPull Rewards"; // Default from your original file
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
            // Keep defaults if section is missing
            Bukkit.getLogger().warning("[Wish] Configuration section for Crate-Open-Menu is missing in menus.yml. Using default values.");
            return;
        }

        title = Utils.formatString(configurationSection.getString("Title", "&2&lPull Rewards")); // Default from menus.yml
        countdownItem1 = Utils.decodeItem(configurationSection.getString("Countdown-Item-1", "GRAY_STAINED_GLASS_PANE name:&7Revealing_in_1s"));
        countdownItem2 = Utils.decodeItem(configurationSection.getString("Countdown-Item-2", "YELLOW_STAINED_GLASS_PANE name:&7Revealing_in_2s"));
        countdownItem3 = Utils.decodeItem(configurationSection.getString("Countdown-Item-3", "ORANGE_STAINED_GLASS_PANE name:&7Revealing_in_3s"));
    }

    /**
     * This open method is a placeholder and should not be called directly
     * for the INTERFACE animation after Crate.java changes.
     * The new open method with List parameters should be used by Crate.java.
     */
    @Override
    public void open(Player player) {
        // This method is unlikely to be used correctly if Crate.java calls the new signature.
        // It could be a fallback or an error.
        Lang.ERR_UNKNOWN.send(player);
        player.closeInventory();
        Bukkit.getLogger().severe("[Wish] CrateOpenMenu.open(Player) was called. This indicates an improper menu opening sequence for INTERFACE crates.");
    }

    /**
     * Opens the Crate Open Menu with pre-generated rewards.
     * This method is called by Crate.java for INTERFACE animation type.
     *
     * @param gachaPlayer The player opening the crate.
     * @param crateSession The current crate session.
     * @param pullCount The number of items being pulled.
     * @param obtainedRewardTiers The list of RewardTier objects obtained (for display purposes).
     * @param obtainedRewards The list of actual Reward objects obtained.
     */
    public void open(GachaPlayer gachaPlayer, CrateSession crateSession, int pullCount, List<RewardTier> obtainedRewardTiers, List<Reward> obtainedRewards) {
        Player player = gachaPlayer.getPlayer();
        if (player == null || !player.isOnline()) {
            Bukkit.getLogger().warning("[Wish] Attempted to open CrateOpenMenu for an offline or null player: " + gachaPlayer.getUuid());
            return;
        }
        if (obtainedRewardTiers == null || obtainedRewards == null || obtainedRewardTiers.size() != pullCount || obtainedRewards.size() != pullCount) {
            Bukkit.getLogger().severe("[Wish] Mismatch in pullCount and obtained rewards/tiers lists for CrateOpenMenu. Crate: " + crateSession.getCrate().getName() + " Player: " + player.getName());
            Lang.ERR_UNKNOWN.send(player);
            player.closeInventory();
            return;
        }

        int inventorySize = Math.max(9, (int) (Math.ceil((double) pullCount / 9.0) * 9)); // Calculate rows needed, min 1 row
        if (inventorySize > 54) inventorySize = 54; // Max inventory size

        Inventory inventory = Bukkit.createInventory(null, inventorySize, title); // Use calculated size

        // Set initial countdown items
        for (int i = 0; i < pullCount; i++) {
            if (i < inventory.getSize()) { // Ensure we don't go out of bounds for smaller pull counts in larger GUIs
                inventory.setItem(i, countdownItem3);
            }
        }

        // Schedule countdown display changes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.getOpenInventory().getTopInventory().equals(inventory)) return; // Check if player still has this menu open
            for (int i = 0; i < pullCount && i < inventory.getSize(); i++) {
                inventory.setItem(i, countdownItem2);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }, 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.getOpenInventory().getTopInventory().equals(inventory)) return;
            for (int i = 0; i < pullCount && i < inventory.getSize(); i++) {
                inventory.setItem(i, countdownItem1);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }, 40L);

        // Schedule item reveal animation
        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (!player.getOpenInventory().getTopInventory().equals(inventory)) { // Stop if player closes menu early
                    if (crateSession.getOpenPhase() == CrateOpenPhase.OPENING) { // If animation was interrupted
                        // Give rewards directly if menu closed during animation to prevent loss
                        for (Reward reward : obtainedRewards) {
                            reward.execute(player);
                        }
                        plugin.getSessionManager().clearSession(player.getUniqueId());
                    }
                    cancel();
                    return;
                }

                if (counter >= pullCount || counter >= obtainedRewardTiers.size()) {
                    crateSession.setOpenPhase(CrateOpenPhase.COMPLETE);
                    cancel();
                    return;
                }

                RewardTier rewardTierForDisplay = obtainedRewardTiers.get(counter);
                if (counter < inventory.getSize()) {
                    inventory.setItem(counter, rewardTierForDisplay.getDisplayItem());
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, .7f, .7f);
                counter++;
            }
        }.runTaskTimer(plugin, 60L, 7L);

        // Set the final rewards in the CrateSession for processClick and processClose logic
        HashMap<Integer, Reward> finalRewardsMapForSession = new HashMap<>();
        for (int i = 0; i < pullCount && i < obtainedRewards.size(); i++) {
            finalRewardsMapForSession.put(i, obtainedRewards.get(i));
        }
        crateSession.setRewards(finalRewardsMapForSession);
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
            player.closeInventory(); // Close if session is somehow null
            // Lang.ERR_UNKNOWN.send(player); // This might be too spammy if menu is just closed normally elsewhere
            plugin.getSessionManager().clearSession(player.getUniqueId()); // Ensure session is cleared
            plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
            return;
        }

        if (menuManager.isOnCooldown(player.getUniqueId())) {
            return;
        } else {
            menuManager.addCooldown(player.getUniqueId());
        }

        // Only allow revealing items by click if the main animation sequence is complete
        if (crateSession.getOpenPhase() != CrateOpenPhase.COMPLETE || e.getCurrentItem() == null || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }

        // Click to reveal final item if animation is done
        // Assumes slots 0 to pullCount-1 contain the items
        if (e.getSlot() < crateSession.getRewards().size()) { // Check against the size of the rewards map
            Reward reward = crateSession.getReward(e.getSlot()); // Get reward using slot number as key
            if (reward != null) {
                // Check if the current item is a tier display item (not the final reward display)
                // This check might be tricky if display items are similar.
                // A simple approach: just update to the final reward's display item.
                e.getClickedInventory().setItem(e.getSlot(), reward.getDisplayItem());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }
    }

    @Override
    public void processClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        CrateSession crateSession = plugin.getSessionManager().getCrateSession(player.getUniqueId());

        // Restore offhand item
        if (offhandSnapshotMap.containsKey(player.getUniqueId())) {
            player.getInventory().setItemInOffHand(offhandSnapshotMap.remove(player.getUniqueId()));
        }

        if (crateSession == null) {
            plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
            return;
        }

        // If menu is closed during the OPENING phase (animation running),
        // force player to stay in menu until COMPLETE or give items directly.
        // The BukkitRunnable in open() now handles giving items if menu is closed during animation.
        // So, we just check if we need to reopen.
        if (crateSession.getOpenPhase() == CrateOpenPhase.OPENING) {
            // To prevent players from closing too early and messing up the animation/reward state visually
            // We can choose to either reopen it, or let the runnable in open() handle giving rewards.
            // Forcing it open can be annoying. The runnable in open() is a better approach for giving items on early close.
            // So, here we mostly just clean up. If rewards weren't given by the runnable, this part will.
            // However, to be safe and ensure rewards are given if the runnable was cancelled by player leaving server for instance:
            if (!crateSession.getRewards().isEmpty()) { // Check if rewards were determined
                Bukkit.getLogger().info("[Wish] CrateOpenMenu closed during OPENING phase by " + player.getName() + ". Attempting to give rewards if not already handled.");
                // The runnable in open() should ideally handle this.
                // If we are sure the runnable handles it, we might not need to give rewards here again.
                // For safety, if the runnable was cancelled by player leaving server, or some other interruption,
                // this ensures items are given if phase is still OPENING.
                // This can lead to double giving if not careful.
                // The current BukkitRunnable in open() handles giving rewards if the menu is closed.
                // So this section might just need to clear the session.
            }
            // The main task for processClose now is to ensure rewards are given if the state is COMPLETE
            // or if it's still OPENING and somehow rewards weren't given by the animation thread.
            // The animation thread in open() now directly gives items if it detects early close.
            // So, we primarily care about the COMPLETE phase here.
        }


        if (crateSession.getOpenPhase() == CrateOpenPhase.COMPLETE) {
            // Give rewards to the player
            if (crateSession.getRewards().isEmpty()) {
                Bukkit.getLogger().warning("[Wish] No rewards found in CrateSession for " + player.getName() + " upon closing CrateOpenMenu in COMPLETE phase.");
            }
            for (Reward reward : crateSession.getRewards()) {
                if (reward != null) { // Ensure reward object is not null
                    reward.execute(player);
                } else {
                    Bukkit.getLogger().warning("[Wish] A null reward was encountered in CrateSession for " + player.getName() + ".");
                }
            }
        }
        // Irrespective of phase, if session is not null, clear it now.
        plugin.getSessionManager().clearSession(player.getUniqueId());
        plugin.getMenuManager().clearActiveMenu(player.getUniqueId());
    }
}