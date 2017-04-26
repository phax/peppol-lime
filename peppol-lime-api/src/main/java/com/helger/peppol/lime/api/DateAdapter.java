package com.helger.peppol.lime.api;

import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.datetime.util.PDTWebDateHelper;

/**
 * This class is used for converting between XML time elements and Java Date
 * objects. It is internally used in the JAXB bindings.
 */
@Immutable
public final class DateAdapter
{
  private DateAdapter ()
  {}

  @Nullable
  public static LocalDateTime getLocalDateTimeFromXSD (@Nullable final String sValue)
  {
    return PDTWebDateHelper.getLocalDateTimeFromXSD (sValue);
  }

  @Nullable
  public static String getAsStringXSD (@Nullable final LocalDateTime aLocalDateTime)
  {
    return aLocalDateTime == null ? null : PDTWebDateHelper.getAsStringXSD (aLocalDateTime);
  }
}
