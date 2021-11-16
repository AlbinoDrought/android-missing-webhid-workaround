package publicdomain.webhidpolyfill.events;

public class ReceiveReportEvent {
    public byte[] data;

    public ReceiveReportEvent(byte[] data) {
        this.data = data;
    }
}
