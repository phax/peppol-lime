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
package com.helger.peppol.lime.client.username;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * Mutable implementation of the {@link IUsernamePWCredentials}.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@NotThreadSafe
public final class UsernamePWCredentials implements IUsernamePWCredentials
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (UsernamePWCredentials.class);
  private final String m_sUsername;
  private final String m_sPassword;

  public UsernamePWCredentials (@Nonnull @Nonempty final String sUsername, @Nullable final String sPassword)
  {
    ValueEnforcer.notEmpty (sUsername, "Username");
    if (sUsername.indexOf (':') >= 0)
      s_aLogger.error ("The user name '" +
                       sUsername +
                       "' contains a ':' character which prevents it from being correctly converted to an HTTP basic authentication value!!!");
    m_sUsername = sUsername;
    m_sPassword = sPassword;
  }

  @Nonnull
  @Nonempty
  public String getUsername ()
  {
    return m_sUsername;
  }

  @Nullable
  public String getPassword ()
  {
    return m_sPassword;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final UsernamePWCredentials rhs = (UsernamePWCredentials) o;
    return EqualsHelper.equals (m_sUsername, rhs.m_sUsername) && EqualsHelper.equals (m_sPassword, rhs.m_sPassword);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sUsername).append (m_sPassword).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("username", m_sUsername).appendPassword ("password").getToString ();
  }
}
