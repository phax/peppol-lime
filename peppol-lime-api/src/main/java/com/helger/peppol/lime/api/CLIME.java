package com.helger.peppol.lime.api;

import javax.annotation.concurrent.Immutable;

import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;

@Immutable
public final class CLIME
{
  private CLIME ()
  {}

  public static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
}
