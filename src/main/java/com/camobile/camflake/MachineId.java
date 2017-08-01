package com.camobile.camflake;


/**
 * {@link Camflake}インスタンスが稼働するマシンを一意に特定するIDを管理します。
 */
@SuppressWarnings("squid:S1609") // Not a functional interface
public interface MachineId {

    /**
     * マシンIDを取得します。
     * 16bitで表現可能な符号なし整数値(0-262143)、かつ{@link Camflake}のインスタンス単位で一意の値になることを期待します。
     * 実装内部で意図したとおりにマシンIDを取得出来なかった場合は{@link CamflakeException}をスローします。
     *
     * @return マシンID
     * @throws CamflakeException 実装内部で何らかの原因でマシンIDを取得できなかった場合
     */
    int getId();
}
