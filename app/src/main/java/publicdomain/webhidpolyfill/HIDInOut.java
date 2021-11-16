package publicdomain.webhidpolyfill;

import android.hardware.usb.UsbEndpoint;

public class HIDInOut {
    public final UsbEndpoint in;
    public final int inPacketSize;
    public final UsbEndpoint out;
    public final int outPacketSize;

    public HIDInOut(UsbEndpoint in, UsbEndpoint out) {
        this.in = in;
        this.inPacketSize = in.getMaxPacketSize();
        this.out = out;
        this.outPacketSize = out.getMaxPacketSize();
    }
}
