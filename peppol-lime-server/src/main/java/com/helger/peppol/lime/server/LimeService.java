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
package com.helger.peppol.lime.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as2lib.client.AS2Client;
import com.helger.as2lib.client.AS2ClientRequest;
import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.system.ENewLineMode;
import com.helger.commons.xml.XMLFactory;
import com.helger.jaxb.JAXBContextCache;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IProcessIdentifier;
import com.helger.peppol.lime.api.CTransportIdentifiers;
import com.helger.peppol.lime.api.IMessageMetadata;
import com.helger.peppol.lime.api.MessageMetadata;
import com.helger.peppol.lime.api.MessageMetadataHelper;
import com.helger.peppol.lime.api.MessageUndeliverableType;
import com.helger.peppol.lime.api.ObjectFactory;
import com.helger.peppol.lime.api.ReasonCodeType;
import com.helger.peppol.lime.api.wstransfer.Create;
import com.helger.peppol.lime.api.wstransfer.CreateResponse;
import com.helger.peppol.lime.api.wstransfer.Delete;
import com.helger.peppol.lime.api.wstransfer.DeleteResponse;
import com.helger.peppol.lime.api.wstransfer.Get;
import com.helger.peppol.lime.api.wstransfer.GetResponse;
import com.helger.peppol.lime.api.wstransfer.Put;
import com.helger.peppol.lime.api.wstransfer.PutResponse;
import com.helger.peppol.lime.api.wstransfer.ResourceCreated;
import com.helger.peppol.lime.client.CLimeIdentifiers;
import com.helger.peppol.lime.server.exception.MessageIdReusedException;
import com.helger.peppol.lime.server.exception.RecipientUnreachableException;
import com.helger.peppol.lime.server.storage.LimeStorage;
import com.helger.peppol.lime.server.storage.MessagePage;
import com.helger.peppol.sbdh.DocumentData;
import com.helger.peppol.sbdh.write.DocumentDataWriter;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.sbdh.SBDMarshaller;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * The main LIME web service
 *
 * @author Ravnholt
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@WebService (serviceName = "limeService",
             portName = "ResourceBindingPort",
             endpointInterface = "com.helger.peppol.lime.api.wstransfer.Resource",
             targetNamespace = "http://www.w3.org/2009/02/ws-tra",
             wsdlLocation = "WEB-INF/wsdl/peppol-lime-1.0.wsdl")
@HandlerChain (file = "WSTransferService_handler.xml")
public class LimeService
{
  private static final String FAULT_UNKNOWN_ENDPOINT = "The endpoint is not known";
  private static final String FAULT_SERVER_ERROR = "ServerError";
  private static final String SERVICENAME = LimeService.class.getAnnotation (WebService.class).serviceName ();
  private static final QName QNAME_PAGEIDENTIFIER = new QName (CLimeIdentifiers.NAMESPACE_LIME,
                                                               CLimeIdentifiers.PAGEIDENTIFIER);
  private static final Logger s_aLogger = LoggerFactory.getLogger (LimeService.class);

  private static final ObjectFactory s_aObjFactory = new ObjectFactory ();

  @Resource
  private WebServiceContext m_aWebServiceContext;

  /**
   * @return The {@link ServletContext} of the current WS request
   */
  @Nonnull
  private ServletContext _getServletContext ()
  {
    return (ServletContext) m_aWebServiceContext.getMessageContext ().get (MessageContext.SERVLET_CONTEXT);
  }

