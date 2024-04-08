/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.gouv.culture.francetransfert.core.exception.StatException;

@Getter
@AllArgsConstructor
public enum SizeIndexEnum {
    LIGHT_SIZE("light_size", 20971520L),              //lignt size => 20Mo = 20 * 1024 * 1024
    AVERAGE_SIZE("average_size", 1073741824L),        //average_size => 1Go = 1 * 1024 * 1024 * 1024
    HEAVY_SIZE("heavy_size", 5368709120L),            //average_size => 5Go = 5 * 1024 * 1024 * 1024
    VERY_HEAVY_SIZE("very_heavy_size", 53687091200L); //average_size => 50Go = 50 * 1024 * 1024 * 1024



    private String key;
    private long value;

    public static List<String> keys() {
        return Stream.of(SizeIndexEnum.values())
                .map(e -> e.key)
                .collect(Collectors.toList());
    }

    public static String getSizeIndex(long enclosureSize) throws StatException {
        String sizeIndexEnclosure = "";
        if (enclosureSize > 0) {
            if (LIGHT_SIZE.getValue() > enclosureSize) {
                sizeIndexEnclosure = LIGHT_SIZE.getKey();
            } else if (AVERAGE_SIZE.getValue() > enclosureSize) {
                sizeIndexEnclosure = AVERAGE_SIZE.getKey();
            } else if (HEAVY_SIZE.getValue() > enclosureSize) {
                sizeIndexEnclosure = HEAVY_SIZE.getKey();
            } else if (VERY_HEAVY_SIZE.getValue() > enclosureSize) {
                sizeIndexEnclosure = VERY_HEAVY_SIZE.getKey();
            }
        }
        return sizeIndexEnclosure;
    }
}
