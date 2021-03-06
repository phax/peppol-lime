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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.state.EChange;
import com.helger.peppol.lime.api.IMessageMetadata;

/**
 * Memory backed storage of all known objects. It is filled on WS create and
 * retrieved on WS put.
 *
 * @author Ravnholt
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@ThreadSafe
final class MessageMetadataRAMStore
{
  private static final ReadWriteLock s_aRWLock = new ReentrantReadWriteLock ();
  private static final ICommonsMap <String, IMessageMetadata> s_aMap = new CommonsHashMap <> ();

  /** Avoid instantiation */
  private MessageMetadataRAMStore ()
  {}

  public static boolean isStored (@Nonnull @Nonempty final String sMessageID)
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_aMap.containsKey (sMessageID);
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  public static EChange createResource (@Nonnull @Nonempty final String sMessageID,
                                        @Nonnull final IMessageMetadata aMetadata)
  {
    ValueEnforcer.notNull (aMetadata, "Metadata");

    s_aRWLock.writeLock ().lock ();
    try
    {
      if (s_aMap.containsKey (sMessageID))
        return EChange.UNCHANGED;
      s_aMap.put (sMessageID, aMetadata);
      return EChange.CHANGED;
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  @Nullable
  public static IMessageMetadata getMessage (@Nonnull @Nonempty final String sMessageID)
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_aMap.get (sMessageID);
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  public static void removeMessage (@Nonnull @Nonempty final String sMessageID)
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      s_aMap.remove (sMessageID);
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }
}
