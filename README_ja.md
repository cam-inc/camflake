Camflake
===
[![CircleCI](https://circleci.com/gh/cam-inc/camflake.svg?style=svg)](https://circleci.com/gh/cam-inc/camflake)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b693d8a4cdcf46d28b5c310e37d58f70)](https://www.codacy.com/app/camobile-io/camflake?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cam-inc/camflake&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/b693d8a4cdcf46d28b5c310e37d58f70)](https://www.codacy.com/app/camobile-io/camflake?utm_source=github.com&utm_medium=referral&utm_content=cam-inc/camflake&utm_campaign=Badge_Coverage)

CamflakeはJavaで実装された分散型のユニークID生成器です。
本ライブラリは[Snowflake](https://github.com/twitter/snowflake)と[Sonyflake](https://github.com/sony/sonyflake)にインスパイアされて開発しました。

Camflakeは63bitの符号なし整数としてユニークIDを生成します。ユニークIDは以下の情報で構成されます。

```
* 41 bits : ある基準時刻からの経過時間（ミリ秒）
*  6 bits : シーケンス番号
* 16 bits : マシンID
```

詳細情報は [Basis](#basis)を参照してください。


## Requirements

* JDK 1.8.0 or over


## Usage
### Installation

```gradle
compile('com.camobile.camflake:camflake:1.0.0')
```

```xml
<dependency>
    <groupId>com.camobile.camflake</groupId>
    <artifactId>camflake</artifactId>
    <version>1.0.0</version>
</dependency>
```


### Basic usage

`Camflake`クラスのインスタンスを生成して`next`メソッドを実行します。

```java
Camflake camflake = new Camflake();
long id = camflake.next();
```

`next`メソッドを実行するごとに異なるユニークIDを生成します。


### Run sample web apps

Camflakeを利用してユニークIDを払い出すサンプルWebアプリケーションを本プロジェクトの`sample`ディレクトリ配下に作成しています。

プロジェクトのルートフォルダで以下のコマンドを実行してください。

```bash
./gradlew bootRun
```

アプリケーションが起動した状態で以下にアクセスすることでユニークIDを取得することができます。

```bash
curl -X GET http://localhost:8080/id/next
```


## Basis

Camflakeは63bitの符号なし整数としてユニークIDを生成します。ユニークIDは以下の情報で構成されます。

```
* 41 bits : ある基準時刻からの経過時間（ミリ秒）
*  6 bits : シーケンス番号（同一時刻内でユニークIDの作成が必要なときに与えるシーケンス番号）
* 16 bits : マシンID（Camflakeが稼働しているJavaインスタンスを一意に特定するID）
```

#### 経過時間
Camflakeは、ある基準時刻からの経過時間情報を元にユニークIDを生成します。デフォルトの基準時刻は`2017-06-01T00:00:00Z`です。

Camflakeのインスタンスの初期化時に任意の値を基準時刻として指定することができます。
ただし、以下の時刻を設定した場合は実行時例外をスローします。
* UNIXエポック時刻`1970-00-00T00:00:00Z`以前の時刻
* 現在時刻よりも未来の時刻

なお、Camflakeは、基準時刻から約69年（2,199,023,255,551ミリ秒）経過するまではユニークIDを生成することができます。
上記上限を超えた場合は実行時例外をスローします。


#### シーケンス番号
同一時刻にユニークIDの生成が必要な際に、ユニークIDの重複を避けるためにCamflake内部で与えられる番号です。ミリ秒あたり最大で64個のシーケンス番号が生成可能です。

すなわち、Camflakeのインスタンスが1ミリ秒あたりに発行可能なユニークIDは最大64個です。


#### マシンID

マシンIDとは、Camflakeを実行しているJavaインスタンスを一意に特定するためのIDです。デフォルトでは、ローカルホストマシンIPアドレスの下位16bitを元に生成します。

以下のような利用用途においては、ユニークIDの重複を避けるためにマシンIDの生成ロジックを個別に実装する必要があります。
* 1ホストマシンで複数のJavaプロセスにおいてユニークIDを個別に発行する場合
* 1ホストマシンで複数のDockerコンテナにおいてユニークIDを個別に発行する場合


## Advanced usage

Camflakeのインスタンス生成時に、任意の基準時刻とマシンIDを設定することでユニークID生成時の挙動を変更することが可能です。

#### 任意のMachineIDを設定する

マシンIDを個別に実装する必要がある場合は、`MachineId`インタフェースを実装したクラスを独自実装してCamflakeを初期化してください。

```java
MachineId machineId = new ConcreteMachineId(); // MachineIdインタフェースを実装したクラス
Camflake camflake = new Camflake(machineId);
```

#### 基準時刻を変更して初期化する

基準時刻をデフォルトから変更したい場合は、任意の`Instant`を生成してCamflakeを初期化してください。

```java
// 例では2017-08-01T00:00:00Z を基準時刻としたい場合
Instant baseTime = ZonedDateTime.of(2017, 8, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant());
Camflake camflake = new Camflake(new DefaultMachineId(), baseTime);
```


## License

本ライブラリはMITライセンスとして公開します。

ライセンスの詳細は[LICENSE](LICENSE.txt)を参照してください。
