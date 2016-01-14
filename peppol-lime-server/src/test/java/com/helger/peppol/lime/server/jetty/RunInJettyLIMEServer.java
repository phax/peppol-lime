/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.lime.server.jetty;

import java.io.File;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.system.SystemProperties;
import com.helger.peppol.smpclient.SMPClientConfiguration;

/**
 * Run this as an application and your SML will be up and running on port 8080
 * of your local machine. Please ensure that you have adopted the Hibernate
 * configuration file.<br>
 * To stop the running Jetty simply invoke the {@link JettyStopLIMEServer}
 * application in this package. It performs a graceful shutdown of the App
 * Server.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class RunInJettyLIMEServer
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (RunInJettyLIMEServer.class);
  private static final String RESOURCE_PREFIX = "target/webapp-classes";

  public static void main (final String... args) throws Exception
  {
    if (System.getSecurityManager () != null)
      throw new IllegalStateException ("Security Manager is set but not supported - aborting!");

    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();

    // Create main server
    final Server aServer = new Server ();
    // Create connector on Port
    final ServerConnector aConnector = new ServerConnector (aServer);
    aConnector.setPort (8091);
    aConnector.setIdleTimeout (30000);
    // aConnector.setStatsOn (true);
    aServer.setConnectors (new Connector [] { aConnector });

    final WebAppContext aWebAppCtx = new WebAppContext ();
    aWebAppCtx.setDescriptor (RESOURCE_PREFIX + "/WEB-INF/web.xml");
    aWebAppCtx.setResourceBase (RESOURCE_PREFIX);
    aWebAppCtx.setContextPath ("/");
    aWebAppCtx.setTempDirectory (new File (SystemProperties.getTmpDir () +
                                           '/' +
                                           RunInJettyLIMEServer.class.getName ()));
    aWebAppCtx.setParentLoaderPriority (true);
    // Important to add the AnnotationConfiguration!
    aWebAppCtx.setConfigurations (new Configuration [] { new WebInfConfiguration (),
                                                         new WebXmlConfiguration (),
                                                         new MetaInfConfiguration (),
                                                         new FragmentConfiguration (),
                                                         new JettyWebXmlConfiguration () });
    aServer.setHandler (aWebAppCtx);
    final ServletContextHandler aCtx = aWebAppCtx;

    // Setting final properties
    // Stops the server when ctrl+c is pressed (registers to
    // Runtime.addShutdownHook)
    aServer.setStopAtShutdown (true);
    // Starting shutdown listener thread
    new JettyMonitor ().start ();
    try
    {
      // Starting the engines:
      aServer.start ();
      if (aCtx.isFailed ())
      {
        s_aLogger.error ("Failed to start server - stopping server!");
        aServer.stop ();
        s_aLogger.error ("Failed to start server - stopped server!");
      }
      else
      {
        // Running the server!
        aServer.join ();
      }
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to run server!", ex);
    }
  }
}
