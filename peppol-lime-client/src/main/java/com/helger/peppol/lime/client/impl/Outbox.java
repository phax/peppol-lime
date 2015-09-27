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
package com.helger.peppol.lime.client.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.string.StringHelper;
import com.helger.peppol.lime.api.IMessageMetadata;
import com.helger.peppol.lime.api.MessageMetadata;
import com.helger.peppol.lime.api.MessageMetadataHelper;
import com.helger.peppol.lime.api.wstransfer.Create;
import com.helger.peppol.lime.api.wstransfer.CreateResponse;
import com.helger.peppol.lime.api.wstransfer.Put;
import com.helger.peppol.lime.api.wstransfer.Resource;
import com.helger.peppol.lime.api.wstransfer.ResourceCreated;
import com.helger.peppol.lime.client.CLimeIdentifiers;
import com.helger.peppol.lime.client.IEndpointReference;
import com.helger.peppol.lime.client.IMessage;
import com.helger.peppol.lime.client.IOutbox;
import com.helger.peppol.lime.client.MessageException;
import com.helger.peppol.lime.client.username.IReadonlyUsernamePWCredentials;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.developer.WSBindingProvider;

/**
 * @author Ravnholt<br>
 *         PEPPOL.AT, BRZ, Philip Helger
 */
public final class Outbox implements IOutbox
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (Outbox.class);

  private static void _validateCredentials (@Nonnull final IReadonlyUsernamePWCredentials aCredentials) throws MessageException
  {
    if (aCredentials == null)
      throw new MessageException ("Credentials can not be a null value");

    if (StringHelper.hasNoTextAfterTrim (aCredentials.getUsername ()) ||
        StringHelper.hasNoTextAfterTrim (aCredentials.getPassword ()))
    {
      throw new MessageException ("Credentials are invalid, username=" +
                                  aCredentials.getUsername () +
                                  " password=" +
                                  aCredentials.getPassword ());
    }
  }

  @Nonnull
  private static EndpointReferenceWithMessageID _createEndpointReferenceDocument (final CreateResponse createResponse)
  {
    ResourceCreated resourceCreated = (ResourceCreated) createResponse.getAny ();
    if (resourceCreated == null)
    {
      resourceCreated = createResponse.getResourceCreated ();
      if (resourceCreated == null)
        throw new IllegalStateException ("No content of create response!");
    }

    final W3CEndpointReference aEndpointReference = resourceCreated.getEndpointReference ().get (0);

    // Extract address, channel ID and message ID
    final EndpointReferenceWithMessageID ret = new EndpointReferenceWithMessageID ();
    ret.setAddress (W3CEndpointReferenceHelper.getAddress (aEndpointReference));
    for (final Element e : W3CEndpointReferenceHelper.getReferenceParameters (aEndpointReference))
    {
      if (CLimeIdentifiers.CHANNELID.equals (e.getLocalName ()))
        ret.setChannelID (e.getTextContent ());
      else
        if (CLimeIdentifiers.MESSAGEID.equals (e.getLocalName ()))
          ret.setMessageID (e.getTextContent ());
        else
          s_aLogger.warn ("EndpointReference contains illegal element " + e.getLocalName ());
    }
    return ret;
  }

  /*
   * Send a new message and return the created message ID
   */
  public String sendMessage (final IReadonlyUsernamePWCredentials aCredentials,
                             final IMessage aMessage,
                             final IEndpointReference aEndpointReference) throws MessageException
  {
    _validateCredentials (aCredentials);

    try
    {
      // Create metadata (everything except messageID)
      final IMessageMetadata aMetadata = new MessageMetadata (null,
                                                              aEndpointReference.getChannelID (),
                                                              aMessage.getSender (),
                                                              aMessage.getReceiver (),
                                                              aMessage.getDocumentType (),
                                                              aMessage.getProcessType () != null ? aMessage.getProcessType ()
                                                                                                 : CLimeIdentifiers.MESSAGEUNDELIVERABLE_PROCESS);

      // Create "create" port
      Resource aPort = LimeHelper.createServicePort (aEndpointReference.getAddress (), aCredentials);
      List <Header> aHeaders = MessageMetadataHelper.createHeadersFromMetadata (aMetadata);
      ((WSBindingProvider) aPort).setOutboundHeaders (aHeaders);

      // Perform "create" action
      final CreateResponse createResponse = aPort.create (new Create ());

      // Evaluate "create" response
      final EndpointReferenceWithMessageID aEndpointDoc = _createEndpointReferenceDocument (createResponse);

      // Create "put" port
      aPort = LimeHelper.createServicePort (aEndpointDoc.getAddress (), aCredentials);
      aHeaders = MessageMetadataHelper.createHeadersFromMetadata (aEndpointDoc);
      ((WSBindingProvider) aPort).setOutboundHeaders (aHeaders);

      // Perform "put" action (no real response expected)
      final Put put = new Put ();
      put.getAny ().add (aMessage.getDocument ().getDocumentElement ());
      aPort.put (put);

      return aEndpointDoc.getMessageID ();
    }
    catch (final Exception e)
    {
      s_aLogger.warn ("Outbox error", e);
      throw new MessageException (e);
    }
  }
}
