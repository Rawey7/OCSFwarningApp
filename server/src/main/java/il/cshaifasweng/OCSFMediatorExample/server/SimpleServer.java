package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;

public class SimpleServer extends AbstractServer {

    private ConnectionToClient playerX = null;
    private ConnectionToClient playerO = null;

    private final char[][] board = new char[3][3];
    private char currentTurn = 'X';
    private GameStatus status = GameStatus.WAITING_FOR_PLAYER;

    public SimpleServer(int port) {
        super(port);
        resetGame();
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {

        if (msg instanceof JoinGameRequest) {
            handleJoin((JoinGameRequest) msg, client);
            return;
        }

        if (msg instanceof MoveMessage) {
            handleMove((MoveMessage) msg, client);
            return;
        }

        sendSafe(client, new ServerErrorMessage(
                "Unknown message type: " + msg.getClass().getSimpleName()
        ));
    }

    // ✅ RANDOM X/O assignment:
    // First join gets random symbol (X or O), second gets the other. X always starts.
    private void handleJoin(JoinGameRequest req, ConnectionToClient client) {

        // already joined?
        if (client.equals(playerX) || client.equals(playerO)) {
            sendCurrentState(client, "You are already in the game.");
            return;
        }

        // game empty -> random assign first player
        if (playerX == null && playerO == null) {
            boolean firstIsX = Math.random() < 0.5;

            if (firstIsX) {
                playerX = client;
                sendSafe(playerX, new AssignSymbolMessage('X', false));
            } else {
                playerO = client;
                sendSafe(playerO, new AssignSymbolMessage('O', false));
            }

            status = GameStatus.WAITING_FOR_PLAYER;
            sendCurrentState(client, "Waiting for second player...");
            return;
        }

        // second player joins -> fill the missing slot
        if (playerX == null) {
            playerX = client;
        } else if (playerO == null) {
            playerO = client;
        } else {
            sendSafe(client, new ServerErrorMessage("Game is full (already 2 players)."));
            return;
        }

        // start game (X begins)
        resetGame();
        status = GameStatus.PLAYING;
        currentTurn = 'X';

        sendSafe(playerX, new AssignSymbolMessage('X', true));   // X starts
        sendSafe(playerO, new AssignSymbolMessage('O', false));

        broadcastState("Game started! X begins.");
    }

    private void handleMove(MoveMessage move, ConnectionToClient client) {

        if (status != GameStatus.PLAYING) {
            sendCurrentState(client, "Game not started yet. Wait for second player.");
            return;
        }

        char mySymbol = getSymbolOf(client);
        if (mySymbol == '?') {
            sendSafe(client, new ServerErrorMessage("You are not a player in this game."));
            return;
        }

        if (mySymbol != currentTurn) {
            sendCurrentState(client, "Not your turn.");
            return;
        }

        int r = move.getRow();
        int c = move.getCol();

        if (r < 0 || r > 2 || c < 0 || c > 2) {
            sendCurrentState(client, "Invalid cell.");
            return;
        }

        if (board[r][c] != ' ') {
            sendCurrentState(client, "Cell already taken.");
            return;
        }

        // apply move
        board[r][c] = mySymbol;

        // check end conditions
        if (isWinner(mySymbol)) {
            status = (mySymbol == 'X') ? GameStatus.X_WON : GameStatus.O_WON;
            broadcastState("Player " + mySymbol + " won!");
            return;
        }

        if (isDraw()) {
            status = GameStatus.DRAW;
            broadcastState("Draw!");
            return;
        }

        // switch turn
        currentTurn = (currentTurn == 'X') ? 'O' : 'X';
        broadcastState("Turn: " + currentTurn);
    }

    private char getSymbolOf(ConnectionToClient client) {
        if (client.equals(playerX)) return 'X';
        if (client.equals(playerO)) return 'O';
        return '?';
    }

    private void broadcastState(String info) {
        GameStateMessage gsm = new GameStateMessage(copyBoard(), currentTurn, status, info);
        sendSafe(playerX, gsm);
        sendSafe(playerO, gsm);
    }

    private void sendCurrentState(ConnectionToClient client, String info) {
        GameStateMessage gsm = new GameStateMessage(copyBoard(), currentTurn, status, info);
        sendSafe(client, gsm);
    }

    private void sendSafe(ConnectionToClient client, Object obj) {
        if (client == null) return;
        try {
            client.sendToClient(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetGame() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
        currentTurn = 'X';
        status = GameStatus.WAITING_FOR_PLAYER;
    }

    private char[][] copyBoard() {
        char[][] copy = new char[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, 3);
        }
        return copy;
    }

    private boolean isWinner(char s) {
        // rows
        for (int r = 0; r < 3; r++) {
            if (board[r][0] == s && board[r][1] == s && board[r][2] == s) return true;
        }
        // cols
        for (int c = 0; c < 3; c++) {
            if (board[0][c] == s && board[1][c] == s && board[2][c] == s) return true;
        }
        // diagonals
        return (board[0][0] == s && board[1][1] == s && board[2][2] == s)
                || (board[0][2] == s && board[1][1] == s && board[2][0] == s);
    }

    private boolean isDraw() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == ' ') return false;
            }
        }
        return true;
    }

    // ✅ Disconnect handling (keeps server reusable)
    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        handleDisconnect(client);
    }

    @Override
    protected void clientException(ConnectionToClient client, Throwable exception) {
        handleDisconnect(client);
    }

    private void handleDisconnect(ConnectionToClient client) {
        if (client == null) return;

        // who remains (if any)?
        ConnectionToClient remaining =
                (client.equals(playerX)) ? playerO :
                        (client.equals(playerO)) ? playerX :
                                null;

        // clear both slots
        if (client.equals(playerX)) playerX = null;
        if (client.equals(playerO)) playerO = null;

        // reset game
        resetGame();

        // keep remaining player as X waiting for a new O
        if (remaining != null) {
            playerX = remaining;
            playerO = null;

            sendSafe(playerX, new AssignSymbolMessage('X', false));
            sendCurrentState(playerX, "Other player disconnected. Waiting for new player...");
        }
    }
}
