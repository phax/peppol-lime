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
package com.helger.peppol.lime.client.impl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.xml.ws.BindingProvider;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.random.VerySecureRandom;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.lime.api.cert.AccessPointX509TrustManager;
import com.helger.peppol.lime.api.wstransfer.Resource;
import com.helger.peppol.lime.client.username.IUsernamePWCredentials;
import com.helger.peppol.lime.client.ws.LimeClientService;

/**
 * @author Ravnholt
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class LimeHelper
{
  private LimeHelper ()
  {}

  private static SSLSocketFactory _createSSLSocketFactory () throws NoSuchAlgorithmException, KeyManagementException
  {
    final TrustManager [] aTrustManagers = new TrustManager [] { new AccessPointX509TrustManager (null, null) };
    // TLS is important for IBM JDK (no difference for Oracle JDK)
    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init (null, aTrustManagers, VerySecureRandom.getInstance ());
    return aSSLContext.getSocketFactory ();
  }

  private static HostnameVerifier _createHostnameVerifier ()
  {
    final HostnameVerifier aHostnameVerifier = new HostnameVerifier ()
    {
      public boolean verify (final String sUrlHostName, final SSLSession aSSLSession)
      {
        return sUrlHostName.equals (aSSLSession.getPeerHost ());
      }
    };
    return aHostnameVerifier;
  }

  @Nonnull
  public static Resource createServicePort (@Nonnull @Nonempty final String sAPStr,
                                            @Nonnull final IUsernamePWCredentials aCredentials) throws KeyManagementException,
                                                                                                        NoSuchAlgorithmException
  {
    if (StringHelper.hasNoTextAfterTrim (sAPStr))
      throw new IllegalArgumentException ("LIME access point url is empty");

    final LimeClientService aService = new LimeClientService ();
    final Resource aPort = aService.getResourceBindingPort ();
    final BindingProvider aBP = (BindingProvider) aPort;
    aBP.getRequestContext ().put (BindingProvider.USERNAME_PROPERTY, aCredentials.getUsername ());
    aBP.getRequestContext ().put (BindingProvider.PASSWORD_PROPERTY, aCredentials.getPassword ());
    aBP.getRequestContext ().put (BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sAPStr);

    final SSLSocketFactory aSSLSocketFactory = _createSSLSocketFactory ();
    if (aSSLSocketFactory != null)
    {
      aBP.getRequestContext ().put ("com.sun.xml.ws.transport.https.client.SSLSocketFactory", aSSLSocketFactory);
      aBP.getRequestContext ().put ("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory",
                                    aSSLSocketFactory);
      // Set as default as well because Metro has problems with the properties
      // See https://java.net/jira/browse/WSIT-1632
      HttpsURLConnection.setDefaultSSLSocketFactory (aSSLSocketFactory);
    }

    final HostnameVerifier aHostnameVerifier = _createHostnameVerifier ();
    if (aHostnameVerifier != null)
    {
      aBP.getRequestContext ().put ("com.sun.xml.ws.transport.https.client.hostname.verifier", aHostnameVerifier);
      aBP.getRequestContext ().put ("com.sun.xml.internal.ws.transport.https.client.hostname.verifier",
                                    aHostnameVerifier);
      // Set as default as well because Metro has problems with the properties
      // See https://java.net/jira/browse/WSIT-1632
      HttpsURLConnection.setDefaultHostnameVerifier (aHostnameVerifier);
    }

    return aPort;
  }
}
