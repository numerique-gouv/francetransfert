/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@PropertySource("classpath:extension.properties")
public class ExtensionProperties {

    @Value("#{'${extension.name}'.split(',')}")
    private List<String> extension;

    public List<String>  getExtensionValue(){
        return !CollectionUtils.isEmpty(extension) ? extension : new ArrayList<>();
    }
}
