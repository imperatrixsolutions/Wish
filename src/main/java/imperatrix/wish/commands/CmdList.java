package imperatrix.wish.commands;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.StringJoiner;

public class CmdList extends CrateCommand {
    private final Wish plugin;

    public CmdList(Wish plugin) {
        super("list", 0, 0);
        setPermission("wish.admin");

        this.plugin = plugin;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        StringJoiner listJoiner = new StringJoiner(", ");
        HashMap<String, String> replacements = new HashMap<>();

        plugin.getCrateCache().getCrates().forEach(c -> listJoiner.add(c.getName()));
        replacements.put("%list%", listJoiner.toString());
        Lang.CRATE_LIST.send(sender, replacements);
    }
}
