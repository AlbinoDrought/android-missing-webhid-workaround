# Experiment: Work around lack of WebHID support on Android

Use USB devices from an Android browser

## Why

Your WebHID project works on desktop Chrome. After digging up your old USB OTG cable, you find out [Chrome for Android does not support WebHID](https://bugs.chromium.org/p/chromium/issues/detail?id=964441). This makes you sad. You are willing to employ a workaround so you can keep your browser-based code.

## What Probably Works

Swapping out some `device.sendReport` and `device.addEventListener('inputreport', ...)` calls with a websocket shim, I could make my project work on Android in the browser.

Here's what worked for me:

<details><summary>ReadWriteOpenCloser interface, WebHID + WebSockets implementations</summary>

```typescript
export interface ReadWriteOpenCloser {
  Open(): Promise<void>;
  Close(): Promise<void>;
  Read(timeout: number): Promise<DataView>;
  Write(data: Uint8Array): Promise<void>;
}

export class HIDReadWriteOpenCloser implements ReadWriteOpenCloser {
  private inputEventQueue: HIDInputReportEvent[] = [];

  private handleInputEventCallback = (event: HIDInputReportEvent) => this.handleInputEvent(event);

  private readCallback: (value: unknown) => void = () => {};

  constructor(private device: HIDDevice) {}

  private handleInputEvent(event: HIDInputReportEvent): void {
    this.inputEventQueue.push(event);
    if (this.readCallback) {
      this.readCallback(event);
    }
  }

  public async Open(): Promise<void> {
    try {
      this.Close();
    } catch (ex) {
      console.error(ex);
    }

    await this.device.open();
    this.device.addEventListener('inputreport', this.handleInputEventCallback);
  }

  public async Close(): Promise<void> {
    this.device.removeEventListener('inputreport', this.handleInputEventCallback);
    await this.device.close();
  }

  public async Read(timeout: number): Promise<DataView> {
    if (this.inputEventQueue.length === 0) {
      // wait for data
      let timeoutHandle;
      await new Promise((resolve, reject) => {
        this.readCallback = resolve;
        timeoutHandle = setTimeout(() => reject('Timeout error'), timeout);
      });
      clearTimeout(timeoutHandle);
    }

    if (this.inputEventQueue.length > 0) {
      return Promise.resolve(this.inputEventQueue.shift()!.data);
    }

    throw new Error('no data');
  }

  public async Write(data: Uint8Array): Promise<void> {
    await this.device.sendReport(0, data);
  }
}

export class WebSocketReadWriteOpenCloser implements ReadWriteOpenCloser {
  private inputEventQueue: MessageEvent<Blob>[] = [];

  private handleInputEventCallback = (event: MessageEvent<Blob>) => this.handleInputEvent(event);

  private readCallback: (value: unknown) => void = () => {};

  private ws: WebSocket|null = null;

  constructor(private url: string) {}

  private handleInputEvent(event: MessageEvent<Blob>): void {
    this.inputEventQueue.push(event);
    if (this.readCallback) {
      this.readCallback(event);
    }
  }

  public async Open(): Promise<void> {
    try {
      this.Close();
    } catch (ex) {
      console.error(ex);
    }

    this.ws = new WebSocket(this.url);
    this.ws.onmessage = this.handleInputEventCallback;
    await new Promise((resolve, reject) => {
      this.ws!.onopen = resolve;
      this.ws!.onclose = reject;
    });
  }

  public async Close(): Promise<void> {
    this.ws?.close();
    this.ws = null;
  }

  public async Read(timeout: number): Promise<DataView> {
    if (this.inputEventQueue.length === 0) {
      // wait for data
      let timeoutHandle;
      await new Promise((resolve, reject) => {
        this.readCallback = resolve;
        timeoutHandle = setTimeout(() => reject('Timeout error'), timeout);
      });
      clearTimeout(timeoutHandle);
    }

    if (this.inputEventQueue.length > 0) {
      return Promise.resolve(new DataView(await this.inputEventQueue.shift()!.data.arrayBuffer()));
    }

    throw new Error('no data');
  }

  public async Write(data: Uint8Array): Promise<void> {
    this.ws?.send(data);
  }
}

export const exampleConnectDesktopHID = (): ReadWriteOpenCloser => {
  // prefer previously-authed devices:
  let devices = await navigator.hid.getDevices();

  if (devices.length === 0) {
    // no previously-authed devices, look for new ones
    devices = await navigator.hid.requestDevice({
      filters: [
        { vendorId: /* your vendor ID */, productId: /* your product ID */ },
      ],
    });
  }

  const device = devices[0];
  if (!device) {
    // no devices found, abort
    throw new Error('no device');
  }

  return new HIDReadWriteOpenCloser(device);
};

export const exampleConnectLocalAndroidWebsockets = (): ReadWriteOpenCloser => {
  return new WebSocketReadWriteOpenCloser('ws://localhost:18080');
};

export const exampleConnectRemoteAndroidWebsockets = (): ReadWriteOpenCloser => {
  return new WebSocketReadWriteOpenCloser('ws://192.168.1.42:18080');
};

export const exampleDoSomethingWithDevice = async (device: ReadWriteOpenCloser) => {
  await device.Open();
  await device.Write(new Uint8Array(64));
  console.log(await device.Read(10000));
  await device.Close();
};

// do something with a true WebHID device
exampleDoSomethingWithDevice(exampleConnectDesktopHID());
// do something with the local shimmed websockets device
exampleDoSomethingWithDevice(exampleConnectLocalAndroidWebsockets());
// do something with a remote shimmed websockets device (worked for me but YMMV)
exampleDoSomethingWithDevice(exampleConnectRemoteAndroidWebsockets());
```

</details>

## What Probably Doesn't Work

This is an insecure proof-of-concept, not a production-ready companion app. If someone can talk to your phone on the network, they can probably talk to the exposed USB devices over websockets too.

In my WebHID project, I only called the `sendReport` method and listened to the `inputreport` event. Once I got those working with this shim app, I stopped. I also only send a message, wait for a reply, send the next message, etc. I'm not sure what other USB/HID usecases are like.

For some reason, this app keeps running in the background even though I didn't ask it to. That's great (it's what we want!) but some phones will probably kill it earlier.

## See Also

- The [452/USBHIDTerminal](https://github.com/452/USBHIDTerminal) project: I tried to use this first but didn't have any luck. It might work better for you.

## License

[CC0 1.0 Universal](./LICENSE)
