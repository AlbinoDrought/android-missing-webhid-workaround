package publicdomain.webhidpolyfill;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import publicdomain.webhidpolyfill.events.ReceiveReportEvent;

public class HIDReader implements Runnable {
    private final HID hid;
    private final EventBus eventBus;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public HIDReader(HID hid, EventBus eventBus) {
        this.hid = hid;
        this.eventBus = eventBus;
        new Thread(this).start();
    }

    @Override
    public void run() {
        int result;
        byte[] buffer = new byte[this.hid.inOut.inPacketSize];
        while (this.running.get() && !this.hid.closed.get()) {
            result = this.hid.read(buffer,  1000);
            if (result < 0) {
                continue;
            }

            this.eventBus.post(new ReceiveReportEvent(buffer.clone()));
        }
    }

    public void stop() {
        this.running.set(false);
    }
}
