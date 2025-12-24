package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class AssignSymbolMessage implements Serializable {
    private final char symbol;       // 'X' or 'O'
    private final boolean yourTurn;  // true if you start

    public AssignSymbolMessage(char symbol, boolean yourTurn) {
        this.symbol = symbol;
        this.yourTurn = yourTurn;
    }

    public char getSymbol() {
        return symbol;
    }

    public boolean isYourTurn() {
        return yourTurn;
    }
}
