package imperatrix.wish.listeners;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.menu.Menu;
import imperatrix.wish.struct.crate.Crate;
import imperatrix.wish.struct.crate.CrateOpenPhase;
import imperatrix.wish.struct.crate.CrateSession;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class CrateListener implements Listener {
    private final Wish plugin;
    private final Set<UUID> destroyingCrate = new HashSet<>();
    private final HashMap<UUID, UUID> crateDestructionMap = new HashMap<>();
    private final HashMap<UUID, Integer> taskMap = new HashMap<>();

    public CrateListener(Wish plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Block clickedBlock = e.getClickedBlock();

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null) {
            return;
        }
        Optional<Crate> crate = plugin.getCrateCache().getCrate(clickedBlock.getLocation());

        if (crate.isEmpty()) {
            return;
        }
        Player player = e.getPlayer();
        CrateSession crateSession = plugin.getSessionManager().getCrateSession(player.getUniqueId());
        Optional<Menu> crateMenu = plugin.getMenuManager().getMenu("crate");

        if (crateSession == null) {
            crateSession = new CrateSession(player.getUniqueId(), crate.get(), clickedBlock.getLocation());
        } else {
            crateSession.setCrate(crate.get());
            crateSession.setCrateLocation(clickedBlock.getLocation());
        }

        e.setCancelled(true);
        if (crate.get().isCrateLocationInUse(clickedBlock.getLocation())) {
            Lang.ERR_CRATE_IN_USE.send(player);
            return;
        }

        if (crateSession.getOpenPhase() == CrateOpenPhase.OPENING) {
            Lang.ERR_OPENING_CRATE.send(player);
            return;
        }

        if (crateMenu.isEmpty()) {
            Lang.ERR_UNKNOWN.send(player);
            return;
        }

        plugin.getSessionManager().registerSession(crateSession);
        crateMenu.get().open(player);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();
        Optional<Crate> crate = plugin.getCrateCache().getCrate(block.getLocation());
        HashMap<String, String> messageReplacements = new HashMap<>();

        if (crate.isEmpty()) {
            return;
        }

        if (!player.hasPermission("gachacrates.admin.removelocation")) {
            Lang.ERR_MISSING_PERM.send(player);
            e.setCancelled(true);
            return;
        }

        messageReplacements.put("%crate%", crate.get().getName());
        if (!destroyingCrate.contains(player.getUniqueId())) {
            e.setCancelled(true);
            startDestruction(player, crate.get());
            Lang.CRATE_CONFIRM_DELETE.send(player, messageReplacements);
            return;
        }
        Optional<Crate> selectedCrate = plugin.getCrateCache().getCrate(crateDestructionMap.get(player.getUniqueId()));

        if (selectedCrate.isEmpty()) {
            e.setCancelled(true);
            destroyingCrate.remove(player.getUniqueId());
            crateDestructionMap.remove(player.getUniqueId());
            Lang.ERR_UNKNOWN.send(player);
            return;
        }

        if (!selectedCrate.get().getUuid().equals(crate.get().getUuid())) {
            e.setCancelled(true);
            startDestruction(player, crate.get());
            Lang.CRATE_CONFIRM_DELETE.send(player, messageReplacements);
            return;
        }

        crate.get().removeLocation(block.getLocation());
        Lang.CRATE_LOCATION_REMOVED.send(player, messageReplacements);
    }

    private void startDestruction(Player player, Crate crate) {
        if (taskMap.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(taskMap.get(player.getUniqueId()));
            taskMap.remove(player.getUniqueId());
        }

        destroyingCrate.add(player.getUniqueId());
        crateDestructionMap.put(player.getUniqueId(), crate.getUuid());
        taskMap.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            destroyingCrate.remove(player.getUniqueId());
            crateDestructionMap.remove(player.getUniqueId());
        }, 60L).getTaskId());
    }
}
