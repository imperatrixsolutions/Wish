package imperatrix.wish.lang;

import imperatrix.wish.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Lang {
    ERR_ALREADY_CRATE("err-already-crate", "A wish already exists here", LangType.NORMAL),
    ERR_CRATE_IN_USE("err-crate-in-use", "This wish is already being opened, try again later", LangType.NORMAL),
    ERR_INVALID_AMOUNT("err-invalid-amount", "%arg% must be a number above 0", LangType.NORMAL),
    ERR_NO_BLOCK("err-no-block", "Unable to fin a target block", LangType.NORMAL),
    ERR_NO_CRATE_FOUND("err-no-crate-found", "No crate was found here", LangType.NORMAL),
    ERR_NOT_ENOUGH_PULLS("err-not-enough-pulls", "You don't have enough pulls to open this", LangType.NORMAL),
    ERR_NOT_PLAYER("err-not-player", "You must be a player to use this command", LangType.NORMAL),
    ERR_OPENING_CRATE("err-opening-crate", "You are already make a wish", LangType.NORMAL),
    ERR_PLAYER_OFFLINE("err-player-offline", "%player% is not online", LangType.NORMAL),
    ERR_MISSING_PERM("err-missing-perm", "You don't have permission to use this command", LangType.NORMAL),
    ERR_UNKNOWN_CRATE("err-unknown-crate", "No crate named '%crate%' was found", LangType.NORMAL),
    ERR_UNKNOWN("err-unknown", "An unknown error has occurred", LangType.NORMAL),

    CRATE_CONFIRM_DELETE("crate-confirm-delete", "Confirm deletion of crate &a%crate% &fby breaking again within &a3s", LangType.NORMAL),
    CRATE_GIVEN("crate-given", "Gave &a%player% %amount%x %crate% &fpulls", LangType.NORMAL),
    CRATE_GIVEN_TO_ALL("crate-given-to-all", "Gave all online players &a%amount%x %crate% &fpulls", LangType.NORMAL),
    CRATE_RECEIVED("crate-received", "Received &a%amount%x %crate% &fpulls", LangType.NORMAL),
    CRATE_LIST("crate-list", "%list%", LangType.NORMAL),
    CRATE_LOCATION_ADDED("crate-location-added", "Crate location added for &a%crate%", LangType.NORMAL),
    CRATE_LOCATION_REMOVED("crate-location-removed", "Crate location removed for &a%crate%", LangType.NORMAL),
    CRATE_LOST("crate-lost", "Lost &a%amount%x %crate% &fpulls", LangType.NORMAL),
    CRATE_PULL_LIST("crate-pull-list", LangType.LONG),
    CRATE_TAKEN("crate-taken", "Took &a%amount%x %crate% &fpulls from &a%player%", LangType.NORMAL),
    CRATE_USAGE("crate-usage", LangType.LONG),

    PITY_TRACKER_FORMAT("pity-tracker-format", "&f  %pity-count%&7/&8%pity-limit% &7%reward-tier%", LangType.NORMAL),
    PULL_LIST_FORMAT("pull-list-format", "&a  %crate%&7: &f%pull-count%", LangType.NORMAL),
    TIER_RATE_FORMAT("tier-rate-format", "&a  %reward-tier% &7%rate%%", LangType.NORMAL),

    PREFIX("prefix", "&2Wish &8\u00BB &f", LangType.NORMAL);

    private final String path;
    private final String def;
    private final LangType langType;
    private static FileConfiguration fileConfiguration;

    Lang(String path, LangType langType) {
        this.path = path;
        this.def = "";
        this.langType = langType;
    }

    Lang(String path, String def, LangType langType) {
        this.path = path;
        this.def = def;
        this.langType = langType;
    }

    public void send(CommandSender sender) {
        switch (langType) {
            case NORMAL:
                sender.sendMessage(toString());
                break;
            case LONG:
                for (String str : toStringList()) {
                    sender.sendMessage(str);
                }
                break;
        }
    }

    public void send(CommandSender sender, boolean addPrefix) {
        switch (langType) {
            case NORMAL -> sender.sendMessage(toString(addPrefix));
            case LONG -> send(sender);
        }
    }

    public void send(CommandSender sender, HashMap<String, String> replacements) {
        switch (langType) {
            case NORMAL -> sender.sendMessage(toString(replacements));
            case LONG -> {
                List<String> messages = new ArrayList<>();
                toStringList().forEach((s) -> {
                    for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                        s = s.replace(replacement.getKey(), replacement.getValue());
                    }

                    messages.add(Utils.formatString(s));
                });
                messages.forEach(sender::sendMessage);
            }
        }
    }

    public static void setFileConfiguration(FileConfiguration fileConfiguration) {
        Lang.fileConfiguration = fileConfiguration;
    }

    public List<String> toStringList() {
        List<String> coloredList = new ArrayList<>();

        for (String message : fileConfiguration.getStringList(path)) {
            coloredList.add(Utils.formatString(message));
        }

        return coloredList;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean addPrefix) {
        String message = "";

        if (this != PREFIX && addPrefix) {
            message += PREFIX.toString();
        }

        message += fileConfiguration.getString(path, def);
        return Utils.formatString(message);
    }

    public String toString(HashMap<String, String> replacements) {
        String message = toString();

        for (String key : replacements.keySet()) {
            message = message.replace(key, replacements.get(key));
        }

        return message;
    }

    public String toString(HashMap<String, String> replacements, boolean addPrefix) {
        String message = toString(addPrefix);

        for (String key : replacements.keySet()) {
            message = message.replace(key, replacements.get(key));
        }

        return message;
    }
}
