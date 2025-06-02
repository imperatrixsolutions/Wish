package imperatrix.wish.commands;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CrateCommand {
    private final String label;
    private String permission = null;
    private final int maxArgs;
    private final int minArgs;
    private boolean playerOnly = false;
    private final List<String> aliases = new ArrayList<>();

    public CrateCommand(String label, int minArgs, int maxArgs) {
        this.label = label;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    public void addAliases(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
    }

    public String getLabel() {
        return label;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public int getMinArgs() {
        return minArgs;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasAlias(String str) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(str)) {
                return true;
            }
        }

        return label.equalsIgnoreCase(str);
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public abstract void run(CommandSender sender, String[] args);

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setPlayerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
    }
}
