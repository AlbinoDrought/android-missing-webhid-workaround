package publicdomain.webhidpolyfill;

import de.greenrobot.event.EventBus;
import fi.iki.elonen.IWebSocketFactory;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketResponseHandler;

public class WebServer extends NanoHTTPD {
    private final WebSocketResponseHandler handler;

    public WebServer(int port, EventBus eventBus) {
        super(port);
        this.handler = new WebSocketResponseHandler(
                new IWebSocketFactory() {
                    @Override
                    public WebSocket openWebSocket(IHTTPSession handshake) {
                        return new WebsocketServer(handshake, eventBus);
                    }
                }
        );
    }

    @Override
    public Response serve(IHTTPSession session) {
        return this.handler.serve(session);
    }
}
