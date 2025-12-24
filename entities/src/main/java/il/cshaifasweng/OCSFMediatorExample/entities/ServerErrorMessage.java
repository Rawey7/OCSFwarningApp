package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class ServerErrorMessage implements Serializable {
    private final String message;

    public ServerErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}