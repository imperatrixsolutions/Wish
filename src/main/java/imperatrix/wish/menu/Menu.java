package imperatrix.wish.menu;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import javax.annotation.Nullable;

public abstract class Menu {
    private final String menuID;

    public Menu(String menuID) {
        this.menuID = menuID;
    }

    public String getMenuID() {
        return menuID;
    }

    public abstract void load(@Nullable ConfigurationSection configurationSection);

    public abstract void open(Player player);

    public abstract void processClick(InventoryClickEvent e);

    public abstract void processClose(InventoryCloseEvent e);
}
