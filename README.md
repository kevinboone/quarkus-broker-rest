# quarkus-camel-amqp-jdbc

Version 1.0.0
Kevin Boone, September 2022

## What is this?

`quarkus-broker-rest` is a simple application for the Apache Camel
extension for Quarkus, that provides a REST interface to a message
broker. It is intended to demonstrate how such an interface might
be provided, and is not an exhaustive implementation. By default,
it includes no TLS security or authentication, although the 
security techniques I demonstrate in `camel-suntimes-auth-trivial`
apply equally well here.

The application runs as an HTTP server, and can send messages to,
and receive messages from, named queues on a message broker, whose
hostname and port are specified in a configuration file. It
uses the AMQP 1.0 protocol, because it is widely supported. 

All the configuration is in a single file: 
`src/main/resources/application.properties`. Some settings in this
file can be overridden at runtime using environment variables. 
The configuration file, in particular, defines the hostname and
port of the broker with which the application will communicate.

The application using the `camel-amqp` component to do the low-level
JMS operations against the broker. 

If GraalVM is installed, this application can be compiled ahead-of-time
to native code, which makes start-up essentially instantaneous.

## Prerequisites

- Java JDK 11 or later
- Maven version 3.2.8 or later
- A message broker, e.g., Apache Artemis, Apache ActiveMQ.
- Some way to put text messages onto a specific queue on the message
  broker. However, `quarkus-broker-rest` can both send and receive,
  so it can be used to test itself
- A way to issue HTTP requests. A web browser can be used to consume
  messages but, to produce messages to the broker, you'll need 
  something that can make a POST request. I use cURL in the examples
  below.

## Limitations

- Only text messages are supported
- Only queues (not topics) are supported
- Only synchronous (blocking) operations are supported
- There is no security (but it can easily be added without code changes)

## The REST API

To receive a message from the broker, issue a GET request on the URL

    /broker/consume/queue_name

where `queue_name` is the name of the queue from which to consume. 

The default HTTP port is 8080. The request will block until a message
is available.

For example:

    $ curl hostname:8080/broker/consume/foo

To produce a message to the broker, use the URL

    /broker/produce/queue_name

Send the message body as the HTTP request body, with no additional formatting.
For example:

    curl -X POST --data-binary "This is a test" \
      localhost:8080/broker/produce/foo -H "content-type: text/plain"

Note that you need to set the content type explicitly, else cURL
will by default send the data as if it were an HTML form submission; 
extra logic would be needed in the application to cope with this.
    
If the message is sent successfully, the client will receive a
200 (OK) response with the text "OK". Otherwise it will receive a
500 (internal error) response with a brief explanation.

## Building

To build a self-contained JAR:

    mvn clean package

## Running

To run the self-contained JAR (assuming you have configured it to be built):

   java -jar target/quarkus-broker-rest-1.0.0-runner.jar

To run in development mode, using

    mvn quarkus:dev

A useful feature of development mode, apart from enabling remote debugging,
is that allows the log level to be changed using a keypress. It also
enables dynamic rebuilding, which can save time; but dynamic rebuilding
does not work flawlessly with this application.

## Native compilation

If GraalVM, or an equivalent, is installed, this application can be compiled
to a native executable, and will run without a JVM. The Quarkus maintainers
now recommend Mandrel for compiling Quarkus to native code:
https://github.com/graalvm/mandrel/releases

To build the native executable it should only be necessary to use the
`-Pnative` profile with Maven:

	GRAALVM_HOME=/path/to/graalvm mvn clean package -Pnative

This compilation process takes a long time for such a small program: 
minutes to tens of minutes. 

## Further work

An obvious way this simple application could be extended is to
handle messages of types other than text. There are various ways
that a client might send a non-text message. It might, for example,
just send the data, and set an appropriate content-type that the
application would have to interpret. That's not particularly 
difficult with Camel. Or it might encode the message body
as, say, base-64, and wrap it up in a JSON message body. This 
approach would allow other JSON attributes to be used to
configure the application's behaviour (e.g., message persistent/volatile,
priority).

Similarly, a receiving client may receive a blob of data, and a
corresponding content-type header. However, there's no direct mapping
for HTTP content types to JMS message types, and I'm not sure there's
really a place for anything other than "text" or "binary".

Another useful feature, which could be trivially added as a new pair
of REST endpoints, would be a way to publish and subscribe using
topics, rather than queues. 

It would also be useful to set a timeout on message consumption, so that
the consumption fails in an orderly way before the HTTP client
times out. This could be used (e.g., with "timeout=0') to provide an
asynchronous message receiver.

## Notes

_HTTP headers_. Any HTTP headers sent by the client will be converted
into JMS message headers, and sent along with the message body.

_Error handling_. If the client sends a POST request, and the message
cannot be delivered to the broker, the message text will be stored in
the `dead_messages` directory as a file. The client will also get a
500 response, so it knows that the failure occurred. The application
saves the message because the client might, in principle, fail before
getting a response from the application, and not know whether the message
was delivered or not. It's also possible -- again theoretically -- 
that the broker might fail between storing the message sent by the
application, and acknowledging it. Again, the application should
store a copy of the message (but this is hard to test).


