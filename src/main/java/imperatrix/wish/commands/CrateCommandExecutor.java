package imperatrix.wish.commands;

import imperatrix.wish.lang.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CrateCommandExecutor implements CommandExecutor {
    private final Set<CrateCommand> commands = new HashSet<>();

    public void addCommand(CrateCommand crateCommand){
        this.commands.add(crateCommand);
    }

    public void addCommands(CrateCommand... crateCommands) {
        this.commands.addAll(Arrays.asList(crateCommands));
    }

    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String label, @Nonnull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        Optional<CrateCommand> optionalCommand = commands.stream().filter((c) -> c.hasAlias(args[0])).findFirst();

        if (optionalCommand.isEmpty()) {
            sendUsage(sender);
            return true;
        }
        CrateCommand command = optionalCommand.get();
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);

        if (command.isPlayerOnly() && !(sender instanceof Player)) {
            Lang.ERR_NOT_PLAYER.send(sender);
            return true;
        }

        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) {
            Lang.ERR_MISSING_PERM.send(sender);
            return true;
        }

        if (newArgs.length < command.getMinArgs() || newArgs.length > command.getMaxArgs()) {
            sendUsage(sender);
            return true;
        }

        command.run(sender, newArgs);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        Lang.CRATE_USAGE.send(sender);
    }
}
