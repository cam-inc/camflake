package com.camobile.camflake;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.junit.Before;
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
 * {@link Camflake}に対する単体テスト
 */
public class CamflakeTest {

    /**
     * msecあたり最大で割当可能なシーケンス番号+1 (64)
     */
    private static final int SEQUENCE_MAX = 1 << 6;

    @Before
    public void setUp() {
    }

    /**
     * baseTimeを1970-01-01T00:00:00Z以前にすると初期化に失敗する
     */
    @Test(expected = CamflakeException.class)
    public void testConstructorFailsWhenBaseTimeIsBeforeEpoch() {

        Instant baseTime = ZonedDateTime
            .of(1969, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"))
            .toInstant();

        new Camflake(new DefaultMachineId(), baseTime);
    }

    /**
     * baseTimeを未来時刻にすると初期化に失敗する
     */
    @Test(expected = CamflakeException.class)
    public void testConstructorFailsWhenBaseTimeIsAfterNow() {
        Instant baseTime = ZonedDateTime
            .of(9999, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"))
            .toInstant();

        new Camflake(new DefaultMachineId(), baseTime);
    }

    /**
     * 基準時刻からの経過時間が上限を超えていると初期化に失敗する
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
     * 毎回異なる値を払い出し、かつ払い出す度に値は大きくなる
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
     * 実行時刻をモックして想定通りの値を返すことを確認する
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
                // 初期化時は正常, 実行時に上限超え
                returns(
                    baseTime, // 初期化時
                    execTime  // 実行時
                );
            }
        };

        Camflake camflake = new Camflake(new TestMachineId());

        long id = camflake.next();
        assertThat(id, is(4194304001L));

    }

    /**
     * 実行時に基準時刻からの経過時間が上限を超えると例外をスローする
     */
    @Test(expected = CamflakeException.class)
    public void testNextFailsWhenExceededTimeLimit() {
        final Instant exceededTime = Instant.ofEpochMilli(1L << 41);
        new Expectations(Instant.class) {
            {
                Instant.now();
                returns(
                    Instant.EPOCH, // 初期化時は正常
                    exceededTime   // 実行時に上限超え
                );
            }
        };

        Camflake camflake = new Camflake(new DefaultMachineId(), Instant.EPOCH);
        camflake.next();
    }

    /**
     * ある経過時間においてシーケンスIDが上限まで振られた場合は一度だけリトライする
     */
    @Test
    public void testNextWithRetryOnce() {

        final Instant execTime = Instant.ofEpochMilli(1L);
        final Instant execTime2 = Instant.ofEpochMilli(3L);
        new Expectations(Instant.class) {
            {
                Instant.now();
                returns(
                    execTime, // 初期化時
                    execTime, // 実行時1回目
                    execTime2 // 実行時2回目(上限超え）
                );
            }
        };

        Camflake camflake = new Camflake(new TestMachineId(), Instant.EPOCH);

        // 予めカウンタを上限に達した状態に変更してリトライさせる
        Deencapsulation.setField(camflake, "counter", new AtomicInteger(SEQUENCE_MAX));

        long id = camflake.next();
        assertThat(id, is(12582913L));
    }

    /**
     * シーケンスIDの再取得でも上限を超えた場合は例外をスローする
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
     * シーケンスIDの再取得の過程で{@link InterruptedException}が発生した場合は例外をスローする
     */
    @Test(expected = CamflakeException.class)
    public void testNextFailsWhenInterrupted(@Mocked Thread unused) throws InterruptedException {
        final Instant execTime = Instant.ofEpochMilli(1L);
        new Expectations(Instant.class) {
            {
                Instant.now();
                returns(
                    execTime, // 初期化時
                    execTime  // 実行時1回目
                );

                // sleep中に例外をスローさせる
                Thread.sleep(anyLong);
                result = new InterruptedException();
            }
        };

        Camflake camflake = new Camflake(new TestMachineId(), Instant.EPOCH);

        // 予めカウンタを上限に達した状態に変更してリトライさせる
        Deencapsulation.setField(camflake, "counter", new AtomicInteger(SEQUENCE_MAX));

        camflake.next();
    }

    /**
     * マシンIDとして1を返すテスト用のユーティリティクラス
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
