package imperatrix.wish.listeners;

import imperatrix.wish.Wish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {
    private final Wish plugin;

    public PlayerListener(Wish plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Ensure player data is loaded on join
        plugin.getPlayerCache().getPlayer(e.getPlayer().getUniqueId());
    }
}
