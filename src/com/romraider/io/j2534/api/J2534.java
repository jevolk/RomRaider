package com.romraider.io.j2534.api;

public interface J2534 {
    boolean isSupported();

    int open();

    Version readVersion(int deviceId);

    int connect(int deviceId);

    void setConfig(int channelId, ConfigItem... items);

    ConfigItem[] getConfig(int channelId, int... parameters);

    int startPassMsgFilter(int channelId, byte mask, byte pattern);

    void writeMsg(int channelId, byte[] data);

    // FIX - Needs to return msg type, etc. Create Response object.
    byte[] readMsg(int channelId);

    void stopMsgFilter(int channelId, int msgId);

    void disconnect(int channelId);

    void close(int deviceId);
}