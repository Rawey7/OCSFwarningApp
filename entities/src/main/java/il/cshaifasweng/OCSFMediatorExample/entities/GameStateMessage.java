package il.cshaifasweng.OCSFMediatorExample.entities;


import java.io.Serializable;

public class GameStateMessage implements Serializable {
    private final char[][] board;      // 3x3
    private final char currentTurn;    // 'X' or 'O'
    private final GameStatus status;
    private final String info;

    public GameStateMessage(char[][] board, char currentTurn, GameStatus status, String info) {
        this.board = board;
        this.currentTurn = currentTurn;
        this.status = status;
        this.info = info;
    }

    public char[][] getBoard() {
        return board;
    }

    public char getCurrentTurn() {
        return currentTurn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getInfo() {
        return info;
    }
}
