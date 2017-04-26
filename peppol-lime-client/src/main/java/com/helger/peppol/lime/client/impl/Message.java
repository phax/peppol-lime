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

import java.util.Date;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.lime.client.IMessage;

/**
 * @author Ravnholt
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class Message implements IMessage
{
  private final Date m_aCreatedTime;
  private final String m_sMessageID;
  private Document m_aDocument;
  private IParticipantIdentifier m_aSenderID;
  private IParticipantIdentifier m_aReceiverID;
  private IDocumentTypeIdentifier m_aDocumentTypeID;
  private IProcessIdentifier m_aProcessID;

  public Message ()
  {
    this (UUID.randomUUID ().toString ());
  }

  public Message (@Nonnull @Nonempty final String sMessageID)
  {
    m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
    m_aCreatedTime = new Date ();
  }

  @Nonnull
  public Date getCreatedTime ()
  {
    return m_aCreatedTime;
  }

  @Nonnull
  public String getMessageID ()
  {
    return m_sMessageID;
  }

  @Nullable
  public Document getDocument ()
  {
    return m_aDocument;
  }

  public void setDocument (@Nullable final Document aDocument)
  {
    m_aDocument = aDocument;
  }

  @Nullable
  public IParticipantIdentifier getSenderID ()
  {
    return m_aSenderID;
  }

  public void setSenderID (@Nullable final IParticipantIdentifier aSenderID)
  {
    m_aSenderID = aSenderID;
  }

  @Nullable
  public IParticipantIdentifier getReceiverID ()
  {
    return m_aReceiverID;
  }

  public void setReceiverID (@Nullable final IParticipantIdentifier aReceiverID)
  {
    m_aReceiverID = aReceiverID;
  }

  @Nullable
  public IDocumentTypeIdentifier getDocumentTypeID ()
  {
    return m_aDocumentTypeID;
  }

  public void setDocumentTypeID (@Nullable final IDocumentTypeIdentifier aDocumentTypeID)
  {
    m_aDocumentTypeID = aDocumentTypeID;
  }

  @Nullable
  public IProcessIdentifier getProcessID ()
  {
    return m_aProcessID;
  }

  public void setProcessID (@Nullable final IProcessIdentifier aProcessID)
  {
    m_aProcessID = aProcessID;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("CreatedTime", m_aCreatedTime)
                                       .append ("MessageID", m_sMessageID)
                                       .append ("Document", m_aDocument)
                                       .append ("SenderID", m_aSenderID)
                                       .append ("ReceiverID", m_aReceiverID)
                                       .append ("DocumentTypeID", m_aDocumentTypeID)
                                       .append ("ProcessID", m_aProcessID)
                                       .getToString ();
  }

}
