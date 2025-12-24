package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class JoinGameRequest implements Serializable {
    private final String playerName;

    public JoinGameRequest(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
