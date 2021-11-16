package publicdomain.webhidpolyfill;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import publicdomain.webhidpolyfill.e.HIDException;
import publicdomain.webhidpolyfill.events.SendReportEvent;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "WebHIDPolyfillActivity";

    private HIDConnector connector;
    private HID connection;
    private EventBus eventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        this.connector = new HIDConnector(usbManager);

        this.eventBus = EventBus.getDefault();
        this.eventBus.register(this);
        new DebugLogger(this.eventBus);

        // reattach previously-authed devices
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (usbManager.hasPermission(device)) {
                deviceAttached(device);
            } else {
                // idk what this means:
                PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED), 0);
                usbManager.requestPermission(device, mPendingIntent);
            }
        }

        // boot webserver
        WebServer webServer = new WebServer(18080, this.eventBus);
        try {
            webServer.start();
        } catch (IOException ex) {
            Log.e(TAG, "Failed starting WebServer: " + ex);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();

        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            this.deviceAttached(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            this.deviceDetached(device);
        }
    }

    @Override
    protected void onDestroy() {
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
        super.onDestroy();
    }

    private void deviceAttached(UsbDevice device) {
        Log.i(TAG, "device attached: " + device.getDeviceName());
        if (this.connection != null) {
            Log.i(TAG, "closing other connection: " + device.getDeviceName());
            this.connection.close();
            this.connection = null;
        }

        try {
            this.connection = this.connector.connect(device);
            Log.i(TAG, "successfully connected to device: " + this.connection.device.getDeviceName());
        } catch (HIDException ex) {
            Log.e(TAG, "failed to connect to device: " + ex.toString());
            return;
        }

        new HIDReader(this.connection, this.eventBus);
    }

    private void deviceDetached(UsbDevice device) {
        Log.i(TAG, "device detached: " + device.getDeviceName());
        if (this.connection != null && this.connection.device.equals(device)) {
            // not sure if connection needs to be closed if it's already detached
            Log.i(TAG, "closing previous connection: " + device.getDeviceName());
            this.connection.close();
            this.connection = null;
        }
    }

    public void onEvent(SendReportEvent event) {
        if (this.connection == null) {
            Log.e(TAG, "handling SendReportEvent while connection is null");
            return;
        }
        // todo: timeout, probably
        int result = this.connection.write(event.data, 0);
        if (result != event.data.length) {
            Log.e(TAG, "bad SendReportEvent write result: " + result);
            return;
        }
        Log.i(TAG, "good SendReportEvent write result: " + result);
    }
}