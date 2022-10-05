/*===========================================================================
 
  MyRouteBuilder.java

  This class defines the Camel routes, including REST enpoints,
  for quarkus_broker_rest

  Copyright (c)2022 Kevin Boone, GPL v3.0

===========================================================================*/

package me.kevinboone.apacheintegration.quarkus_broker_res5t;

import org.apache.camel.builder.RouteBuilder;
import io.quarkus.runtime.StartupEvent;
import javax.enterprise.event.Observes;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;

public class MyRouteBuilder extends RouteBuilder 
  {
  public void configure() throws Exception 
    {
    //restConfiguration().bindingMode (RestBindingMode.json);

    /* Use an exception handler to deal with failed message delivery. 
       In this case, we just dump the failed message as a file to a
      specified 'dead message' directory. */
    onException (javax.jms.JMSException.class)
      .handled(true)
      // We can choose what response code is appropriate in
      //   this situation -- 500 "internal error" is generic.
      .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
      // Set the response type, because it will otherwise be
      //   obtained from the type of the original request
      .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
      .to ("file:dead_messages")
      // Since we have now stored the dead message, we can set the
      //   message body to return to the HTTP client. 
      .setBody().simple
         ("Broker operation failed: ${exception.message}");
 
    // Define the message producer endpoint, which requires a POST
    //    {{queue}} is a placeholder that will be substituted from
    //    the client's request, and end up as an Exchange header.
    rest("/broker/produce/{queue}")
       .post()
       .to ("direct:produce");

    // Define the message consumer endpoint, which requires a GET 
    rest("/broker/consume/{queue}")
       .get()
       .to ("direct:consume");

    from ("direct:produce")
      // We _might_ want to remove Camel-level headers from the
      //   request here, because otherwise they will end up as 
      //   JMS message headers, sent to the message broker. However, we
      //   probably want to leave the client the option to set its own
      //   headers. We aren't doing any such filtering here.
      .log (LoggingLevel.INFO, "Producing a message to ${header.queue}")
      // Note that we must use toD() here, because the client will be
      //   specifying the queue name, which is part of the endpoint
      //   definition. We could use to() if the application only 
      //   supported a single queue.
      .toD ("amqp:queue:${header.queue}?disableReplyTo=true&jmsMessageType=Text")
      // Return some simple text to the client. If we get this far, it's
      //   almost certain that the message was delivered
      .setBody().constant ("OK\r\n");

    from ("direct:consume")
      .log (LoggingLevel.INFO, "Receiving a message from ${header.queue}")
      // Use pollEnrish() to consume the message. It is blocking in this
      //   configuration. The simple() expression contains the queue
      //   name, that was set by the client's request.
      .pollEnrich()
      .simple ("amqp:queue:${header.queue}?disableReplyTo=true")
      // We only support plain text messages in this application. Howedver,
      //   let's set the content type in case the client did something
      //   silly.
      .setHeader("ContentType").constant ("text/plain");
    }
  }


