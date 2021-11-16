package publicdomain.webhidpolyfill;

import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import publicdomain.webhidpolyfill.events.ReceiveReportEvent;
import publicdomain.webhidpolyfill.events.SendReportEvent;

public class WebsocketServer extends WebSocket {
    private final TimerTask pingTimer;
    private final EventBus eventBus;

    public WebsocketServer(NanoHTTPD.IHTTPSession handshakeRequest, EventBus eventBus) {
        super(handshakeRequest);
        this.eventBus = eventBus;
        this.eventBus.register(this);

        // prevent connection from being closed due to inactivity:
        // send ping messages every 2 seconds, and the client will respond with pong
        this.pingTimer = new TimerTask() {
            @Override
            public void run(){
                try {
                    ping(new byte[0]);
                } catch (IOException e) {
                    pingTimer.cancel();
                }
            }
        };
        new Timer().schedule(this.pingTimer, 1000, 2000);
    }

    public void onEvent(ReceiveReportEvent event) {
        try {
            this.send(event.data);
        } catch (IOException ex) {
            Log.w(this.getClass().getCanonicalName(), "Error sending data: " + ex);
        }
    }

    @Override
    protected void onPong(WebSocketFrame pongFrame) {

    }

    @Override
    protected void onMessage(WebSocketFrame messageFrame) {
        this.eventBus.post(new SendReportEvent(messageFrame.getBinaryPayload()));
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        this.pingTimer.cancel();
        this.eventBus.unregister(this);
    }

    @Override
    protected void onException(IOException e) {

    }
}
