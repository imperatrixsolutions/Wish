package imperatrix.wish.commands;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.struct.crate.Crate;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;

public class CmdSet extends CrateCommand {
    private final Wish plugin;

    public CmdSet(Wish plugin) {
        super("set", 1, 1);
        setPermission("wish.admin");
        setPlayerOnly(true);

        this.plugin = plugin;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        Optional<Crate> crate = plugin.getCrateCache().getCrate(args[0]);
        HashMap<String, String> replacements = new HashMap<>();
        Player player = (Player) sender;

        if (crate.isEmpty()) {
            replacements.put("%crate%", args[0]);
            Lang.ERR_UNKNOWN_CRATE.send(player, replacements);
            return;
        } else {
            replacements.put("%crate%", crate.get().getName());
        }
        Block targetBlock = player.getTargetBlock(null, 5);

        if (targetBlock.isEmpty()) {
            Lang.ERR_NO_BLOCK.send(player);
            return;
        }

        if (plugin.getCrateCache().getCrate(targetBlock.getLocation()).isPresent()) {
            Lang.ERR_ALREADY_CRATE.send(player);
            return;
        }

        crate.get().addLocation(targetBlock.getLocation());
        Lang.CRATE_LOCATION_ADDED.send(player, replacements);
    }
}
