package imperatrix.wish.commands;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.struct.GachaPlayer;
import imperatrix.wish.struct.crate.Crate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Optional;

public class CmdGiveAll extends CrateCommand {
    public final Wish plugin;

    public CmdGiveAll(Wish plugin) {
        super("giveall", 1, 2);
        setPermission("wish.admin");

        this.plugin = plugin;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        Optional<Crate> optionalCrate = plugin.getCrateCache().getCrate(args[0]);
        HashMap<String, String> replacements = new HashMap<>();

        if (optionalCrate.isEmpty()) {
            replacements.put("%crate%", args[0]);
            Lang.ERR_UNKNOWN_CRATE.send(sender, replacements);
            return;
        }
        Crate crate = optionalCrate.get();
        int amount = 1;

        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (IllegalArgumentException e) {
                replacements.put("%arg%", args[1]);
                Lang.ERR_INVALID_AMOUNT.send(sender, replacements);
                return;
            }
        }

        if (amount < 1) {
            replacements.put("%arg%", args[1]);
            Lang.ERR_INVALID_AMOUNT.send(sender, replacements);
            return;
        }
        int finalAmount = amount;

        replacements.put("%crate%", crate.getName());
        replacements.put("%amount%", Integer.toString(amount));
        Bukkit.getOnlinePlayers().forEach((p) -> {
            GachaPlayer gachaPlayer = plugin.getPlayerCache().getPlayer(p.getUniqueId());

            gachaPlayer.setAvailablePulls(crate, gachaPlayer.getAvailablePulls(crate) + finalAmount);
            Lang.CRATE_RECEIVED.send(p, replacements);
        });
        Lang.CRATE_GIVEN_TO_ALL.send(sender, replacements);
    }
}
