package imperatrix.wish.cache;

import imperatrix.wish.struct.crate.CrateSession;

import javax.annotation.Nullable;
import java.util.*;

public class SessionManager {
    private final HashMap<UUID, CrateSession> crateSessions = new HashMap<>();

    public void clearSession(UUID playerUuid) {
        crateSessions.remove(playerUuid);
    }

    @Nullable
    public CrateSession getCrateSession(UUID playerUuid) {
        return crateSessions.get(playerUuid);
    }

    public void registerSession(CrateSession crateSession) {
        crateSessions.put(crateSession.getPlayerUuid(), crateSession);
    }
}