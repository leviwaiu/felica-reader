package com.leviwaiu;

import org.apache.commons.lang3.ArrayUtils;
import org.usb4java.*;

import javax.usb.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class FelicaReader {

    private static final int VENDOR_ID = 0x054C;
    private static final int PRODUCT_ID = 0x06C3;
    private UsbDevice device;
    private UsbPipe inPipe;
    private UsbPipe outPipe;
    private String manufacturer;
    private String product;
    private UsbInterface iface;

    public FelicaReader() {
        try {

            Context context = new Context();
            int result = LibUsb.init(context);
            if(result != LibUsb.SUCCESS){
                throw new LibUsbException("Unable to initialize libusb.", result);
            }

            UsbServices services = UsbHostManager.getUsbServices();

            DeviceHandle devicehandle = LibUsb.openDeviceWithVidPid(context, (short) VENDOR_ID, (short) PRODUCT_ID);
            LibUsb.setAutoDetachKernelDriver(devicehandle, true);
            LibUsb.setConfiguration(devicehandle, 1);

            UsbHub rootHub = services.getRootUsbHub();
            device = this.findDevice(rootHub, VENDOR_ID, PRODUCT_ID);

            this.manufacturer = device.getManufacturerString();
            this.product = device.getProductString();

            UsbConfiguration config = (UsbConfiguration) device.getUsbConfigurations().get(0);
            this.iface = (UsbInterface) config.getUsbInterfaces().get(0);

            UsbEndpoint endpointIn = null;
            UsbEndpoint endpointOut = null;

            for (int i = 0; i < iface.getUsbEndpoints().size(); i++) {
                byte endPointAddr = ((UsbEndpoint) (iface.getUsbEndpoints().get(i))).getUsbEndpointDescriptor()
                        .bEndpointAddress();
                if ((((endPointAddr & 0x80) == 0x80))) {
                    endpointIn = (UsbEndpoint) (iface.getUsbEndpoints().get(i));
                } else if ((endPointAddr & 0x80) == 0x00) {
                    endpointOut = (UsbEndpoint) (iface.getUsbEndpoints().get(i));
                }
            }

            this.outPipe = endpointOut.getUsbPipe();
            this.inPipe = endpointIn.getUsbPipe();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void open() throws UsbException{
        //this.iface.claim();
        //Try force from kernel?

        this.iface.claim();
        this.inPipe.open();
        this.outPipe.open();
        this.outPipe.syncSubmit(Frame.ACK);
    }

    public void close() throws UsbException{
        this.inPipe.close();
        this.outPipe.close();
        this.iface.release();
    }

    public ByteBuffer sendCommand(byte commandCode){
        return this.sendCommand(this.inPipe, this.outPipe, commandCode, new byte[]{});
    }
    public ByteBuffer sendCommand(byte commandCode, byte[] commandData) {
        return this.sendCommand(this.inPipe, this.outPipe, commandCode, commandData);
    }
    public ByteBuffer sendCommand(UsbPipe inPipe, UsbPipe outPipe, byte commandCode) {
        return this.sendCommand(inPipe, outPipe, commandCode, new byte[]{});
    }

    public ByteBuffer sendCommand(UsbPipe inPipe, UsbPipe outPipe, byte commandCode, byte[] commandData){
        ByteBuffer ret;
        Frame frame;
        byte[] data = new byte[255];
        try {
            frame = new Frame(ArrayUtils.addAll(new byte[]{(byte) 0xD6, commandCode}, commandData));
            outPipe.syncSubmit(frame.frame);
            inPipe.syncSubmit(data);
            frame = new Frame(data);

            if(frame.type == Frame.TYPE_ACK){
                data = new byte[255];

                int received = inPipe.syncSubmit(data);
                frame = new Frame(data);
                if (frame.data[0] == (byte) 0xD7 && frame.data[1] == (byte) commandCode + 1){
                    return ByteBuffer.wrap(Arrays.copyOfRange(frame.data, 2, frame.data.length));
                }
            }
            else {
                return null;
            }
        }
        catch (UsbException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public UsbDevice findDevice(UsbHub hub, int vendorId, int productId) {
        for(UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()){
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();

            if(desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            if(device.isUsbHub()) {
                device = findDevice((UsbHub) device, vendorId, productId);
                if(device != null) return device;
            }
        }
        return null;
    }

    public void setInPipe(UsbPipe inPipe) {
        this.inPipe = inPipe;
    }

    public void setOutPipe(UsbPipe outPipe) {
        this.outPipe = outPipe;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getProduct() {
        return product;
    }
}
