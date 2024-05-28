> [!CAUTION]
> Use at your own risk: This extension is intended for educational purposes and has undergone limited testing.

This ThingWorx Extension allows to browse through the platform’s JVM JMX Beans and expose them as properties.

## Features

1. Privides a Mashup to browse the JMX MBeans and attributes from ThingWorx  (similar to jconsole)
2. Ability to _bind_ JMX attributes to modeled properties on generic Things

## Installation

1. Import the JMXMonitor Extension
2. (Optional) Unzip and Import the demo entities to get sample MBeans containers (OS, c3p0, JVM) and Mashup with metrics

## Usage

- Use the `JMX.MainMashup` to browse the JMX attributes and create MBean container Things

![Slide1](https://github.com/dattodroid/thingworx-executor-extension/assets/159778604/469ba7a6-404b-4c89-ab78-cf62fa8b29d3)

- Tips
  - Press [Enter] in the MBean Filter field to start searching
  - Use the [View] links on the Container Thing to directly access its property page in Composer

![Slide2](https://github.com/dattodroid/thingworx-executor-extension/assets/159778604/bb2e01b0-2c28-4ff1-9d92-9573f11e54d9)

- The attributes are exposed as normal properties on the container Things
  - Property values are automatically pulled from the JVM when accessed (driven by `aspect.cacheTime`)
  - Use the `RefreshMBeanAttributes` service to read values in batch (other services such as `GetPropertyValues` are also working, but the refresh service is more efficient) - you can call this service at regular internal from a timer to log the property values.

## (Optional) JMXDemo_Entities.xml

- Contains few sample container Things:
  - `jmx.OS` Thing - OperationSystem metrics: free memory and CPU usages...
  - `jmx.JVM` Thing - JVM metrics: Heap Memory usage and number of Threads ...
  - `jmx.C3P0.PP1` Thing - Persistence Provider Connection Pool metrics: active connections, treads ...
- Enable the `jmx.RefreshTimer` timer to start logging historical data
- Use the mashup `jmx.DemoMetrics` to graph those metrics

![Slide3](https://github.com/dattodroid/thingworx-executor-extension/assets/159778604/3df3d9f8-2224-426b-a4d3-ba7139f9e825)
