/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IgnimissionDomainDataParameter {

    private String asam_product_filter;
    private String asam_autorise;
    private String asam_date;


    @Override
    public String toString() {
        return "{\"asam_product_filter\" : \""+asam_product_filter+"\",\"asam_autorise\" : \""+asam_autorise+"\", \"asam_date\" : \""+asam_date+"\"}";
    }
}
