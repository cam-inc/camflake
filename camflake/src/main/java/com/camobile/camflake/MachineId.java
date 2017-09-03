package com.camobile.camflake;

/**
 * Manages machine ID, which identifies {@link Camflake} instance.
 */
@SuppressWarnings("squid:S1609") // Not a functional interface
public interface MachineId {

    /**
     * Returns machine ID.
     *
     * Machine ID is expected as a 16-bits unsigned integer value (0-262143), and identifies {@link Camflake} instance.
     * This method throws {@link CamflakeException} if the implementation of this method failed to get or generate machine ID.
     *
     * @return Machine ID.
     * @throws CamflakeException If failed to get or generate machine ID.
     */
    int getId();
}
