package publicdomain.webhidpolyfill;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import publicdomain.webhidpolyfill.e.FailedToClaimInterfaceException;
import publicdomain.webhidpolyfill.e.FailedToOpenDeviceException;
import publicdomain.webhidpolyfill.e.HIDException;
import publicdomain.webhidpolyfill.e.InOutEndpointsNotFoundException;
import publicdomain.webhidpolyfill.e.NoHIDInterfaceFoundException;

public class HIDConnector {
    private final UsbManager mUsbManager;

    public HIDConnector(UsbManager mUsbManager) {
        this.mUsbManager = mUsbManager;
    }

    public HID connect(UsbDevice device) throws HIDException {
        UsbInterface iface = this.findHIDInterface(device);

        UsbDeviceConnection connection = this.mUsbManager.openDevice(device);
        if (connection == null) {
            throw new FailedToOpenDeviceException();
        }

        try {
            if (!connection.claimInterface(iface, true)) {
                throw new FailedToClaimInterfaceException();
            }

            HIDInOut inOut = this.findInOut(iface);

            return new HID(
                device,
                iface,
                connection,
                inOut
            );
        } catch (HIDException ex) {
            connection.releaseInterface(iface);
            connection.close();
            throw ex;
        }
    }

    private UsbInterface findHIDInterface(UsbDevice device) throws NoHIDInterfaceFoundException {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);
            if (iface.getInterfaceClass() == UsbConstants.USB_CLASS_HID) {
                return iface;
            }
        }
        throw new NoHIDInterfaceFoundException();
    }

    private HIDInOut findInOut(UsbInterface iface) throws InOutEndpointsNotFoundException {
        UsbEndpoint in = null;
        UsbEndpoint out = null;

        for (int i = 0; i < iface.getEndpointCount(); i++) {
            UsbEndpoint endpoint = iface.getEndpoint(i);

            if (endpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_INT) {
                continue;
            }

            int direction = endpoint.getDirection();
            if (direction == UsbConstants.USB_DIR_IN) {
                in = endpoint;
            } else if (direction == UsbConstants.USB_DIR_OUT) {
                out = endpoint;
            }

            if (in != null && out != null) {
                // we have found both endpoints, stop looking
                break;
            }
        }

        if (in == null || out == null) {
            throw new InOutEndpointsNotFoundException();
        }

        return new HIDInOut(in, out);
    }
}
