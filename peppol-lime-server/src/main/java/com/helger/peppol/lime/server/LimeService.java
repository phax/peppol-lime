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
package com.helger.peppol.lime.server;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.system.ENewLineMode;
import com.helger.jaxb.JAXBContextCache;
import com.helger.peppol.as2client.AS2ClientBuilder;
import com.helger.peppol.as2client.AS2ClientHelper;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
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
import com.helger.peppol.lime.server.storage.MessagePageListCreator;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.url.PeppolURLProvider;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.xml.XMLFactory;
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
  /** Safely determined value "limeService" */
  private static final String SERVICENAME = LimeService.class.getAnnotation (WebService.class).serviceName ();
  private static final QName QNAME_PAGEIDENTIFIER = new QName (CLimeIdentifiers.NAMESPACE_LIME,
                                                               CLimeIdentifiers.PAGEIDENTIFIER);
  private static final Logger s_aLogger = LoggerFactory.getLogger (LimeService.class);

  private static final ObjectFactory s_aObjFactory = new ObjectFactory ();

  @Resource
  private WebServiceContext m_aWebServiceContext;

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
  private String _getThisServiceURL ()
  {
    // First try from configuration file
    final String sServiceURL = LimeServerConfiguration.getServiceURL ();
    if (StringHelper.hasText (sServiceURL))
      return sServiceURL;

    // fallback to build manually
    final HttpServletRequest aServletRequest = _getServletRequest ();
    return aServletRequest.getScheme () +
           "://" +
           aServletRequest.getServerName () +
           ":" +
           aServletRequest.getLocalPort () +
           aServletRequest.getContextPath () +
           '/' +
           SERVICENAME;
  }

  @Nonnull
  private LimeStorage _createLimeStorage ()
  {
    // Get value from configuration file
    String sStorePath = LimeServerConfiguration.getStoragePath ();
    if (sStorePath == null)
    {
      // Default to servlet context
      final ServletContext aSC = (ServletContext) m_aWebServiceContext.getMessageContext ()
                                                                      .get (MessageContext.SERVLET_CONTEXT);
      sStorePath = aSC.getRealPath ("/");
    }
    return new LimeStorage (sStorePath);
  }

  @Nonnull
  private static SOAPFaultException _createSoapFault (final String sFaultMessage,
                                                      final Exception e) throws RuntimeException
  {
    try
    {
      s_aLogger.info ("LIME Server error", e);
      final SOAPFault soapFault = SOAPFactory.newInstance ().createFault ();
      soapFault.setFaultString (sFaultMessage);
      soapFault.setFaultCode (new QName (SOAPConstants.URI_NS_SOAP_ENVELOPE, "Sender"));
      soapFault.setFaultActor ("LIME AP");
      return new SOAPFaultException (soapFault);
    }
    catch (final SOAPException e2)
    {
      throw new RuntimeException ("Problem processing SOAP Fault on service-side", e2);
    }
  }

  @Nonnull
  public static W3CEndpointReference createW3CEndpointReference (@Nonnull final String sURL,
                                                                 @Nonnull final String sChannelID,
                                                                 @Nonnull final String sMessageID)
  {
    final List <Element> aReferenceParameters = new ArrayList <> ();

    // Channel ID
    final Document aDummyDoc = XMLFactory.newDocument ();
    Element aElement = aDummyDoc.createElementNS (CTransportIdentifiers.NAMESPACE_TRANSPORT_IDS,
                                                  CLimeIdentifiers.CHANNELID);
    aElement.appendChild (aDummyDoc.createTextNode (sChannelID));
    aReferenceParameters.add (aElement);

    // Message ID
    aElement = aDummyDoc.createElementNS (CTransportIdentifiers.NAMESPACE_TRANSPORT_IDS, CLimeIdentifiers.MESSAGEID);
    aElement.appendChild (aDummyDoc.createTextNode (sMessageID));
    aReferenceParameters.add (aElement);

    return W3CEndpointReferenceHelper.createEndpointReference (sURL, aReferenceParameters);
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
    final String sThisServiceURL = _getThisServiceURL ();

    IMessageMetadata aMetadata = null;
    try
    {
      // Grabs the list of headers from the SOAP message
      final HeaderList aHeaderList = _getInboundHeaderList ();
      aMetadata = MessageMetadataHelper.createMetadataFromHeadersWithCustomMessageID (aHeaderList, sMessageID);

      if (MessageMetadataRAMStore.createResource (sMessageID, aMetadata).isUnchanged ())
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

    // Create response
    final CreateResponse ret = new CreateResponse ();
    final ResourceCreated aResourceCreated = new ResourceCreated ();
    final W3CEndpointReference w3CEndpointReference = createW3CEndpointReference (sThisServiceURL,
                                                                                  aMetadata.getChannelID (),
                                                                                  sMessageID);
    aResourceCreated.getEndpointReference ().add (w3CEndpointReference);
    ret.setResourceCreated (aResourceCreated);
    return ret;
  }

  @Nullable
  private static EndpointType _getEndpoint (@Nonnull final IParticipantIdentifier aRecipientId,
                                            @Nonnull final IDocumentTypeIdentifier aDocumentID,
                                            @Nonnull final IProcessIdentifier aProcessID,
                                            @Nonnull final ISMLInfo aSMLInfo,
                                            @Nonnull final ISMPTransportProfile aTransportProfile)
  {
    EndpointType ret = null;
    try
    {
      ret = new SMPClientReadOnly (PeppolURLProvider.INSTANCE, aRecipientId, aSMLInfo).getEndpoint (aRecipientId,
                                                                                                    aDocumentID,
                                                                                                    aProcessID,
                                                                                                    aTransportProfile);
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

  private static void _logPutRequest (@Nullable final String sAction,
                                      @Nullable final String sSenderUrl,
                                      @Nonnull final IMessageMetadata aMetadata,
                                      @Nullable final String sReceiverID)
  {
    final String sNewLine = ENewLineMode.DEFAULT.getText ();
    final String s = "REQUEST start--------------------------------------------------" +
                     sNewLine +
                     "Action: " +
                     sAction +
                     sNewLine +
                     "Sender URL: " +
                     sSenderUrl +
                     sNewLine +
                     "Sending to : " +
                     sReceiverID +
                     sNewLine +
                     "Messsage ID: " +
                     aMetadata.getMessageID () +
                     sNewLine +
                     "Sender ID: " +
                     aMetadata.getSenderID ().getURIEncoded () +
                     sNewLine +
                     "Recipient ID: " +
                     aMetadata.getRecipientID ().getURIEncoded () +
                     sNewLine +
                     "Document ID: " +
                     aMetadata.getDocumentTypeID ().getURIEncoded () +
                     sNewLine +
                     "Process ID: " +
                     aMetadata.getProcessID ().getURIEncoded () +
                     sNewLine +
                     "REQUEST end----------------------------------------------------" +
                     sNewLine;
    s_aLogger.info (s);
  }

  private void _sendMessageUndeliverable (@Nonnull final Exception ex,
                                          @Nullable final String sMessageID,
                                          @Nonnull final ReasonCodeType eReasonCode,
                                          @Nonnull final IMessageMetadata aMetadata)
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

  private static void _sendToAccessPointViaAS2 (@Nonnull final Put aBody,
                                                @Nonnull final EndpointType aRecipientEndpoint,
                                                @Nonnull final IMessageMetadata aMetadata) throws Exception
  {
    final Element aSourceNode = (Element) aBody.getAnyAtIndex (0);
    final X509Certificate aReceiverCertificate = SMPClientReadOnly.getEndpointCertificate (aRecipientEndpoint);

    final File aKeyStoreFile = new File (LimeServerConfiguration.getAS2KeystorePath ());
    // TODO remove check when using as2-peppol-client > 1.0.2
    if (!aKeyStoreFile.canWrite ())
      s_aLogger.error ("The PKCS12 key store file '" +
                       aKeyStoreFile.getAbsolutePath () +
                       "' is not writable. This will result in a weird behaviour!");

    final String sReceiverURL = W3CEndpointReferenceHelper.getAddress (aRecipientEndpoint.getEndpointReference ());
    final AS2ClientResponse aResponse = new AS2ClientBuilder ().setPeppolSenderID (aMetadata.getSenderID ())
                                                               .setPeppolReceiverID (aMetadata.getRecipientID ())
                                                               .setPeppolDocumentTypeID (aMetadata.getDocumentTypeID ())
                                                               .setPeppolProcessID (aMetadata.getProcessID ())
                                                               .setBusinessDocument (aSourceNode)
                                                               .setPKCS12KeyStore (aKeyStoreFile,
                                                                                   LimeServerConfiguration.getAS2KeystorePassword ())
                                                               .setSenderAS2ID (LimeServerConfiguration.getAS2SenderID ())
                                                               .setSenderAS2Email (LimeServerConfiguration.getAS2SenderEmail ())
                                                               .setSenderAS2KeyAlias (LimeServerConfiguration.getAS2SenderKeyAlias ())
                                                               .setReceiverAS2ID (AS2ClientHelper.getSubjectCommonName (aReceiverCertificate))
                                                               .setReceiverAS2KeyAlias (AS2ClientHelper.getSubjectCommonName (aReceiverCertificate))
                                                               .setReceiverAS2Url (sReceiverURL)
                                                               .setReceiverCertificate (aReceiverCertificate)
                                                               .setAS2SigningAlgorithm (LimeServerConfiguration.getAS2SignAlgorithm ())
                                                               .sendSynchronous ();
    if (aResponse.hasException ())
      s_aLogger.error ("Error sending to " + sReceiverURL + ": " + aResponse.getAsString ());
    else
      s_aLogger.info ("Successfully forwarded message to " + sReceiverURL);
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

        _createLimeStorage ().saveDocument (sStorageChannelID, sMessageID, aMetadataDocument, aDocument);
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Failed to handle incoming LIME document", ex);
      throw new RecipientUnreachableException ("Failed to handle incoming LIME document", ex);
    }
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
    final IMessageMetadata aMetadata = MessageMetadataRAMStore.getMessage (sMessageID);

    if (aMetadata == null)
      throw _createSoapFault (FAULT_SERVER_ERROR,
                              new IllegalStateException ("No such message ID found: " + sMessageID));

    try
    {
      // Default to AS2 here
      final ISMLInfo aSML = LimeServerConfiguration.getSML ();
      final ISMPTransportProfile aTransportProfile = ESMPTransportProfile.TRANSPORT_PROFILE_AS2;

      final EndpointType aSenderEndpoint = _getEndpoint (aMetadata.getSenderID (),
                                                         aMetadata.getDocumentTypeID (),
                                                         aMetadata.getProcessID (),
                                                         aSML,
                                                         aTransportProfile);
      if (aSenderEndpoint == null)
        throw new IllegalStateException ("Failed to resolve sender endpoint URL for " + aMetadata.toString ());

      final EndpointType aRecipientEndpoint = _getEndpoint (aMetadata.getRecipientID (),
                                                            aMetadata.getDocumentTypeID (),
                                                            aMetadata.getProcessID (),
                                                            aSML,
                                                            aTransportProfile);
      if (aRecipientEndpoint == null)
        throw new IllegalStateException ("Failed to resolve recipient endpoint URL for " + aMetadata.toString ());

      final String sSenderURL = W3CEndpointReferenceHelper.getAddress (aSenderEndpoint.getEndpointReference ());
      final String sRecipientURL = W3CEndpointReferenceHelper.getAddress (aRecipientEndpoint.getEndpointReference ());
      if (EqualsHelper.equalsIgnoreCase (sSenderURL, sRecipientURL))
      {
        _logPutRequest ("This is a local request - sending directly to inbox",
                        sSenderURL,
                        aMetadata,
                        "INBOX: " + aMetadata.getRecipientID ().getValue ());
        _sendToInbox (aMetadata, aBody);
      }
      else
      {
        _logPutRequest ("This is a request for a remote access point", sSenderURL, aMetadata, sRecipientURL);
        _sendToAccessPointViaAS2 (aBody, aRecipientEndpoint, aMetadata);
      }
      // On success, remove the metadata
      MessageMetadataRAMStore.removeMessage (sMessageID);
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
      if (StringHelper.hasNoText (sMessageID))
      {
        // Add page list to response
        final String sThisServiceURL = _getThisServiceURL ();
        final int nPageNumber = StringParser.parseInt (StringHelper.trim (sPageIdentifier), 0);
        final Document aDocument = MessagePageListCreator.getPageList (nPageNumber,
                                                                       sThisServiceURL,
                                                                       _createLimeStorage (),
                                                                       sChannelID);
        if (aDocument != null)
          aGetResponse.getAny ().add (aDocument.getDocumentElement ());
      }
      else
      {
        // add single message to response
        final LimeStorage aStorage = _createLimeStorage ();
        final Document aDocumentMetadata = aStorage.getDocumentMetadata (sChannelID, sMessageID);
        final Document aDocument = aStorage.getDocument (sChannelID, sMessageID);
        aGetResponse.getAny ().add (aDocumentMetadata.getDocumentElement ());
        aGetResponse.getAny ().add (aDocument.getDocumentElement ());
      }
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
  @Nonnull
  public DeleteResponse delete (final Delete body)
  {
    final HeaderList aHeaderList = _getInboundHeaderList ();
    final String sChannelID = MessageMetadataHelper.getChannelID (aHeaderList);
    final String sMessageID = MessageMetadataHelper.getMessageID (aHeaderList);
    try
    {
      _createLimeStorage ().deleteDocument (sChannelID, sMessageID);
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error deleting document", ex);
    }
    return new DeleteResponse ();
  }
}
