package com.example.documentation_20190713.Bluetooth;

//needs to be public for access outside of Bluetooth package
public interface SerialListener {
    void onSerialConnect();
    void onSerialConnectError(Exception e);
    void onSerialRead(byte[] data);
    void onSerialIoError(Exception e);
}
