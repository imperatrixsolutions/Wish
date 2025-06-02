package imperatrix.wish.struct;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import imperatrix.wish.Wish;
import imperatrix.wish.struct.crate.Crate;
import imperatrix.wish.struct.GachaPlayer;

import java.util.Optional;

public class WishPlaceholderExpansion extends PlaceholderExpansion {

    private final Wish plugin;

    public WishPlaceholderExpansion(Wish plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "wish";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // Parse the placeholder for pulls left for a specific crate
        if (identifier.startsWith("pulls_")) {
            String crateName = identifier.substring("pulls_".length());
            Optional<Crate> optionalCrate = plugin.getCrateCache().getCrate(crateName);

            if (optionalCrate.isPresent()) {
                Crate crate = optionalCrate.get();
                return String.valueOf(getPullsForCrate(player, crate));
            } else {
                return "Invalid crate name";
            }
        }

        return null; // Placeholder not found
    }

    // Method to get the number of pulls left for a player in a specific crate
    public int getPullsForCrate(Player player, Crate crate) {
        GachaPlayer gachaPlayer = plugin.getPlayerCache().getPlayer(player.getUniqueId());
        return gachaPlayer.getAvailablePulls(crate);
    }
}



