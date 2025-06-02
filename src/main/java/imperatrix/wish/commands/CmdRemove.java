package imperatrix.wish.commands;

import imperatrix.wish.Wish;
import imperatrix.wish.lang.Lang;
import imperatrix.wish.struct.crate.Crate;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;

public class CmdRemove extends CrateCommand {
    private final Wish plugin;

    public CmdRemove(Wish plugin) {
        super("remove", 0, 0);
        setPermission("wish.admin");
        setPlayerOnly(true);

        this.plugin = plugin;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        HashMap<String, String> replacements = new HashMap<>();
        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlock(null, 5);

        if (targetBlock.isEmpty()) {
            Lang.ERR_NO_BLOCK.send(player);
            return;
        }
        Optional<Crate> optionalCrate = plugin.getCrateCache().getCrate(targetBlock.getLocation());

        if (optionalCrate.isEmpty()) {
            Lang.ERR_NO_CRATE_FOUND.send(player);
            return;
        }

        replacements.put("%crate%", optionalCrate.get().getName());
        optionalCrate.get().removeLocation(targetBlock.getLocation());
        Lang.CRATE_LOCATION_REMOVED.send(player, replacements);
    }
}