  /**
   * @return The HTTP {@link HeaderList} of the current WS request
   */
  @Nonnull
  private HeaderList _getInboundHeaderList ()
  {
    return (HeaderList) m_aWebServiceContext.getMessageContext ().get (JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
  }

  /**
   * @return The {@link HttpServletRequest} of the current WS request
   */
  @Nonnull
  private HttpServletRequest _getServletRequest ()
  {
    return (HttpServletRequest) m_aWebServiceContext.getMessageContext ().get (MessageContext.SERVLET_REQUEST);
  }

  @Nonnull
  private static W3CEndpointReference _createW3CEndpointReference (@Nonnull final String sOurAPURL,
                                                                   @Nonnull final String sChannelID,
                                                                   @Nonnull final String sMessageID)
  {
    final Document aDummyDoc = XMLFactory.newDocument ();
    final List <Element> aReferenceParameters = new ArrayList <Element> ();

    // Channel ID
    Element aElement = aDummyDoc.createElementNS (CTransportIdentifiers.NAMESPACE_TRANSPORT_IDS,
                                                  CLimeIdentifiers.CHANNELID);
    aElement.appendChild (aDummyDoc.createTextNode (sChannelID));
    aReferenceParameters.add (aElement);

    // Message ID
    aElement = aDummyDoc.createElementNS (CTransportIdentifiers.NAMESPACE_TRANSPORT_IDS, CLimeIdentifiers.MESSAGEID);
    aElement.appendChild (aDummyDoc.createTextNode (sMessageID));
    aReferenceParameters.add (aElement);

    return W3CEndpointReferenceHelper.createEndpointReference (sOurAPURL, aReferenceParameters);
  }

  @Nonnull
  private static CreateResponse _createCreateResponse (@Nonnull final String sOurAPURL,
                                                       @Nonnull final String sChannelID,
                                                       @Nonnull final String sMessageID)
  {
    final CreateResponse ret = new CreateResponse ();
    {
      final ResourceCreated aResourceCreated = new ResourceCreated ();
      final W3CEndpointReference w3CEndpointReference = _createW3CEndpointReference (sOurAPURL, sChannelID, sMessageID);
      aResourceCreated.getEndpointReference ().add (w3CEndpointReference);
      ret.setResourceCreated (aResourceCreated);
    }
    return ret;
  }

  /**
   * Called to initiate a new message. All standard PEPPOL SOAP headers except
   * messageID must be passed in. The messageID is created in this method and
   * returned.
   *
   * @param body
   *        The body - is ignored. May be <code>null</code>.
   * @return A non-<code>null</code> response containing a
   *         {@link ResourceCreated} object containing our AP URL, message ID
   *         and channel ID.
   */
  @Nonnull
  public CreateResponse create (@Nullable final Create body)
  {
    // Create a new unique messageID
    final String sMessageID = "uuid:" + UUID.randomUUID ().toString ();
    final String sOurAPURL = _getOurAPURL ();

    IMessageMetadata aMetadata = null;

    try
    {
      // Grabs the list of headers from the SOAP message
      final HeaderList aHeaderList = _getInboundHeaderList ();
      aMetadata = MessageMetadataHelper.createMetadataFromHeadersWithCustomMessageID (aHeaderList, sMessageID);

      if (ResourceMemoryStore.getInstance ().createResource (sMessageID, sOurAPURL, aMetadata).isUnchanged ())
        throw new MessageIdReusedException ("Message id '" +
                                            sMessageID +
                                            "' is reused by this LIME service. Seems like we have a problem with the UUID generator");
    }
    catch (final Exception ex)
    {
      throw _createSoapFault (FAULT_SERVER_ERROR, ex);
    }

    // Will not happen
    if (aMetadata == null)
      throw _createSoapFault (FAULT_SERVER_ERROR, new IllegalStateException ());

    return _createCreateResponse (sOurAPURL, aMetadata.getChannelID (), sMessageID);
  }

  /**
   * After {@link #create(Create)} the main document can be transmitted using
   * this method. Expects the message ID from {@link #create(Create)} as a SOAP
   * header.
   *
   * @param aBody
   *        The message to be put.
   * @return An empty, non-<code>null</code> put response.
   */
  @Nonnull
  public PutResponse put (@Nonnull final Put aBody)
  {
    final HeaderList aHeaderList = _getInboundHeaderList ();
    final String sMessageID = MessageMetadataHelper.getMessageID (aHeaderList);
    final String sOwnAPURL = _getOurAPURL ();
    final IMessageMetadata aMetadata = ResourceMemoryStore.getInstance ().getMessage (sMessageID, sOwnAPURL);
    final ISMLInfo aSML = LimeServerConfiguration.getSML ();

    try
    {
      if (aMetadata == null)
        throw new IllegalStateException ("No such message ID found: " + sMessageID);

      final EndpointType aSenderEndpoint = _getEndpoint (aMetadata.getSenderID (),
                                                         aMetadata.getDocumentTypeID (),
                                                         aMetadata.getProcessID (),
                                                         aSML);
      if (aSenderEndpoint == null)
        throw new IllegalStateException ("Failed to resolve sender endpoint URL for " + aMetadata.toString ());

      final EndpointType aRecipientEndpoint = _getEndpoint (aMetadata.getRecipientID (),
                                                            aMetadata.getDocumentTypeID (),
                                                            aMetadata.getProcessID (),
                                                            aSML);
      if (aRecipientEndpoint == null)
        throw new IllegalStateException ("Failed to resolve recipient endpoint URL for " + aMetadata.toString ());

      final String sSenderURL = W3CEndpointReferenceHelper.getAddress (aSenderEndpoint.getEndpointReference ());
      final String sRecipientURL = W3CEndpointReferenceHelper.getAddress (aRecipientEndpoint.getEndpointReference ());
      if (EqualsHelper.equalsIgnoreCase (sSenderURL, sRecipientURL))
      {
        _logRequest ("This is a local request - sending directly to inbox",
                     sSenderURL,
                     aMetadata,
                     "INBOX: " + aMetadata.getRecipientID ().getValue ());
        _sendToInbox (aMetadata, aBody);
      }
      else
      {
        _logRequest ("This is a request for a remote access point", sSenderURL, aMetadata, sRecipientURL);
        _sendToAccessPoint (aBody, aRecipientEndpoint, aMetadata);
      }
    }
    catch (final RecipientUnreachableException ex)
    {
      _sendMessageUndeliverable (ex, sMessageID, ReasonCodeType.TRANSPORT_ERROR, aMetadata);
      throw _createSoapFault (FAULT_UNKNOWN_ENDPOINT, ex);
    }
    catch (final Exception ex)
    {
      _sendMessageUndeliverable (ex, sMessageID, ReasonCodeType.OTHER_ERROR, aMetadata);
      throw _createSoapFault (FAULT_SERVER_ERROR, ex);
    }
    return new PutResponse ();
  }

  /**
   * Retrieve a list of messages, or a certain message from the inbox.
   *
   * @param body
   *        Ignored - may be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public GetResponse get (@Nullable final Get body)
  {
    final HeaderList aHeaderList = _getInboundHeaderList ();
    final String sChannelID = MessageMetadataHelper.getChannelID (aHeaderList);
    final String sMessageID = MessageMetadataHelper.getMessageID (aHeaderList);
    final String sPageIdentifier = MessageMetadataHelper.getStringContent (aHeaderList.get (QNAME_PAGEIDENTIFIER,
                                                                                            false));

    final GetResponse aGetResponse = new GetResponse ();
    try
    {
      final String sStorageRoot = _getServletContext ().getRealPath ("/");
      if (StringHelper.hasNoText (sMessageID))
        _addPageListToResponse (sStorageRoot, sPageIdentifier, sChannelID, aGetResponse);
      else
        _addSingleMessageToResponse (sStorageRoot, sChannelID, sMessageID, aGetResponse);
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error on get", ex);
    }
    return aGetResponse;
  }

  /**
   * Delete
   *
   * @param body
   *        delete body
   * @return response
   */
  public DeleteResponse delete (final Delete body)
  {
    final HeaderList aHeaderList = _getInboundHeaderList ();
    final String channelID = MessageMetadataHelper.getChannelID (aHeaderList);
    final String messageID = MessageMetadataHelper.getMessageID (aHeaderList);
    try
    {
      final String sStorageRoot = _getServletContext ().getRealPath ("/");
      new LimeStorage (sStorageRoot).deleteDocument (channelID, messageID);
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error deleting document", ex);
    }
    return new DeleteResponse ();
  }

  private void _sendMessageUndeliverable (@Nonnull final Exception ex,
                                          @Nullable final String sMessageID,
                                          @Nonnull final ReasonCodeType eReasonCode,
                                          @Nullable final IMessageMetadata aMetadata)
  {
    if (aMetadata == null)
    {
      s_aLogger.error ("No message metadata found. Unable to send MessageUndeliverable for Message ID: " + sMessageID);
    }
    else
    {
      try
      {
        s_aLogger.warn ("Unable to send MessageUndeliverable for Message ID: " +
                        sMessageID +
                        " Reason: " +
                        ex.getMessage ());

        final MessageUndeliverableType aMsgUndeliverable = s_aObjFactory.createMessageUndeliverableType ();
        aMsgUndeliverable.setMessageIdentifier (sMessageID);
        aMsgUndeliverable.setReasonCode (eReasonCode);
        aMsgUndeliverable.setDetails ("(" +
                                      aMetadata.getRecipientID ().getScheme () +
                                      "," +
                                      aMetadata.getRecipientID ().getValue () +
                                      ") " +
                                      ex.getMessage ());

        final IMessageMetadata aRealMetadata = new MessageMetadata (aMetadata.getMessageID (),
                                                                    aMetadata.getChannelID (),
                                                                    CLimeIdentifiers.MESSAGEUNDELIVERABLE_SENDER,
                                                                    aMetadata.getSenderID (),
                                                                    CLimeIdentifiers.MESSAGEUNDELIVERABLE_DOCUMENT,
                                                                    CLimeIdentifiers.MESSAGEUNDELIVERABLE_PROCESS);

        final Document aDocument = XMLFactory.newDocument ();
        final Marshaller aMarshaller = JAXBContextCache.getInstance ()
                                                       .getFromCache (MessageUndeliverableType.class)
                                                       .createMarshaller ();
        aMarshaller.marshal (s_aObjFactory.createMessageUndeliverable (aMsgUndeliverable), new DOMResult (aDocument));

        // Create a dummy "put" and send it to the inbox of the sender
        final Put put = new Put ();
        put.getAny ().add (aDocument.getDocumentElement ());
        _sendToInbox (aRealMetadata, put);
      }
      catch (final Exception ex1)
      {
        s_aLogger.error ("Unable to send MessageUndeliverable for Message ID: " + sMessageID, ex1);
      }
    }
  }

  @Nonnull
  private static SOAPFaultException _createSoapFault (final String faultMessage,
                                                      final Exception e) throws RuntimeException
  {
    try
    {
      s_aLogger.info ("Server error", e);
      final SOAPFault soapFault = SOAPFactory.newInstance ().createFault ();
      soapFault.setFaultString (faultMessage);
      soapFault.setFaultCode (new QName (SOAPConstants.URI_NS_SOAP_ENVELOPE, "Sender"));
      soapFault.setFaultActor ("LIME AP");
      return new SOAPFaultException (soapFault);
    }
    catch (final SOAPException e2)
    {
      throw new RuntimeException ("Problem processing SOAP Fault on service-side", e2);
    }
  }

  private static void _addSingleMessageToResponse (final String sStorageRoot,
                                                   final String sChannelID,
                                                   final String sMessageID,
                                                   final GetResponse getResponse) throws SAXException
  {
    final LimeStorage aStorage = new LimeStorage (sStorageRoot);
    final Document documentMetadata = aStorage.getDocumentMetadata (sChannelID, sMessageID);
    final Document document = aStorage.getDocument (sChannelID, sMessageID);
    getResponse.getAny ().add (documentMetadata.getDocumentElement ());
    getResponse.getAny ().add (document.getDocumentElement ());
  }

  @Nonnull
  private String _getOurAPURL ()
  {
    // FIXME read this from the configuration file for easily correct handling
    // of the endpoint URL
    final ServletRequest servletRequest = _getServletRequest ();
    final String sContextPath = _getServletContext ().getContextPath ();
    final String thisAccessPointURLstr = servletRequest.getScheme () +
                                         "://" +
                                         servletRequest.getServerName () +
                                         ":" +
                                         servletRequest.getLocalPort () +
                                         sContextPath +
                                         '/';
    return thisAccessPointURLstr + SERVICENAME;
  }

  private void _addPageListToResponse (@Nonnull @Nonempty final String sStorageRoot,
                                       @Nullable final String sPageNumber,
                                       final String sChannelID,
                                       final GetResponse aGetResponse) throws Exception
  {
    final String sOwnAPURL = _getOurAPURL ();
    final int nPageNumber = StringParser.parseInt (StringHelper.trim (sPageNumber), 0);
    final Document aDocument = MessagePage.getPageList (nPageNumber,
                                                        sOwnAPURL,
                                                        new LimeStorage (sStorageRoot),
                                                        sChannelID);
    if (aDocument != null)
      aGetResponse.getAny ().add (aDocument.getDocumentElement ());
  }

  private static void _logRequest (@Nullable final String sAction,
                                   @Nullable final String sOwnUrl,
                                   @Nonnull final IMessageMetadata aMetadata,
                                   @Nullable final String sReceiverID)
  {
    final String sNewLine = ENewLineMode.DEFAULT.getText ();
    final String s = "REQUEST start--------------------------------------------------" +
                     sNewLine +
                     "Action: " +
                     sAction +
                     sNewLine +
                     "Own URL: " +
                     sOwnUrl +
                     sNewLine +
                     "Sending to : " +
                     sReceiverID +
                     sNewLine +
                     "Messsage ID: " +
                     aMetadata.getMessageID () +
                     sNewLine +
                     "Sender ID: " +
                     aMetadata.getSenderID ().getValue () +
                     sNewLine +
                     "Sender type: " +
                     aMetadata.getSenderID ().getScheme () +
                     sNewLine +
                     "Recipient ID: " +
                     aMetadata.getRecipientID ().getValue () +
                     sNewLine +
                     "Recipient type: " +
                     aMetadata.getRecipientID ().getScheme () +
                     sNewLine +
                     "Document ID: " +
                     aMetadata.getDocumentTypeID ().getValue () +
                     sNewLine +
                     "Document type: " +
                     aMetadata.getDocumentTypeID ().getScheme () +
                     sNewLine +
                     "Process ID: " +
                     aMetadata.getProcessID ().getValue () +
                     sNewLine +
                     "Process type: " +
                     aMetadata.getProcessID ().getScheme () +
                     sNewLine +
                     "REQUEST end----------------------------------------------------" +
                     sNewLine;
    s_aLogger.info (s);
  }

  private static void _sendToAccessPoint (@Nonnull final Put aBody,
                                          @Nonnull final EndpointType aRecipientEndpoint,
                                          @Nonnull final IMessageMetadata aMetadata) throws Exception
  {
    final Element aSourceNode = (Element) aBody.getAnyAtIndex (0);

    // Build SBDH
    final DocumentData aDD = DocumentData.create (aSourceNode);
    aDD.setSender (aMetadata.getSenderID ().getScheme (), aMetadata.getSenderID ().getValue ());
    aDD.setReceiver (aMetadata.getRecipientID ().getScheme (), aMetadata.getRecipientID ().getValue ());
    aDD.setDocumentType (aMetadata.getDocumentTypeID ().getScheme (), aMetadata.getDocumentTypeID ().getValue ());
    aDD.setProcess (aMetadata.getProcessID ().getScheme (), aMetadata.getProcessID ().getValue ());

    final StandardBusinessDocument aSBD = new DocumentDataWriter ().createStandardBusinessDocument (aDD);
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    if (new SBDMarshaller ().write (aSBD, new StreamResult (aBAOS)).isFailure ())
      throw new IllegalStateException ("Failed to serialize SBD!");
    aBAOS.close ();

    // send message via AS2
    final AS2ClientRequest aRequest = new AS2ClientRequest ("OpenPEPPOL AS2 message");
    // Set as string - less problems than with byte[]
    aRequest.setData (aBAOS.getAsString (CCharset.CHARSET_UTF_8_OBJ), CCharset.CHARSET_UTF_8_OBJ);
    final AS2ClientSettings aSettings = AS2SettingsProvider.createAS2ClientSettings (aRecipientEndpoint, aMetadata);
    final AS2ClientResponse aResponse = new AS2Client ().sendSynchronous (aSettings, aRequest);
    if (aResponse.hasException ())
      s_aLogger.error ("Error sending to " + aSettings.getDestinationAS2URL () + ": " + aResponse.getAsString ());
    else
      s_aLogger.info ("Successfully forwarded message to " + aSettings.getDestinationAS2URL ());
  }

  private void _sendToInbox (@Nonnull final IMessageMetadata aMetadata,
                             @Nonnull final Put aBody) throws RecipientUnreachableException
  {
    final String sStorageChannelID = aMetadata.getRecipientID ().getValue ();
    if (sStorageChannelID == null)
      throw new RecipientUnreachableException ("Unknown recipient at LIME-AP: " + aMetadata.getRecipientID ());

    // Extract the message ID from the incoming message SOAP headers
    final HeaderList aHeaderList = _getInboundHeaderList ();
    final String sMessageID = MessageMetadataHelper.getMessageID (aHeaderList);

    s_aLogger.info ("Recipient: " + aMetadata.getRecipientID () + "; ChannelID: " + sStorageChannelID);

    try
    {
      final List <Object> aObjects = aBody.getAny ();
      if (CollectionHelper.getSize (aObjects) == 1)
      {
        final Node aElement = (Node) CollectionHelper.getFirstElement (aObjects);
        final Document aDocument = aElement.getOwnerDocument ();
        final Document aMetadataDocument = MessageMetadataHelper.createHeadersDocument (aMetadata);

        final String sStorageRoot = _getServletContext ().getRealPath ("/");
        new LimeStorage (sStorageRoot).saveDocument (sStorageChannelID, sMessageID, aMetadataDocument, aDocument);
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Failed to handle incoming LIME document", ex);
      throw new RecipientUnreachableException ("Failed to handle incoming LIME document", ex);
    }
  }

  @Nullable
  private static EndpointType _getEndpoint (@Nonnull final IParticipantIdentifier aRecipientId,
                                            @Nonnull final IDocumentTypeIdentifier aDocumentID,
                                            @Nonnull final IProcessIdentifier aProcessID,
                                            @Nonnull final ISMLInfo aSMLInfo)
  {
    EndpointType ret = null;
    try
    {
      ret = new SMPClientReadOnly (aRecipientId, aSMLInfo).getEndpoint (aRecipientId,
                                                                        aDocumentID,
                                                                        aProcessID,
                                                                        ESMPTransportProfile.TRANSPORT_PROFILE_AS2);
      if (ret == null)
        s_aLogger.error ("Failed to resolve AP endpoint url for recipient " +
                         aRecipientId +
                         ", document type " +
                         aDocumentID +
                         " and process " +
                         aProcessID);
    }
    catch (final SMPClientException ex)
    {
      s_aLogger.error ("Failed to resolve AP endpoint url for recipient " +
                       aRecipientId +
                       ", document type " +
                       aDocumentID +
                       " and process " +
                       aProcessID,
                       ex);
    }
    return ret;
  }
}
