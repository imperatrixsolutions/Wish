package imperatrix.wish;

import imperatrix.wish.cache.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import imperatrix.wish.commands.*;
import imperatrix.wish.file.CustomFile;
import imperatrix.wish.file.FileManager;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.listeners.CrateListener;
import imperatrix.wish.listeners.MenuListener;
import imperatrix.wish.listeners.PlayerListener;
import imperatrix.wish.menu.menus.CrateMenu;
import imperatrix.wish.menu.MenuManager;
import imperatrix.wish.menu.menus.CrateOpenMenu;
import imperatrix.wish.menu.menus.PullMenu;
import imperatrix.wish.menu.menus.RewardsMenu;
import imperatrix.wish.struct.WishPlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Wish extends JavaPlugin {
    private final CrateCache crateCache = new CrateCache();
    private final PlayerCache playerCache = new PlayerCache(this);
    private final FileManager fileManager = new FileManager(this);
    private final MenuManager menuManager = new MenuManager();
    private final SessionManager sessionManager = new SessionManager();

    @Override
    public void onEnable() {
        registerConfig();
        registerCommands();
        registerListeners();
        registerMenus();

        // Înregistrarea expansiunii PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WishPlaceholderExpansion(this).register();
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholder expansion will not be registered.");
        }
    }

    @Override
    public void onDisable() {
        playerCache.saveTo(fileManager.getFile("data"));
        crateCache.saveTo(fileManager.getFile("crates"));
    }

    /**
     * Retrieve the crate cache, used for managing crates cached in memory
     *
     * @return CrateCache
     */
    public CrateCache getCrateCache() {
        return crateCache;
    }

    /**
     * Retrieve the menu manager, used for tracking active menus and finding menus
     *
     * @return MenuManager
     */
    public MenuManager getMenuManager() {
        return menuManager;
    }

    /**
     * Retrieve the player cache, used for retrieving GachaPlayers
     *
     * @return PlayerCache
     */
    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    /**
     * Retrieve the session manager, used for holding info regarding active crates
     *
     * @return SessionManager
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    private void registerCommands() {
        PluginCommand crateCommand = getCommand("wish");
        CrateCommandExecutor commandExecutor = new CrateCommandExecutor();

        commandExecutor.addCommands(new CmdSet(this), new CmdRemove(this), new CmdGive(this), new CmdTake(this),
                new CmdGiveAll(this), new CmdList(this), new CmdCheck(this));
        if (crateCommand != null) {
            crateCommand.setExecutor(commandExecutor);
        }
    }

    private void registerConfig() {
        CustomFile cratesFile = fileManager.getFile("crates");
        CustomFile dataFile = fileManager.getFile("data");
        CustomFile langFile = fileManager.getFile("lang");
        CustomFile menusFile = fileManager.getFile("menus");

        saveDefaultConfig();
        cratesFile.saveDefaultConfig();
        dataFile.saveDefaultConfig();
        langFile.saveDefaultConfig();
        menusFile.saveDefaultConfig();

        GachaConfig.load(getConfig());
        GachaConfig.validateConfig(ConfigType.MENUS, menusFile);
        crateCache.loadFrom(cratesFile.getConfig());
        playerCache.setFile(dataFile.getConfig());
        Lang.setFileConfiguration(langFile.getConfig());
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CrateListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
    }

    private void registerMenus() {
        CustomFile menusFile = fileManager.getFile("menus");
        CrateMenu crateMenu = new CrateMenu(this);
        PullMenu pullMenu = new PullMenu(this);
        RewardsMenu rewardsMenu = new RewardsMenu(this);
        CrateOpenMenu crateOpenMenu = new CrateOpenMenu(this);

        crateMenu.load(menusFile.getConfig().getConfigurationSection("Crate-Menu"));
        pullMenu.load(menusFile.getConfig().getConfigurationSection("Pull-Menu"));
        rewardsMenu.load(menusFile.getConfig().getConfigurationSection("Rewards-Menu"));
        crateOpenMenu.load(menusFile.getConfig().getConfigurationSection("Crate-Open-Menu"));
        menuManager.addMenu(crateMenu, pullMenu, rewardsMenu, crateOpenMenu);
    }
}
