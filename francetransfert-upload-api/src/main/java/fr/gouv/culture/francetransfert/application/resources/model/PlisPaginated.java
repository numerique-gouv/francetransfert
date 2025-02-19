package fr.gouv.culture.francetransfert.application.resources.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlisPaginated {
    List<FileInfoRepresentation> plis;
    int page;
    int pageSize;
    int totalItems;
    int totalPages;
}
