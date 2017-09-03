package com.camobile.camflake;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Camflake}.
 */
public class CamflakeTest {

    /**
     * Maximum sequence ID + 1 (64)
     */
    private static final int SEQUENCE_MAX = 1 << 6;

    /**
     * Fails to construct Camflake instance as base time is before UNIX epoch (1970-01-01T00:00:00Z).
     */
    @Test(expected = CamflakeException.class)
    public void testConstructorFailsWhenBaseTimeIsBeforeEpoch() {

        Instant baseTime = ZonedDateTime
            .of(1969, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"))
            .toInstant();

        new Camflake(new DefaultMachineId(), baseTime);
    }

    /**
     * Fails to construct Camflake instance as base time is after current time.
     */
    @Test(expected = CamflakeException.class)
    public void testConstructorFailsWhenBaseTimeIsAfterNow() {
        Instant baseTime = ZonedDateTime
            .of(9999, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"))
            .toInstant();

        new Camflake(new DefaultMachineId(), baseTime);
    }

    /**
     * Fails to construct Camflake instance as elapsed time from base time exceeded maximum.
     */
    @Test(expected = CamflakeException.class)
    public void testConstructorFailsWhenExceededTimeLimit() {

        final Instant exceededTime = Instant.ofEpochMilli(1L << 41);
        new Expectations(Instant.class) {
            {
                Instant.now();
                result = exceededTime;
            }
        };

        new Camflake(new DefaultMachineId(), Instant.EPOCH);
    }

    /**
     * Generates different unique ID each time next method is invoked, and the latter is larger.
     */
    @Test
    public void testNext() {
        Camflake camflake = new Camflake();
        long id1 = camflake.next();
        long id2 = camflake.next();
        long id3 = camflake.next();

        assertTrue(id2 > id1);
        assertTrue(id3 > id2);
    }

    /**
     * Generates the expected unique ID.
     */
    @Test
    public void testNextReturnsExpectedValue() {
        final Instant baseTime = ZonedDateTime
            .of(2017, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            .toInstant();
        final Instant execTime = ZonedDateTime
            .of(2017, 6, 1, 0, 0, 1, 0, ZoneId.of("UTC"))
            .toInstant();
        new Expectations(Instant.class) {
            {
                Instant.now();
                returns(
                    baseTime, // at initialize
                    execTime  // at runtime
                );
            }
        };

        Camflake camflake = new Camflake(new TestMachineId());

        long id = camflake.next();
        assertThat(id, is(4194304001L));

    }

    /**
     * Throws CamflakeException if elapsed time from base time exceeded maximum.
     */
    @Test(expected = CamflakeException.class)
    public void testNextFailsWhenExceededTimeLimit() {
        final Instant exceededTime = Instant.ofEpochMilli(1L << 41);
        new Expectations(Instant.class) {
            {
                Instant.now();
                returns(
                    Instant.EPOCH, // at initialize
                    exceededTime   // when invoke next method
                );
            }
        };

        Camflake camflake = new Camflake(new DefaultMachineId(), Instant.EPOCH);
        camflake.next();
    }

    /**
     * Retries once if sequence ID exceeded maximum (63).
     */
    @Test
    public void testNextWithRetryOnce() {

        final Instant execTime = Instant.ofEpochMilli(1L);
        final Instant execTime2 = Instant.ofEpochMilli(3L);
        new Expectations(Instant.class) {
            {
                Instant.now();
                returns(
                    execTime, // at initialize
                    execTime, // when invoke next method first time.
                    execTime2 // when invoke next method second time.
                );
            }
        };

        Camflake camflake = new Camflake(new TestMachineId(), Instant.EPOCH);

        // force update sequence ID to maximum to test retry.
        Deencapsulation.setField(camflake, "counter", new AtomicInteger(SEQUENCE_MAX));

        long id = camflake.next();
        assertThat(id, is(12582913L));
    }

    /**
     * Retried once but sequence ID exceeded maximum again throws CamflakeException as a result.
     */
    @Test(expected = CamflakeException.class)
    public void testNextWithRetryFailsWhenSequenceIsExceeded() {
        new MockUp<Camflake>() {
            @Mock
            private int getSequence(long elapsed) {
                return SEQUENCE_MAX;
            }
        };

        Camflake camflake = new Camflake();
        camflake.next();
    }

    /**
     * Throws CamflakeException if interruption occurred during thread sleeping.
     */
    @Test(expected = CamflakeException.class)
    public void testNextFailsWhenInterrupted(@Mocked Thread unused) throws InterruptedException {
        final Instant execTime = Instant.ofEpochMilli(1L);
        new Expectations(Instant.class) {
            {
                Instant.now();
                returns(
                    execTime, // at initialize
                    execTime  // when invoke next method first time.
                );

                // Throws InterruptedException during thread sleeps.
                Thread.sleep(anyLong);
                result = new InterruptedException();
            }
        };

        Camflake camflake = new Camflake(new TestMachineId(), Instant.EPOCH);

        // force update sequence ID to maximum to test retry.
        Deencapsulation.setField(camflake, "counter", new AtomicInteger(SEQUENCE_MAX));

        camflake.next();
    }

    /**
     * This is a test utility class of MachineId, which always returns 1 as a machine ID.
     */
    private class TestMachineId implements MachineId {

        private Logger log = LoggerFactory.getLogger(TestMachineId.class);

        @Override
        public int getId() {
            log.debug("Calculated machineID: 1");
            return 1;
        }
    }
}
