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
package com.helger.peppol.lime.client.ws;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.url.URLHelper;
import com.helger.peppol.lime.api.wstransfer.Resource;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.3-b02- Generated
 * source version: 2.1
 */
@WebServiceClient (name = "limeClientService",
                   targetNamespace = "http://www.w3.org/2009/02/ws-tra",
                   wsdlLocation = LimeClientService.WSDL_PATH)
public class LimeClientService extends Service
{
  public static final String WSDL_PATH = "WEB-INF/wsdl/peppol-lime-1.0.wsdl";
  private static final URL WSTRANSFERSERVICE_WSDL_LOCATION;

  static
  {
    final URL url = URLHelper.getClassPathURL (WSDL_PATH);
    if (url == null)
      throw new InitializationException ("Failed to create URL for the wsdl Location: '" +
                                         WSDL_PATH +
                                         "', retrying as a local file");
    WSTRANSFERSERVICE_WSDL_LOCATION = url;
  }

  public LimeClientService ()
  {
    super (WSTRANSFERSERVICE_WSDL_LOCATION, new QName ("http://www.w3.org/2009/02/ws-tra", "limeService"));
  }

  /**
   * @return returns Resource
   */
  @WebEndpoint (name = "ResourceBindingPort")
  public Resource getResourceBindingPort ()
  {
    return super.getPort (new QName ("http://www.w3.org/2009/02/ws-tra", "ResourceBindingPort"), Resource.class);
  }

  /**
   * @param features
   *        A list of {@link javax.xml.ws.WebServiceFeature} to configure on the
   *        proxy. Supported features not in the <code>features</code> parameter
   *        will have their default values.
   * @return returns Resource
   */
  @WebEndpoint (name = "ResourceBindingPort")
  public Resource getResourceBindingPort (final WebServiceFeature... features)
  {
    return super.getPort (new QName ("http://www.w3.org/2009/02/ws-tra", "ResourceBindingPort"),
                          Resource.class,
                          features);
  }
}
