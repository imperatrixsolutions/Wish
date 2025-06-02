package imperatrix.wish.commands;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.struct.GachaPlayer;
import imperatrix.wish.struct.crate.Crate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;

public class CmdGive extends CrateCommand {
    private final Wish plugin;

    public CmdGive(Wish plugin) {
        super("give", 2, 3);
        setPermission("wish.admin");

        this.plugin = plugin;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[0]);
        HashMap<String, String> replacements = new HashMap<>();

        if (target == null) {
            replacements.put("%player%", args[0]);
            Lang.ERR_PLAYER_OFFLINE.send(sender, replacements);
            return;
        }
        Optional<Crate> optionalCrate = plugin.getCrateCache().getCrate(args[1]);

        if (optionalCrate.isEmpty()) {
            replacements.put("%crate%", args[1]);
            Lang.ERR_UNKNOWN_CRATE.send(sender, replacements);
            return;
        }
        GachaPlayer gachaPlayer = plugin.getPlayerCache().getPlayer(target.getUniqueId());
        Crate crate = optionalCrate.get();
        int amount = 1;

        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (IllegalArgumentException e) {
                replacements.put("%arg%", args[2]);
                Lang.ERR_INVALID_AMOUNT.send(sender, replacements);
                return;
            }
        }

        if (amount < 1) {
            replacements.put("%arg%", args[2]);
            Lang.ERR_INVALID_AMOUNT.send(sender, replacements);
            return;
        }

        replacements.put("%crate%", crate.getName());
        replacements.put("%player%", target.getName());
        replacements.put("%amount%", Integer.toString(amount));
        Lang.CRATE_GIVEN.send(sender, replacements);
        Lang.CRATE_RECEIVED.send(target, replacements);
        gachaPlayer.setAvailablePulls(crate, gachaPlayer.getAvailablePulls(crate) + amount);
    }
}
