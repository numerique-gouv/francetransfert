/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.clamav;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.exception.ClamAVClientNotAvailableException;
import fr.gouv.culture.francetransfert.exception.ClamAVException;
import fr.gouv.culture.francetransfert.model.enums.ErrorEnum;
import jakarta.annotation.PostConstruct;

@Service
public class ClamAVScannerManager {

	private static Logger LOGGER = LoggerFactory.getLogger(ClamAVScannerManager.class);

	@Value("${scan.clamav.host}")
	private String host;

	@Value("${scan.clamav.port}")
	private Integer port;

	@Value("${scan.clamav.timeout}")
	private int timeout;

	@Value("${scan.clamav.chunksize}")
	private int chunkSize;

	private InetSocketAddress address;

	protected final byte[] INSTREAM = "zINSTREAM\0".getBytes();
	protected final Pattern FOUND = Pattern.compile("^stream: (.+) FOUND$");
	protected final String OK = "stream: OK";
	protected final byte[] PING = "zPING\0".getBytes();
	protected final String PONG = "PONG";

	@PostConstruct
	public void init() {
		this.address = new InetSocketAddress(host, port);
		setAddress(this.address);
	}

	/**
	 * Method scans files to check whether file is virus infected
	 *
	 * @param fileChannel
	 * @return
	 * @throws IOException
	 * @throws ClamAVException
	 */
	public String performScan(FileChannel fileChannel) throws IOException, ClamAVException {
		LOGGER.debug("STEP VIRUS SCAN");
		return scan(fileChannel, getAddress(), timeout);
	}

	/**
	 * Method scans files to check whether file is virus infected
	 *
	 * @param inputStream
	 * @return
	 * @throws IOException
	 * @throws ClamAVException
	 */
	public String performScan(InputStream inputStream) throws IOException, ClamAVException {
		LOGGER.debug("STEP VIRUS SCAN");
		return scan(inputStream, getAddress(), timeout);
	}

	/**
	 * @return
	 */
	public boolean ping() {
		return ping(this.address, timeout);
	}

	/**
	 * @param address
	 * @param timeout
	 * @return
	 */
	private boolean ping(InetSocketAddress address, int timeout) throws ClamAVClientNotAvailableException {
		try (SocketChannel socketChannel = SocketChannel.open(address)) {
			socketChannel.write(ByteBuffer.wrap(PING));

			socketChannel.socket().setSoTimeout(timeout);

			ByteBuffer data = ByteBuffer.allocate(1024);
			socketChannel.read(data);
			String status = new String(data.array());
			status = status.substring(0, status.indexOf(0));
			LOGGER.info(" Clamav server ping response status : {}", status);
			if (PONG.equals(status)) {
				return true;
			}
		} catch (IOException ex) {
			throw new ClamAVClientNotAvailableException(ErrorEnum.PING_ERROR.getValue(), ex);
		}
		return false;
	}

	/**
	 * @param fileChannel
	 * @param address
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @throws ClamAVException
	 */
	private String scan(FileChannel fileChannel, InetSocketAddress address, int timeout)
			throws IOException, ClamAVException {
		try (SocketChannel socketChannel = SocketChannel.open(address)) {
			socketChannel.write(ByteBuffer.wrap(INSTREAM));
			ByteBuffer size = ByteBuffer.allocate(4);
			size.clear();
			size.putInt((int) fileChannel.size()).flip();
			socketChannel.write(size);
			fileChannel.transferTo(0, (int) fileChannel.size(), socketChannel);
			size.clear();
			size.putInt(0).flip();
			socketChannel.write(size);

			return scanResult(socketChannel, timeout);
		}
	}

	/**
	 * Method scans files to check whether file is virus infected
	 *
	 * @param inputStream
	 * @param address
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @throws ClamAVException
	 */
	private String scan(InputStream inputStream, InetSocketAddress address, int timeout)
			throws IOException, ClamAVException {

		try (SocketChannel socketChannel = SocketChannel.open(address)) {
			socketChannel.write(ByteBuffer.wrap(INSTREAM));
			ByteBuffer size = ByteBuffer.allocate(4);
			byte[] b = new byte[chunkSize];
			int chunk = chunkSize;
			while (chunk == chunkSize) {
				chunk = inputStream.read(b);
				if (chunk > 0) {
					size.clear();
					size.putInt(chunk).flip();
					socketChannel.write(size);
					socketChannel.write(ByteBuffer.wrap(b, 0, chunk));
				}
			}
			size.clear();
			size.putInt(0).flip();
			socketChannel.write(size);

			return scanResult(socketChannel, timeout);
		}
	}

	/**
	 * @param socketChannel
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @throws ClamAVException
	 */
	private String scanResult(SocketChannel socketChannel, int timeout) throws IOException, ClamAVException {
		socketChannel.socket().setSoTimeout(timeout);
		ByteBuffer data = ByteBuffer.allocate(1024);
		socketChannel.read(data);
		String status = new String(data.array());
		status = status.substring(0, status.indexOf(0));
		Matcher matcher = FOUND.matcher(status);
		if (matcher.matches()) {
			return matcher.group(1);
		} else if (OK.equals(status)) {
			return "OK";
		}
		throw new ClamAVException(ErrorEnum.SCAN_ERROR.getValue() + " : " + status);
	}

	/**
	 * @return
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * @param address
	 */
	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

}
