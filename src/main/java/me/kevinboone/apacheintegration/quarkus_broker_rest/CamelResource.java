/*===========================================================================
 
  CamelResource.java

  Copyright (c)2022 Kevin Boone, GPL v3.0

===========================================================================*/

package me.kevinboone.apacheintegration.quarkus_camel_amqp_jdbc;

import io.quarkus.runtime.StartupEvent;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.apache.camel.CamelContext;

@ApplicationScoped
public class CamelResource 
  {
  //@Inject
  //@DataSource("camel-ds")
  //AgroalDataSource dataSource;

  void startup (@Observes StartupEvent event, CamelContext context) 
     throws Exception 
    {
    context.getRouteController().startAllRoutes();
    }

  @PostConstruct
  void postConstruct() throws Exception 
    {
    }
  }

