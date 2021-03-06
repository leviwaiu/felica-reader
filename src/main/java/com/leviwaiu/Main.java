package com.leviwaiu;

import org.apache.commons.codec.binary.Hex;

import javax.usb.UsbException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        ByteBuffer buf;
        byte[] data = new byte[255];
        int received;

        FelicaReader reader = new FelicaReader();
        System.out.println(reader.getManufacturer() + " " + reader.getProduct());

        try{
            reader.open();
            reader.sendCommand(Chipset.CMD_SET_COMMAND_TYPE, new byte[]{0x01});
            buf = reader.sendCommand(Chipset.CMD_GET_FIRMWARE_VERSION);
            System.out.println("Firmware version: " + String.format("%d.%02d", buf.get(1), buf.get(0)));

            buf = reader.sendCommand(Chipset.CMD_GET_PD_DATA_VERSION);
            System.out.println("PD Data version: " + String.format("%d.%02d", buf.get(1), buf.get(0)));

            reader.sendCommand(Chipset.CMD_SWITCH_RF, new byte[]{0x00});

            reader.sendCommand(Chipset.CMD_IN_SET_RF, new byte[]{0x01, 0x01, 0x0f, 0x01});
            reader.sendCommand(Chipset.CMD_IN_SET_PROTOCOL, new byte[]{0x00, 0x18, 0x01, 0x01, 0x02, 0x01, 0x03, 0x00, 0x04, 0x00, 0x05, 0x00, 0x06, 0x00, 0x07, 0x08, 0x08, 0x00, 0x09, 0x00, 0x0a, 0x00, 0x0b, 0x00, 0x0c, 0x00, 0x0e, 0x04, 0x0f, 0x00, 0x10, 0x00, 0x11, 0x00, 0x12, 0x00, 0x13, 0x06});
            reader.sendCommand(Chipset.CMD_IN_SET_PROTOCOL, new byte[]{0x00, 0x18});

            System.out.println("----------Start--------------");

            boolean isLoop = true;
            while (isLoop) {
                buf = reader.sendCommand(Chipset.CMD_IN_COMM_RF, new byte[]{0x6e, 0x00, 0x06, 0x00, (byte) 0xff, (byte) 0xff, 0x01, 0x00});
                if (Arrays.equals(buf.array(), new byte[]{(byte) 0x80, 0x00, 0x00, 0x00})) {
                    System.out.println("Type A or B");
                } else {
                    //Type-F
                    if (buf.get(5) == 0x14 && buf.get(6) == 0x01) {
                        System.out.println("IDm: " + Hex.encodeHexString(Arrays.copyOfRange(buf.array(), 7, 15)));
                        System.out.println("PMm: " + Hex.encodeHexString(Arrays.copyOfRange(buf.array(), 15, 23)));
                        isLoop = false;
                    }
                }

                Thread.sleep(250);
            }

        }
        catch (UsbException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
