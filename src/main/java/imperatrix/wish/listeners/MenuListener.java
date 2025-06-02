package imperatrix.wish.listeners;

import imperatrix.wish.Wish;
import imperatrix.wish.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {
    private final Wish plugin;

    public MenuListener(Wish plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Menu menu = plugin.getMenuManager().getActiveMenu(player.getUniqueId());

        if (menu == null) {
            return;
        }

        e.setCancelled(true);
        menu.processClick(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        Menu menu = plugin.getMenuManager().getActiveMenu(player.getUniqueId());

        if (menu == null) {
            return;
        }

        menu.processClose(e);
    }
}
