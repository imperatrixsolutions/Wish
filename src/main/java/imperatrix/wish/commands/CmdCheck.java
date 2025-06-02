package imperatrix.wish.commands;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.struct.GachaPlayer;
import imperatrix.wish.struct.crate.Crate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CmdCheck extends CrateCommand {
    private final Wish plugin;

    public CmdCheck(Wish plugin) {
        super("check", 0, 1);
        setPermission("wish.default");

        this.plugin = plugin;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        Player target;
        HashMap<String, String> replacements = new HashMap<>();

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                Lang.ERR_NOT_PLAYER.send(sender);
                return;
            }

            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                replacements.put("%player%", args[0]);
                Lang.ERR_PLAYER_OFFLINE.send(sender, replacements);
                return;
            }
        }

        for (String line : Lang.CRATE_PULL_LIST.toStringList()) {
            if (line.contains("%pull-list%")) {
                getPullList(plugin.getPlayerCache().getPlayer(target.getUniqueId())).forEach(sender::sendMessage);
                continue;
            }

            sender.sendMessage(line);
        }
    }

    private List<String> getPullList(GachaPlayer gachaPlayer) {
        List<String> pullList = new ArrayList<>();
        HashMap<String, String> replacements = new HashMap<>();

        for (Crate crate : plugin.getCrateCache().getCrates()) {
            replacements.put("%crate%", crate.getName());
            replacements.put("%pull-count%", Integer.toString(gachaPlayer.getAvailablePulls(crate)));
            pullList.add(Lang.PULL_LIST_FORMAT.toString(replacements, false));
        }

        return pullList;
    }
}
