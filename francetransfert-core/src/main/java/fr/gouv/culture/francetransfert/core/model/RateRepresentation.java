/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.model;

import org.apache.commons.text.StringEscapeUtils;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class RateRepresentation extends StatModel {

    @Min(0)
    @Max(3)
    private int satisfaction;

    @Size(max = 2500)
    private String message;

    public int getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(int satisfaction) {
        this.satisfaction = satisfaction;
    }

    public String getMessage() {
        return StringEscapeUtils.escapeHtml4(message);
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
