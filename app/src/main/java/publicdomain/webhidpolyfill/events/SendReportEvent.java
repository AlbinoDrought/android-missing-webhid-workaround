package publicdomain.webhidpolyfill.events;

public class SendReportEvent {
    public final byte[] data;

    public SendReportEvent(byte[] data) {
        this.data = data;
    }
}
