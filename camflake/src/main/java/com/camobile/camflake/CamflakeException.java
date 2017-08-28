package com.camobile.camflake;

/**
 * {@link Camflake}の実行時例外。
 */
public final class CamflakeException extends RuntimeException {

    /**
     * {@link CamflakeException} インスタンスを初期化します。
     *
     * @param message 例外メッセージ
     */
    public CamflakeException(String message) {
        super(message);
    }

    /**
     * {@link CamflakeException} インスタンスを初期化します。
     *
     * @param message 例外メッセージ
     * @param cause 原因例外
     */
    public CamflakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
