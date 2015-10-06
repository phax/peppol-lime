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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.xml.XMLFactory;
import com.helger.jaxb.JAXBContextCache;
import com.helger.peppol.lime.api.CTransportIdentifiers;
import com.helger.peppol.lime.api.Entry;
import com.helger.peppol.lime.api.IMessageMetadata;
import com.helger.peppol.lime.api.MessageMetadataHelper;
import com.helger.peppol.lime.api.PageListType;
import com.helger.peppol.lime.api.wstransfer.GetResponse;
import com.helger.peppol.lime.api.wstransfer.Resource;
import com.helger.peppol.lime.client.CLimeIdentifiers;
import com.helger.peppol.lime.client.IEndpointReference;
import com.helger.peppol.lime.client.IInbox;
import com.helger.peppol.lime.client.IMessage;
import com.helger.peppol.lime.client.IMessageReference;
import com.helger.peppol.lime.client.MessageException;
import com.helger.peppol.lime.client.soapheader.SoapHeaderMapper;
import com.helger.peppol.lime.client.username.IReadonlyUsernamePWCredentials;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * @author Ravnholt
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class Inbox implements IInbox
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (Inbox.class);

  private static void _validateCredentialsObj (@Nonnull final IReadonlyUsernamePWCredentials aCredentials) throws MessageException
  {
    if (aCredentials == null)
      throw new MessageException ("Credentials can not be a null value");

    if (StringHelper.hasNoTextAfterTrim (aCredentials.getUsername ()) ||
        StringHelper.hasNoTextAfterTrim (aCredentials.getPassword ()))
      throw new MessageException ("Credentials are invalid, username=" +
                                  aCredentials.getUsername () +
                                  " password=" +
                                  aCredentials.getPassword ());
  }

  @Nonnull
  @Nonempty
  @ReturnsMutableCopy
  private static List <Element> _createChannelReferenceParameter (final IEndpointReference aEndpointReference)
  {
    final Document aDummyDoc = XMLFactory.newDocument ();
    final Element node = aDummyDoc.createElementNS (CTransportIdentifiers.NAMESPACE_TRANSPORT_IDS,
                                                    CLimeIdentifiers.CHANNELID);
    node.setTextContent (aEndpointReference.getChannelID ());
    return CollectionHelper.newList (node);
  }

  public List <IMessageReference> getMessageList (final IReadonlyUsernamePWCredentials aCredentials,
                                                  final IEndpointReference aEndpointReference) throws MessageException
  {
    _validateCredentialsObj (aCredentials);
    try
    {
      final List <Element> aReferenceParameters = _createChannelReferenceParameter (aEndpointReference);
      final List <IMessageReference> aMessages = new ArrayList <IMessageReference> ();
      boolean bMorePages;
      do
      {
        bMorePages = _getSinglePage (aEndpointReference, aReferenceParameters, aCredentials, aMessages);
      } while (bMorePages);
      return aMessages;
    }
    catch (final Exception e)
    {
      s_aLogger.warn ("Failed to get message list", e);
      throw new MessageException (e);
    }
  }

  public List <IMessageReference> getMessageListPage (final IReadonlyUsernamePWCredentials aCredentials,
                                                      final IEndpointReference aEndpointReference,
                                                      final int nPageNumber) throws MessageException
  {
    _validateCredentialsObj (aCredentials);
    try
    {
      final List <IMessageReference> aMessages = new ArrayList <IMessageReference> ();
      _getSinglePage (aEndpointReference, null, aCredentials, aMessages);
      return aMessages;
    }
    catch (final Exception e)
    {
      throw new MessageException (e);
    }
  }

  public IMessage getMessage (final IReadonlyUsernamePWCredentials aCredentials,
                              final IMessageReference aMessageReference) throws MessageException
  {
    _validateCredentialsObj (aCredentials);
    try
    {
      // get a specific message
      final Resource aPort = LimeHelper.createServicePort (aMessageReference.getEndpointReference ().getAddress (),
                                                           aCredentials);
      SoapHeaderMapper.setupHandlerChain ((BindingProvider) aPort,
                                          aMessageReference.getEndpointReference ().getChannelID (),
                                          aMessageReference.getMessageID (),
                                          null);

      // no body required
      final GetResponse aGetResponse = aPort.get (null);
      final List <Object> aObjects = aGetResponse.getAny ();

      if (CollectionHelper.getSize (aObjects) == 1)
      {
        final Document document = ((Node) CollectionHelper.getFirstElement (aObjects)).getOwnerDocument ();
        final Message aMessage = new Message ();
        aMessage.setDocument (document);
        aMessage.setMessageID (aMessageReference.getMessageID ());
        _setMessageMetadata (aPort, aMessage);
        return aMessage;
      }
      throw new MessageException ("No message found with id: " + aMessageReference.getMessageID ());
    }
    catch (final Exception e)
    {
      s_aLogger.warn ("Inbox error: ", e);
      throw new MessageException (e);
    }
  }

  public void deleteMessage (final IReadonlyUsernamePWCredentials aCredentials,
                             final IMessageReference aMessageReference) throws MessageException
  {
    _validateCredentialsObj (aCredentials);
    try
    {
      // Delete a specific message
      final Resource aPort = LimeHelper.createServicePort (aMessageReference.getEndpointReference ().getAddress (),
                                                           aCredentials);
      SoapHeaderMapper.setupHandlerChain ((BindingProvider) aPort,
                                          aMessageReference.getEndpointReference ().getChannelID (),
                                          aMessageReference.getMessageID (),
                                          null);
      aPort.delete (null);
    }
    catch (final Exception e)
    {
      throw new MessageException (e);
    }
  }

  // TODO MessageReferenceInterface skal ændres til at indeholde en
  // endpointreference og reference parameters
  private static boolean _getSinglePage (@Nonnull final IEndpointReference aEndpointReference,
                                         @Nullable final List <Element> aReferenceParameters,
                                         @Nonnull final IReadonlyUsernamePWCredentials aCredentials,
                                         @Nonnull final List <IMessageReference> aMessages) throws JAXBException,
                                                                                            DOMException,
                                                                                            KeyManagementException,
                                                                                            NoSuchAlgorithmException
  {
    s_aLogger.info ("Retrieving inbox messages");
    // Get a message list
    final Resource aPort = LimeHelper.createServicePort (aEndpointReference.getAddress (), aCredentials);
    SoapHeaderMapper.setupHandlerChain ((BindingProvider) aPort, null, null, aReferenceParameters);
    final GetResponse aGetResponse = aPort.get (null);

    boolean bMorePages = false;
    if (aGetResponse != null && CollectionHelper.getSize (aGetResponse.getAny ()) == 1)
    {
      final Unmarshaller unmarshaller = JAXBContextCache.getInstance ()
                                                        .getFromCache (PageListType.class)
                                                        .createUnmarshaller ();
      final Node aResponseAnyNode = (Node) CollectionHelper.getFirstElement (aGetResponse.getAny ());
      final PageListType aPageList = unmarshaller.unmarshal (aResponseAnyNode, PageListType.class).getValue ();
      if (aPageList != null && aPageList.getEntryList () != null)
      {
        for (final Entry aEntry : aPageList.getEntryList ().getEntry ())
        {
          final IMessageReference aMsgReference = new MessageReference ();
          aMsgReference.setEndpointReference (aEndpointReference);
          // Find the message ID
          for (final Element e : W3CEndpointReferenceHelper.getReferenceParameters (aEntry.getEndpointReference ()))
            if (CLimeIdentifiers.MESSAGEID.equals (e.getLocalName ()))
            {
              aMsgReference.setMessageId (e.getTextContent ());
              break;
            }

          aMessages.add (aMsgReference);
        }
        if (aPageList.getNextPageIdentifier () != null &&
            aPageList.getNextPageIdentifier ().getEndpointReference () != null)
        {
          final W3CEndpointReference aNextPageER = aPageList.getNextPageIdentifier ().getEndpointReference ();
          aEndpointReference.setAddress (W3CEndpointReferenceHelper.getAddress (aNextPageER));
          aReferenceParameters.clear ();
          aReferenceParameters.addAll (W3CEndpointReferenceHelper.getReferenceParameters (aNextPageER));
          bMorePages = true;
        }
      }
    }
    return bMorePages;
  }

  private static void _setMessageMetadata (final Resource port, final Message message) throws Exception
  {
    final HeaderList aHeaderList = (HeaderList) ((BindingProvider) port).getResponseContext ()
                                                                        .get (JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
    final IMessageMetadata aMetadata = MessageMetadataHelper.createMetadataFromHeaders (aHeaderList);
    message.setSender (aMetadata.getSenderID ());
    message.setReceiver (aMetadata.getRecipientID ());
    message.setDocumentType (aMetadata.getDocumentTypeID ());
    message.setProcessType (aMetadata.getProcessID ());
  }
}
