package fr.gouv.culture.francetransfert.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GlimpsResultResponse {

	private boolean done;
	@JsonProperty("is_malware")
	private boolean isMalware;
	private boolean status;
	private String error;
	@JsonProperty("error_code")
	private String errorCode;
	private List<String> filenames;

}
