/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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

package com.helger.peppol.lime.api.wstransfer;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.2.7-b01 Generated
 * source version: 2.2
 */
@WebService (name = "Resource", targetNamespace = "http://www.w3.org/2009/02/ws-tra")
@SOAPBinding (parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso ({ com.helger.peppol.identifier.ObjectFactory.class,
               com.helger.peppol.lime.api.wstransfer.ObjectFactory.class })
public interface Resource
{

  /**
   * @param body
   * @return returns org.w3._2009._02.ws_tra.GetResponse
   */
  @WebMethod (operationName = "Get")
  @WebResult (name = "GetResponse", targetNamespace = "http://www.w3.org/2009/02/ws-tra", partName = "Body")
  @Action (input = "http://www.w3.org/2009/02/ws-tra/Get", output = "http://www.w3.org/2009/02/ws-tra/GetResponse")
  public GetResponse get (@WebParam (name = "Get",
                                     targetNamespace = "http://www.w3.org/2009/02/ws-tra",
                                     partName = "Body") Get body);

  /**
   * @param body
   * @return returns org.w3._2009._02.ws_tra.PutResponse
   */
  @WebMethod (operationName = "Put")
  @WebResult (name = "PutResponse", targetNamespace = "http://www.w3.org/2009/02/ws-tra", partName = "Body")
  @Action (input = "http://www.w3.org/2009/02/ws-tra/Put", output = "http://www.w3.org/2009/02/ws-tra/PutResponse")
  public PutResponse put (@WebParam (name = "Put",
                                     targetNamespace = "http://www.w3.org/2009/02/ws-tra",
                                     partName = "Body") Put body);

  /**
   * @param body
   * @return returns org.w3._2009._02.ws_tra.DeleteResponse
   */
  @WebMethod (operationName = "Delete")
  @WebResult (name = "DeleteResponse", targetNamespace = "http://www.w3.org/2009/02/ws-tra", partName = "Body")
  @Action (input = "http://www.w3.org/2009/02/ws-tra/Delete",
           output = "http://www.w3.org/2009/02/ws-tra/DeleteResponse")
  public DeleteResponse delete (@WebParam (name = "Delete",
                                           targetNamespace = "http://www.w3.org/2009/02/ws-tra",
                                           partName = "Body") Delete body);

  /**
   * @param body
   * @return returns org.w3._2009._02.ws_tra.CreateResponse
   * @throws FaultMessage
   */
  @WebMethod (operationName = "Create")
  @WebResult (name = "CreateResponse", targetNamespace = "http://www.w3.org/2009/02/ws-tra", partName = "Body")
  @Action (input = "http://www.w3.org/2009/02/ws-tra/Create",
           output = "http://www.w3.org/2009/02/ws-tra/CreateResponse")
  public CreateResponse create (@WebParam (name = "Create",
                                           targetNamespace = "http://www.w3.org/2009/02/ws-tra",
                                           partName = "Body") Create body);

}
