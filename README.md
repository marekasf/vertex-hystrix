# vertex-hystrix

Library to expose [hystrix.stream](https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-metrics-event-stream) to the [vert.x](http://vertx.io/) web application.

## Usage

### 1. Install in local maven repository:

	./gradlew install
	
### 2. Add project dependency	

	// gradle example
	compile 'org.marekasf:vertex-hystrix:0.0.1'
	
### 3. Add "com.netflix.hystrix:hystrix-core" to the system classpath.

Hystrix uses static field [HystrixCommandMetrics](https://github.com/Netflix/Hystrix/blob/master/hystrix-core/src/main/java/com/netflix/hystrix/HystrixCommandMetrics.java), but Vert.x isolates verticles by classloader.
To overcome this problem "com.netflix.hystrix:hystrix-core" library needs to be added to the system classpath.
Gradle example:
	
	buildscript {
		dependencies {
			classpath "io.vertx:vertx-core:$vertxVersion"
			classpath "io.vertx:vertx-platform:$vertxVersion"
			classpath "com.netflix.hystrix:hystrix-core:1.4.0-RC5"
		}
	}
	
### 4. Register route matcher in Vert.x verticle:
	
	vertx.createHttpServer()
	   .requestHandler(new RouteMatcher()
	   .get("/hystrix.stream", new HystrixMetricsStreamHandler()::hystrixStream));
	   
### 5. Create [hystrix command](https://github.com/Netflix/Hystrix/wiki/How-To-Use).

There is [HystrixVertxHttpClient](/src/main/java/org/marekasf/vertx_hystrix/HystrixVertxHttpClient.java) helper class for Vert.x HttpClient command.

### 6. Access hystrix.stream

For local execution use [http://localhost:8080/hystrix.stream](http://localhost:8080/hystrix.stream)