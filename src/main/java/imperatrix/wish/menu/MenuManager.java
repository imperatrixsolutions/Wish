package imperatrix.wish.menu;

import javax.annotation.Nullable;
import java.util.*;

public class MenuManager {
    private final Set<Menu> menus = new HashSet<>();
    private final HashMap<UUID, Menu> activeMenuMap = new HashMap<>();
    private final HashMap<UUID, Long> clickCooldownMap = new HashMap<>();

    public void addCooldown(UUID uuid) {
        clickCooldownMap.put(uuid, System.currentTimeMillis());
    }

    public void addMenu(Menu... menus) {
        this.menus.addAll(Arrays.asList(menus));
    }

    public void clearActiveMenu(UUID uuid) {
        activeMenuMap.remove(uuid);
    }

    @Nullable
    public Menu getActiveMenu(UUID uuid) {
        return activeMenuMap.get(uuid);
    }

    public Optional<Menu> getMenu(String menuId) {
        return menus.stream().filter((m) -> m.getMenuID().equals(menuId)).findFirst();
    }

    public boolean isOnCooldown(UUID uuid) {
        if (!clickCooldownMap.containsKey(uuid)) {
            return false;
        }
        long lastClick = clickCooldownMap.get(uuid);

        if (System.currentTimeMillis() - lastClick < 100) {
            return true;
        }

        clickCooldownMap.remove(uuid);
        return false;
    }

    public void setActiveMenu(UUID uuid, Menu menu) {
        activeMenuMap.put(uuid, menu);
    }
}
