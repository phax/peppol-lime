package com.helger.peppol.lime.server;

import java.io.File;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.peppol.lime.api.IMessageMetadata;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.utils.ConfigFile;

final class AS2SettingsProvider
{
  private static final ConfigFile s_aLimeConfig = new ConfigFile ("private-lime-server.properties",
                                                                  "lime-server.properties");

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

  @Nonnull
  public static AS2ClientSettings createAS2ClientSettings (@Nonnull final String sAPEndpointAddress,
                                                           @Nonnull final IMessageMetadata aMetadata) throws SMPClientException,
                                                                                                      CertificateException
  {
    // Query SMP
    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aMetadata.getRecipientID (), ESML.DIGIT_PRODUCTION);
    final EndpointType aEndpoint = aSMPClient.getEndpoint (aMetadata.getRecipientID (),
                                                           aMetadata.getDocumentTypeID (),
                                                           aMetadata.getProcessID (),
                                                           ESMPTransportProfile.TRANSPORT_PROFILE_AS2);
    if (aEndpoint == null)
      throw new NullPointerException ("Failed to resolve endpoint for docType/process");

    // Extract from SMP response
    final X509Certificate aReceiverCertificate = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
    final String sReceiverID = _getCN (aReceiverCertificate);
    final String sReceiverKeyAlias = sReceiverID;

    // Start client configuration
    final AS2ClientSettings aSettings = new AS2ClientSettings ();
    aSettings.setKeyStore (new File (s_aLimeConfig.getString ("keystore.path")),
                           s_aLimeConfig.getString ("keystore.password"));

    // Fixed sender
    aSettings.setSenderData (s_aLimeConfig.getString ("sender.as2.id"),
                             s_aLimeConfig.getString ("sender.as2.email"),
                             s_aLimeConfig.getString ("sender.as2.keyalias"));

    // Dynamic receiver
    aSettings.setReceiverData (sReceiverID, sReceiverKeyAlias, sAPEndpointAddress);
    aSettings.setReceiverCertificate (aReceiverCertificate);

    // AS2 stuff - no need to change anything in this block
    aSettings.setPartnershipName (aSettings.getSenderAS2ID () + "_" + aSettings.getReceiverAS2ID ());
    aSettings.setMDNOptions (new DispositionOptions ().setMICAlg (ECryptoAlgorithmSign.DIGEST_SHA1)
                                                      .setMICAlgImportance (DispositionOptions.IMPORTANCE_REQUIRED)
                                                      .setProtocol (DispositionOptions.PROTOCOL_PKCS7_SIGNATURE)
                                                      .setProtocolImportance (DispositionOptions.IMPORTANCE_REQUIRED));
    aSettings.setEncryptAndSign (null, ECryptoAlgorithmSign.DIGEST_SHA1);
    aSettings.setMessageIDFormat ("OpenPEPPOL-$date.ddMMyyyyHHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$");
    return aSettings;
  }
}
