/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.satisfaction;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.model.RateRepresentation;
import fr.gouv.culture.francetransfert.security.WorkerException;

@Service
public class SatisfactionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SatisfactionService.class);

	private static final String[] HEADER = { "ID_PLIS", "DATE", "COMMENTAIRE", "NOTE", "TYPE_SATISFACTION", "DOMAINE" };

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	public boolean saveData(RateRepresentation rate) throws WorkerException {
		try {
			String hostname = InetAddress.getLocalHost().getHostName().split("\\.")[0];
			LOGGER.debug("Hostname: " + hostname);
			// // ip-10-50-11-193_2022-01-18_FranceTransfert_upload_satisfaction.csv
			String fileName = hostname + "_FranceTransfert_" + LocalDate.parse(rate.getDate()).format(DateTimeFormatter.ISO_LOCAL_DATE)
					+ "_" + rate.getType().getValue() + ".csv";
			Path filePath = Path.of(System.getProperty("java.io.tmpdir"), fileName);
			StringBuilder sb = new StringBuilder();
			CSVFormat option = CSVFormat.DEFAULT.builder().setQuoteMode(QuoteMode.ALL).setHeader(HEADER).setSkipHeaderRecord(Files.exists(filePath)).build();
			CSVPrinter csvPrinter = new CSVPrinter(sb, option);
			csvPrinter.printRecord(rate.getPlis(), LocalDate.parse(rate.getDate()).format(DateTimeFormatter.ISO_LOCAL_DATE),
					rate.getMessage().replaceAll("\\s", " "), rate.getSatisfaction(), rate.getType().getValue(),
					rate.getDomain().toLowerCase());
			csvPrinter.flush();
			csvPrinter.close();
			Files.writeString(filePath, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			return true;
		} catch (Exception e) {
			LOGGER.error("error save data in CsvFile", e);
			throw new WorkerException("error save data in CsvFile");
		}
	}
}
