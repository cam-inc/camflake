package com.camobile.camflake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GUIDの払い出しを行います。
 */
public final class Camflake {

    /**
     * Logger
     */
    private static Logger log = LoggerFactory.getLogger(Camflake.class);

    /**
     * 時間上限:基準時刻より2199023255551msec秒経過
     */
    private static final long TIME_MAX = (1L << 41) - 1L; // 時間の上限msec

    /**
     * msecあたり最大で割当可能なシーケンス番号(0-63)
     */
    private static final long SEQUENCE_MAX = (1L << 6) - 1L;

    /**
     * ロックオブジェクト
     */
    private static final Object LOCK = new Object();

    /**
     * GUIDの取得開始に伴う開始基準時刻(msec)
     */
    private long baseTime;

    /**
     * このプログラムを実行しているマシンを一意に特定するID
     * 16bitで表現可能な符号なし整数値(0-262143)を期待します。
     */
    private int machineId;

    /**
     * シーケンス番号の割当を行う際の基準時刻からの経過時間(msec)
     * この値は随時更新されます。
     */
    private volatile long elapsedTime;

    /**
     * シーケンス番号の割当を行うカウンタ
     * elapsedTimeの更新のたびにゼロリセットされる
     */
    private AtomicInteger counter = new AtomicInteger(0);

    /**
     * {@link Camflake} インスタンスを初期化します。
     *
     * @param machineId マシンID
     * @param baseTime  基準時刻(UTC)。この時刻からの経過時間をもとにGUIDの払い出しを行います。
     *                  この時刻は1970-01-01T00:00:00Zよりも後、かつ実行時の時刻よりも過去に設定してください。
     * @throws RuntimeException     マシンIDの取得に失敗した場合, 基準時刻が不正な値の場合, 時間上限に達した場合
     * @throws NullPointerException machineIdまたはbaseTimeがnullの場合
     */
    public Camflake(MachineId machineId, Instant baseTime) {

        Instant now = Instant.now();
        if (baseTime.isBefore(Instant.EPOCH) || baseTime.isAfter(now)) {
            throw new CamflakeException("Base time should be after 1970-01-01T00:00:00Z, or before now.");
        }
        long elapsed = now.toEpochMilli() - baseTime.toEpochMilli();
        if (elapsed > TIME_MAX) {
            throw new CamflakeException("Exceeded the time limit.");
        }

        this.machineId = machineId.getId();
        this.baseTime = baseTime.toEpochMilli();
        this.elapsedTime = elapsed;
    }

    /**
     * {@link Camflake} インスタンスを初期化します。
     * 初期化時の基準時刻として2017-06-01T00:00:00Zを使用します。
     *
     * @param machineId マシンID
     * @throws CamflakeException    マシンIDの取得に失敗した場合
     * @throws NullPointerException machineIdがnullの場合
     */
    public Camflake(MachineId machineId) {
        this(machineId,
            ZonedDateTime
                .of(2017, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant());
    }

    /**
     * {@link Camflake} インスタンスを初期化します。
     * マシンIDは実行マシンのローカルIPアドレスの下16bitをもとに生成されます。
     * 初期化時の基準時刻として2017-06-01T00:00:00Zを使用します。
     *
     * @throws CamflakeException マシンIDの取得に失敗した場合
     */
    public Camflake() {
        this(new DefaultMachineId());
    }

    /**
     * GUIDを払い出します。
     *
     * @return GUID
     * @throws CamflakeException 時間上限を超えた場合, シーケンスの払い出しに失敗した場合
     */
    public long next() {

        // elapsedTime
        long elapsed = getElapsedTime();
        // sequence
        int sequence = getSequence(elapsed);
        log.debug("sequence id: {}", sequence);
        // 1msecオーダーで63個もシーケンスを払い出すことは想定しづらいが一度だけ救済する
        if (sequence > SEQUENCE_MAX) {
            sleep(TimeUnit.MILLISECONDS.toMillis(2L));

            elapsed = getElapsedTime();
            sequence = getSequence(elapsed);
            if (sequence > SEQUENCE_MAX) {
                throw new CamflakeException("Failed to issue sequence id.");
            }
        }

        long id = (elapsed << 22) | (sequence << 16) | machineId;
        log.debug("guid: {}", id);

        return id;
    }

    /**
     * 基準時刻からの経過時間をミリ秒オーダーで取得します。
     *
     * @return 基準時刻からの経過時間（msec）
     * @throws CamflakeException 経過時間が時間上限を超えた場合
     */
    private long getElapsedTime() {

        long now = Instant.now().toEpochMilli();
        log.debug("actual time: {}", now);
        log.debug("elapsedTime: {}", elapsedTime);

        long elapsed = now - baseTime;
        log.debug("elapsed: {}", elapsed);
        if (elapsed > TIME_MAX) {
            throw new CamflakeException("Exceeded the time limit.");
        }
        return elapsed;
    }

    /**
     * ある経過時間におけるシーケンスIDを取得します。
     * 払い出されるシーケンスIDの最大値としては63を想定しますが、
     * それを超えて払い出された場合もこのメソッド内では検知しません。
     * 必ず呼び出し元でシーケンスIDの正当性を検証してください。
     *
     * @param elapsed 基準時刻からの経過時間(ms)
     * @return シーケンスID(0-63を想定するがそれを超えて払い出す可能性がある)
     */
    private int getSequence(long elapsed) {

        synchronized (LOCK) {
            if (this.elapsedTime < elapsed) {
                this.elapsedTime = elapsed;
                counter.set(0); // reset
            }
            return counter.getAndIncrement();
        }
    }

    /**
     * 指定時間処理を停止します。
     *
     * @param durationMillis 停止時間
     * @throws CamflakeException スレッドの停止をインタラプトされたとき
     */
    private void sleep(long durationMillis) {

        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CamflakeException("Interruption occurred during thread sleep.", e);
        }
    }
}
