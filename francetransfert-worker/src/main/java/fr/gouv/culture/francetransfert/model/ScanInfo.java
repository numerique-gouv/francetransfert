package fr.gouv.culture.francetransfert.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8460116025076636449L;

	private String uuid;
	private String filename;
	private String errorCode;
	private boolean virus;
	private boolean error;
	private boolean fatalError;

}
