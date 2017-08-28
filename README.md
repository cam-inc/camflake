Camflake
===

Camflake is a distributed unique ID generator implemented with Java.
This library is inspired by [Snowflake](https://github.com/twitter/snowflake) and [Sonyflake](https://github.com/sony/sonyflake).

Camflake generates the unique ID as unsigned 63 bit long value.
It is composed of values as below.
```
* 41 bits : Elapsed time from base time（msec order）
*  6 bits : Sequence number
* 16 bits : Machine ID
```

Refer [Basis](#basis) for more details.
And Japanese documents are [here](README_ja.md).


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

The instance generates another unique ID each time `next` method is invoked.


### Run sample web apps

We have provided a sample web application which generates the unique ID by using Camflake.
To start the app, invoke the command below at the root directory of this project. 

```bash
./gradew bootRun
```

After started the app, you can get the unique ID by accessing to the URL on below.

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

You can modify the base time during initialization of Camflake instance.
Though, RuntimeException will be thrown if you pass a invalid parameter to the constructor.

* A base time which is before UNIX epoch (`1970-01-01T00:00:00Z`)
* A base time which is after current time.

The max time of the elapsed time is about 69 years (2,199,023,255,551 msec) from base time.
RuntimeException will thrown when the elapsed time exceeded the max time.


#### Sequence number
Sequence number is an identifier to determine the unique ID which was created on same time.
This number will be generated max to 64 in millisecond.
That is to say, Camflake instance can generate maximum 64 unique ID in millisecond.


#### Machine ID
Machine ID is an identifier of Camflake instance.
The latter 16 bits of the host address will be used to create machine ID as default.

You can develop your own machine ID generator to avoid generating duplicated ID in cases like below.

* If you have to run Camflake on multiple Java process on one host machine.
* If you have to run Camflake on multiple Docker container on one host machine.


## Advanced usages

You can modify Camflake by initializing with any proper base time and machine id.

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
