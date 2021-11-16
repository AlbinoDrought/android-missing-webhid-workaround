package publicdomain.webhidpolyfill;

import android.util.Log;

import de.greenrobot.event.EventBus;
import publicdomain.webhidpolyfill.events.ReceiveReportEvent;
import publicdomain.webhidpolyfill.events.SendReportEvent;

public class DebugLogger {
    public DebugLogger(EventBus eventBus) {
        eventBus.register(this);
    }

    public void onEvent(ReceiveReportEvent event) {
        Log.i("DebugLogger", "<= " + bytesToHex(event.data));
    }

    public void onEvent(SendReportEvent event) {
        Log.i("DebugLogger", "=> " + bytesToHex(event.data));
    }

    private static String bytesToHex(byte[] buffer) {
        StringBuilder data = new StringBuilder();
        for (byte b : buffer) {
            data.append(String.format("%02X", b));
        }
        return data.toString();
    }
}
