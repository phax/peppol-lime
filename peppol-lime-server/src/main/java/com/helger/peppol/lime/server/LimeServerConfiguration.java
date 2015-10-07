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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.utils.ConfigFile;

/**
 * The central configuration for the SMP server. This class manages the content
 * of the "lime-server.properties" file. The order of the properties file
 * resolving is as follows:
 * <ol>
 * <li>Check for the value of the system property
 * <code>lime.server.properties.path</code></li>
 * <li>The filename <code>private-lime-server.properties</code> in the root of
 * the classpath</li>
 * <li>The filename <code>lime-server.properties</code> in the root of the
 * classpath</li>
 * </ol>
 *
 * @author Philip Helger
 */
@Immutable
public final class LimeServerConfiguration
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (LimeServerConfiguration.class);
  private static final ConfigFile s_aConfigFile;

  static
  {
    final List <String> aFilePaths = new ArrayList <> ();
    // Check if the system property is present
    final String sPropertyPath = SystemProperties.getPropertyValue ("lime.server.properties.path");
    if (StringHelper.hasText (sPropertyPath))
      aFilePaths.add (sPropertyPath);

    // Use the default paths
    aFilePaths.add ("private-lime-server.properties");
    aFilePaths.add ("lime-server.properties");

    s_aConfigFile = new ConfigFile (ArrayHelper.newArray (aFilePaths, String.class));
    if (s_aConfigFile.isRead ())
      s_aLogger.info ("Read lime-server.properties from " + s_aConfigFile.getReadResource ().getPath ());
    else
      s_aLogger.warn ("Failed to read lime-server.properties from any of the paths: " + aFilePaths);
  }

  private LimeServerConfiguration ()
  {}

  /**
   * @return The overall config file. Use this to read arbitrary settings. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    return s_aConfigFile;
  }

  /**
   * @return The ID of the SML to be used. Should correspond the ID of the
   *         entries in the {@link ESML} enum.
   */
  @Nullable
  public static String getSMLID ()
  {
    return s_aConfigFile.getString ("sml.id");
  }

  /**
   * @return The value of {@link #getSMLID()} resolved to an {@link ESML} value.
   *         If no value or an invalid value is provided, the fallback value
   *         {@link ESML#DIGIT_PRODUCTION} is returned.
   */
  @Nonnull
  public static ESML getSML ()
  {
    final String sSMLID = getSMLID ();
    return ESML.getFromIDOrDefault (sSMLID, ESML.DIGIT_PRODUCTION);
  }

  @Nullable
  public static String getAS2KeystorePath ()
  {
    return s_aConfigFile.getString ("as2.keystore.path");
  }

  @Nullable
  public static String getAS2KeystorePassword ()
  {
    return s_aConfigFile.getString ("as2.keystore.password");
  }

  @Nullable
  public static String getAS2SenderKeyAlias ()
  {
    return s_aConfigFile.getString ("as2.sender.keyalias");
  }

  @Nullable
  public static String getAS2SenderID ()
  {
    return s_aConfigFile.getString ("as2.sender.id");
  }

  @Nullable
  public static String getAS2SenderEmail ()
  {
    return s_aConfigFile.getString ("as2.sender.email");
  }

  /**
   * @return The signing algorithm to be used. Defaults to "sha1" if nothing is
   *         specified.
   */
  @Nonnull
  public static ECryptoAlgorithmSign getAS2SignAlgorithm ()
  {
    final String sAlgo = s_aConfigFile.getString ("as2.sign.algorithm");
    return ECryptoAlgorithmSign.getFromIDOrDefault (sAlgo, ECryptoAlgorithmSign.DIGEST_SHA1);
  }
}
