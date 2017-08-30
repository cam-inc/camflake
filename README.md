Camflake
===

Camflake is a distributed unique ID generator implemented in Java.
This library is inspired by [Twitter's Snowflake](https://github.com/twitter/snowflake) and [Sony's Sonyflake](https://github.com/sony/sonyflake).

Camflake generates an unique ID as unsigned 63 bit long value, which is composed of these below values.
```
* 41 bits : Elapsed time from base time（msec order）
*  6 bits : Sequence number
* 16 bits : Machine ID
```

Refer [Basis](#basis) path for more details.
And you can find the Japanese documents [here](README_ja.md).


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

Initiate `Camflake` instance and invoke `next` method.

```java
Camflake camflake = new Camflake();
long id = camflake.next();
```

Every time `next` method is invoked, the instance generates a different unique ID.


### Run sample web apps

We have provided a sample web application which generates the unique ID by using Camflake.
To start app, invoke below command at the root directory of this project.

```bash
./gradlew bootRun
```

After app started, you can get the unique ID by accessing to the URL below.

```bash
curl http://localhost:8080/id/next
```


## Basis

Camflake generates the unique ID as unsigned 63 bit long value. The unique ID is composed of

```
* 41 bits : Elapsed time from base time（msec order）
*  6 bits : Sequence number
* 16 bits : Machine ID
```

#### Elapsed time
To generate unique ID, Camflake uses elapsed time from base time.
The default base time is `2017-06-01T00:00:00Z`.

You can modify base time during Camflake instance's initialization.
If you pass an invalid parameter to constructor, Runtime Exception will be thrown.

* A base time, which is before UNIX epoch (`1970-01-01T00:00:00Z`)
* A base time, which is after current time.

The elapsed time maximum is about 69 years (2,199,023,255,551 msec) from the base time.
And Runtime Exception will be thrown when the elapsed time exceeded the maximum.


#### Sequence number
Sequence number is the unique ID's identifier, which determines among the unique IDs created at the same time.
The sequence number could be generated maximum to 64 numbers in millisecond.
Thus, Camflake instance could generate maximum 64 different unique ID in millisecond.


#### Machine ID
Machine ID is the Camflake instance's identifier, which is created by default using the latter 16-bits of the host's IP address.

You can develop your own machine ID generator to avoid generating duplicated ID in the below cases.

1. You have to run Camflake on multiple Java process on one host machine.
2. You have to run Camflake on multiple Docker container on one host machine.


## Advanced usage

You can modify Camflake by initializing with any proper base time or machine ID.

#### Modify machine id

Develop a concrete Java class which implements `MachineId` interface when you need to generate your own machine Id.

```java
MachineId machineId = new ConcreteMachineId(); // Concrete java class which implements MachineId interface.
Camflake camflake = new Camflake(machineId);
```

#### Modify base time

Construct Camflake with any proper `Instant` instance to modify base time.

```java
// This example modifies base time to 2017-08-01T00:00:00Z
Instant baseTime = ZonedDateTime.of(2017, 8, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant());
Camflake camflake = new Camflake(new DefaultMachineId(), baseTime);
```

## Licence

This library is MIT license.

See [LICENSE](LICENSE.txt) for details.
