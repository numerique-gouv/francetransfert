package fr.gouv.culture.francetransfert.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenEnclosureDataDownload {

    private String recipient;
    private String token;
    private String enclosure;
}
