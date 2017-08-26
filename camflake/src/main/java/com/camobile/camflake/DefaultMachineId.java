package com.camobile.camflake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ローカルマシンのIPアドレスの下16bitをもとに生成されるマシンIDです。
 * TODO ロジックの見直しが必要（あくまで参照実装）
 */
class DefaultMachineId implements MachineId {

    /**
     * Logger
     */
    private static Logger log = LoggerFactory.getLogger(MachineId.class);

    @Override
    public int getId() {
        try {
            byte[] a = InetAddress.getLocalHost().getAddress();
            log.debug("Localhost IP address: {}", InetAddress.getLocalHost().getHostAddress());

            int machineId = (Byte.toUnsignedInt(a[2]))<<8 | Byte.toUnsignedInt(a[3]);
            log.debug("Calculated machineID: {}", machineId);
            return machineId;

        } catch (UnknownHostException e) {
            log.error("Failed to process machine id.", e);
            throw new CamflakeException(e);
        }
    }
}
