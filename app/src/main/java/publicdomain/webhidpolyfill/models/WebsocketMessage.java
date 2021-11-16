package publicdomain.webhidpolyfill.models;

public class WebsocketMessage {
    public static final String TYPE_OPEN = "open";
    public static final String TYPE_CLOSE = "close";
    public static final String TYPE_SEND = "send";
    public static final String TYPE_RECEIVE = "receive";

    public final String type;
    public final byte[] data;

    public WebsocketMessage(String type, byte[] data) {
        this.type = type;
        this.data = data;
    }
}
