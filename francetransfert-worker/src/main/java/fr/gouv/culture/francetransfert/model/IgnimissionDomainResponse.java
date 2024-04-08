/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class IgnimissionDomainResponse {

    @JsonProperty("export_id")
    private String exportId;
    @JsonProperty("table_data")
    private TableData tableData;


    public int getNbItems() {
        return (Objects.nonNull(this.tableData)) ? this.tableData.getCount() : 0;
    }

    public String getDomains() {
        if (Objects.nonNull(this.tableData) && !CollectionUtils.isEmpty(this.tableData.getRows())) {
            return this.tableData.getRows()
                    .stream()
                    .filter(domain -> Objects.nonNull(domain))
                    .map(IgnimissionDomainResponse.Row::getExtension)
                    .collect(Collectors.joining(" "));
        }
        return null;
    }

    public List<String> getDomainsAsList() {
        if (Objects.nonNull(this.tableData) && !CollectionUtils.isEmpty(this.tableData.getRows())) {
            return this.tableData.getRows()
                    .stream()
                    .filter(domain -> Objects.nonNull(domain))
                    .map(IgnimissionDomainResponse.Row::getExtension)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TableData {
        @JsonProperty("count")
        private int count;

        @JsonProperty("rows")
        private List<Row> rows;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    static class Row {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String extension;
    }
}
