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
package com.helger.peppol.lime.server.storage;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.dom.DOMResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.jaxb.JAXBContextCache;
import com.helger.jaxb.JAXBMarshallerHelper;
import com.helger.peppol.lime.api.CTransportIdentifiers;
import com.helger.peppol.lime.api.Entry;
import com.helger.peppol.lime.api.NextPageIdentifierType;
import com.helger.peppol.lime.api.ObjectFactory;
import com.helger.peppol.lime.api.PageListType;
import com.helger.peppol.lime.client.CLimeIdentifiers;
import com.helger.peppol.lime.server.LimeService;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.xml.XMLFactory;
import com.helger.xml.serialize.write.XMLWriter;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * @author Ravnholt
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class MessagePageListCreator
{
  public static final int MESSAGE_PAGE_SIZE = 100;

  private static final Logger s_aLogger = LoggerFactory.getLogger (MessagePageListCreator.class);
  private static final ObjectFactory s_aObjFactory = new ObjectFactory ();

  private MessagePageListCreator ()
  {}

  private static void _addPageListEntries (@Nonnegative final int nFromIndex,
                                           @Nonnegative final int nToIndex,
                                           @Nonnull final String [] aMessageIDs,
                                           @Nonnull final LimeStorage aStorage,
                                           @Nonnull final String sChannelID,
                                           @Nonnull final String sEndpoint,
                                           @Nonnull final PageListType aPageList)
  {
    aPageList.setEntryList (s_aObjFactory.createEntryListType ());

    for (int i = nFromIndex; i <= nToIndex; i++)
    {
      final String sMessageID = aMessageIDs[i];

      final Entry aEntry = s_aObjFactory.createEntry ();
      aEntry.setSize (Long.valueOf (aStorage.getSize (sChannelID, sMessageID)));
      aEntry.setCreationTime (aStorage.getCreationTime (sChannelID, sMessageID));
      aEntry.setEndpointReference (LimeService.createW3CEndpointReference (sEndpoint, sChannelID, sMessageID));
      aPageList.getEntryList ().addEntry (aEntry);
    }
  }

  private static void _addNextPageIdentifier (final String sEndpointURL,
                                              final int nCurPageNum,
                                              final String sChannelID,
                                              final PageListType aPageList)
  {
    final Document aDummyDoc = XMLFactory.newDocument ();
    final List <Element> aReferenceParameters = new ArrayList <> ();

    // Page identifier
    Element aElement = aDummyDoc.createElementNS (CLimeIdentifiers.NAMESPACE_LIME, CLimeIdentifiers.PAGEIDENTIFIER);
    aElement.appendChild (aDummyDoc.createTextNode (Integer.toString (nCurPageNum + 1)));
    aReferenceParameters.add (aElement);

    // Channel ID
    aElement = aDummyDoc.createElementNS (CTransportIdentifiers.NAMESPACE_TRANSPORT_IDS, CLimeIdentifiers.CHANNELID);
    aElement.appendChild (aDummyDoc.createTextNode (sChannelID));
    aReferenceParameters.add (aElement);

    final NextPageIdentifierType aNextPageIdentifier = s_aObjFactory.createNextPageIdentifierType ();
    aNextPageIdentifier.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference (sEndpointURL,
                                                                                                  aReferenceParameters));
    aPageList.setNextPageIdentifier (aNextPageIdentifier);
  }

  @Nonnull
  private static Document _marshallPageList (final PageListType aPageList) throws JAXBException
  {
    final Marshaller aMarshaller = JAXBContextCache.getInstance ()
                                                   .getFromCache (PageListType.class)
                                                   .createMarshaller ();
    JAXBMarshallerHelper.setSunNamespacePrefixMapper (aMarshaller, new NamespacePrefixMapper ()
    {
      @Override
      public String getPreferredPrefix (final String namespaceUri, final String suggestion, final boolean requirePrefix)
      {
        if (CLimeIdentifiers.NAMESPACE_LIME.equalsIgnoreCase (namespaceUri))
          return "peppol";
        return suggestion;
      }
    });

    final Document ret = XMLFactory.newDocument ();
    aMarshaller.marshal (s_aObjFactory.createPageList (aPageList), new DOMResult (ret));
    s_aLogger.info (XMLWriter.getNodeAsString (ret));
    return ret;
  }

  @Nullable
  public static Document getPageList (@Nonnegative final int nPageNum,
                                      @Nonnull final String sEndpointURL,
                                      @Nonnull final LimeStorage aStorage,
                                      @Nonnull final String sChannelID) throws JAXBException
  {
    // Get all message IDs
    final String [] aMessageIDs = aStorage.getMessageIDs (sChannelID);
    final int nPageSize = MESSAGE_PAGE_SIZE;
    final int nMaxPageIndex = aMessageIDs.length / nPageSize;
    if (nPageNum < 0 || nPageNum > nMaxPageIndex)
      throw new IllegalArgumentException ("Page number must be between 0 and " + nMaxPageIndex + " but is " + nPageNum);

    s_aLogger.info ("Messages found in inbox: " + aMessageIDs.length);

    if (aMessageIDs.length <= 0 || (aMessageIDs.length / nPageSize) < nPageNum)
    {
      s_aLogger.info ("Page List not created. MessageIDs=" +
                      aMessageIDs.length +
                      " pageSize=" +
                      nPageSize +
                      " pageNum=" +
                      nPageNum);
      return null;
    }

    final int nFromIndex = nPageNum * nPageSize;
    final int nToIndex = Math.min (((nPageNum + 1) * nPageSize) - 1, aMessageIDs.length - 1);

    final PageListType aPageList = s_aObjFactory.createPageListType ();
    aPageList.setNumberOfEntries (Long.valueOf (nToIndex - nFromIndex + 1));
    _addPageListEntries (nFromIndex, nToIndex, aMessageIDs, aStorage, sChannelID, sEndpointURL, aPageList);
    if ((aMessageIDs.length / nPageSize) >= nPageNum + 1)
    {
      _addNextPageIdentifier (sEndpointURL, nPageNum, sChannelID, aPageList);
    }

    final Document ret = _marshallPageList (aPageList);
    s_aLogger.info ("Page List created. MessageIDs=" +
                    aMessageIDs.length +
                    " pageSize=" +
                    nPageSize +
                    " pageNum=" +
                    nPageNum);
    return ret;
  }
}
