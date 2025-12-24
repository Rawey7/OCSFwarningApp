package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class PrimaryController {

    @FXML private Label statusLabel;

    @FXML private Button b00; @FXML private Button b01; @FXML private Button b02;
    @FXML private Button b10; @FXML private Button b11; @FXML private Button b12;
    @FXML private Button b20; @FXML private Button b21; @FXML private Button b22;

    private Button[][] buttons;

    private char mySymbol = '?';
    private boolean myTurn = false;

    @FXML
    void initialize() {
        buttons = new Button[][]{
                {b00, b01, b02},
                {b10, b11, b12},
                {b20, b21, b22}
        };

        EventBus.getDefault().register(this);

        try {
            SimpleClient.getClient().openConnection();

            TextInputDialog d = new TextInputDialog("Player");
            d.setHeaderText("Enter player name");
            String name = d.showAndWait().orElse("Player");

            SimpleClient.getClient().joinGame(name);
            statusLabel.setText("Joined. Waiting...");

            setBoardDisabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Connection error.");
        }
    }

    @FXML
    private void cellClicked(ActionEvent event) {
        if (!myTurn) return;
        Button clicked = (Button) event.getSource();

        int row = -1, col = -1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (buttons[r][c] == clicked) {
                    row = r; col = c;
                }
            }
        }
        if (row == -1) return;

        try {
            SimpleClient.getClient().sendToServer(new MoveMessage(row, col));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onAssign(AssignSymbolMessage msg) {
        Platform.runLater(() -> {
            mySymbol = msg.getSymbol();
            myTurn = msg.isYourTurn();
            statusLabel.setText("You are " + mySymbol + (myTurn ? " (your turn)" : " (wait)"));
            setBoardDisabled(!myTurn);
        });
    }

    @Subscribe
    public void onGameState(GameStateMessage msg) {
        Platform.runLater(() -> {
            // update board
            char[][] board = msg.getBoard();
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    buttons[r][c].setText(String.valueOf(board[r][c]));
                }
            }

            statusLabel.setText(msg.getInfo() + " | Status: " + msg.getStatus());

            // your turn?
            myTurn = (mySymbol != '?' && msg.getStatus() == GameStatus.PLAYING && msg.getCurrentTurn() == mySymbol);
            setBoardDisabled(!myTurn || msg.getStatus() != GameStatus.PLAYING);
        });
    }

    @Subscribe
    public void onError(ServerErrorMessage msg) {
        Platform.runLater(() -> statusLabel.setText("ERROR: " + msg.getMessage()));
    }

    private void setBoardDisabled(boolean disabled) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                buttons[r][c].setDisable(disabled);
            }
        }
    }
}
