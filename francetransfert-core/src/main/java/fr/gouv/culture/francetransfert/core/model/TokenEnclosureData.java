package fr.gouv.culture.francetransfert.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenEnclosureData {

    private String senderMail;
    private String token;
    private String enclosure;
}
