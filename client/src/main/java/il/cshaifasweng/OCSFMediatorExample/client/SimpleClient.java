package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

public class SimpleClient extends AbstractClient {

    private static SimpleClient client = null;

    private SimpleClient(String host, int port) {
        super(host, port);
    }

    // ✅ singleton access (REQUIRED)
    public static SimpleClient getClient() {
        if (client == null) {
            client = new SimpleClient("localhost", 3000);
        }
        return client;
    }

    // called from controller
    public void joinGame(String name) {
        try {
            sendToServer(new JoinGameRequest(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleMessageFromServer(Object msg) {

        if (msg instanceof AssignSymbolMessage ||
                msg instanceof GameStateMessage ||
                msg instanceof ServerErrorMessage) {

            EventBus.getDefault().post(msg);
            return;
        }

        System.out.println(msg);
    }
}
