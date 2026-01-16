/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.utils;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public class SanitizerUtil {
  private static final PolicyFactory POLICY = Sanitizers.FORMATTING;

  public static String sanitize(String htmlContent) {
    return POLICY.sanitize(htmlContent);
  }

}