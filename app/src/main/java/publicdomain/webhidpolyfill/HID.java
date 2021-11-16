package publicdomain.webhidpolyfill;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;

import java.util.concurrent.atomic.AtomicBoolean;

public class HID implements AutoCloseable {
    public final UsbDevice device;
    public final UsbInterface iface;
    public final UsbDeviceConnection connection;
    public final HIDInOut inOut;
    public final AtomicBoolean closed = new AtomicBoolean(false);

    public HID(UsbDevice device, UsbInterface iface, UsbDeviceConnection connection, HIDInOut inOut) {
        this.device = device;
        this.iface = iface;
        this.connection = connection;
        this.inOut = inOut;
    }

    public void close() {
        this.closed.set(true);
        this.connection.releaseInterface(this.iface);
        this.connection.close();
    }

    public int read(byte[] buffer, int timeout) {
        return this.connection.bulkTransfer(
            this.inOut.in,
            buffer,
            buffer.length,
            timeout
        );
    }

    public int write(byte[] buffer, int timeout) {
        return this.connection.bulkTransfer(
            this.inOut.out,
            buffer,
            buffer.length,
            timeout
        );
    }
}
