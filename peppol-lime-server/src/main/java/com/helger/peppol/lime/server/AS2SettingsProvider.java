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

import java.io.File;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.peppol.lime.api.IMessageMetadata;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;

/**
 * Helper class to provide the {@link AS2ClientSettings} to be used.
 *
 * @author Philip Helger
 */
@Immutable
final class AS2SettingsProvider
{
  private AS2SettingsProvider ()
  {}

  /**
   * @param aCert
   *        Source certificate. May not be <code>null</code>.
   * @return The common name of the certificate subject
   * @throws CertificateEncodingException
   */
  @Nonnull
  private static String _getCN (@Nonnull final X509Certificate aCert) throws CertificateEncodingException
  {
    final X500Name x500name = new JcaX509CertificateHolder (aCert).getSubject ();
    final RDN cn = x500name.getRDNs (BCStyle.CN)[0];
    return IETFUtils.valueToString (cn.getFirst ().getValue ());
  }

  /**
   * Build the AS2 client settings to be used.
   *
   * @param aRecipientEndpoint
   *        The endpoint of the recipient as determined by an SMP lookup
   *        previously
   * @param aMetadata
   *        The metadata of the document to be transmitted
   * @return The {@link AS2ClientSettings} to be used and never
   *         <code>null</code>.
   * @throws SMPClientException
   * @throws CertificateException
   */
  @Nonnull
  public static AS2ClientSettings createAS2ClientSettings (@Nonnull final EndpointType aRecipientEndpoint,
                                                           @Nonnull final IMessageMetadata aMetadata) throws SMPClientException,
                                                                                                      CertificateException
  {
    // Extract from SMP response
    final X509Certificate aReceiverCertificate = SMPClientReadOnly.getEndpointCertificate (aRecipientEndpoint);
    final String sReceiverID = _getCN (aReceiverCertificate);
    final String sReceiverKeyAlias = sReceiverID;

    // Start client configuration
    final AS2ClientSettings aSettings = new AS2ClientSettings ();
    aSettings.setKeyStore (new File (LimeServerConfiguration.getAS2KeystorePath ()),
                           LimeServerConfiguration.getAS2KeystorePassword ());

    // Fixed sender
    aSettings.setSenderData (LimeServerConfiguration.getAS2SenderID (),
                             LimeServerConfiguration.getAS2SenderEmail (),
                             LimeServerConfiguration.getAS2SenderKeyAlias ());

    // Dynamic receiver
    aSettings.setReceiverData (sReceiverID,
                               sReceiverKeyAlias,
                               W3CEndpointReferenceHelper.getAddress (aRecipientEndpoint.getEndpointReference ()));
    aSettings.setReceiverCertificate (aReceiverCertificate);

    // AS2 stuff - no need to change anything in this block
    aSettings.setPartnershipName (aSettings.getSenderAS2ID () + "_" + aSettings.getReceiverAS2ID ());
    final ECryptoAlgorithmSign eSignAlgo = LimeServerConfiguration.getAS2SignAlgorithm ();
    aSettings.setMDNOptions (new DispositionOptions ().setMICAlg (eSignAlgo)
                                                      .setMICAlgImportance (DispositionOptions.IMPORTANCE_REQUIRED)
                                                      .setProtocol (DispositionOptions.PROTOCOL_PKCS7_SIGNATURE)
                                                      .setProtocolImportance (DispositionOptions.IMPORTANCE_REQUIRED));
    aSettings.setEncryptAndSign (null, eSignAlgo);
    aSettings.setMessageIDFormat ("OpenPEPPOL-LIME-$date.ddMMyyyyHHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$");
    return aSettings;
  }
}
