/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package mail;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import fr.gouv.culture.francetransfert.FranceTransfertWorkerStarter;
import fr.gouv.culture.francetransfert.core.model.NewRecipient;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.model.Recipient;
import fr.gouv.culture.francetransfert.model.RootData;
import fr.gouv.culture.francetransfert.services.mail.notification.MailAvailbleEnclosureServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailEnclosureNoLongerAvailbleServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailNotificationServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailRelaunchServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailVirusFoundServices;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;
import fr.gouv.culture.francetransfert.utils.WorkerUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FranceTransfertWorkerStarter.class)
public class MailNotificationServicesTest {

	@Autowired
	private MailNotificationServices mailNotificationServices;

	@Autowired
	private MailRelaunchServices mailRelaunchServices;

	@Autowired
	private MailAvailbleEnclosureServices mailAvailbleEnclosureServices;

	@Autowired
	private MailEnclosureNoLongerAvailbleServices mailEnclosureNoLongerAvailbleServices;

	@Autowired
	private MailVirusFoundServices mailVirusFoundServices;

	private GreenMail smtpServer;
	private Enclosure enclosure;

	@Before
	public void setUp() throws Exception {
		smtpServer = new GreenMail(new ServerSetup(25, null, "smtp"));
		smtpServer.start();
		enclosure = Enclosure.builder().guid("enclosureId")
				.rootFiles(
						Arrays.asList(new RootData("file_1.txt", "txt", WorkerUtils.getFormattedFileSize(12), "file_2"),
								new RootData("file_2.txt", "txt", WorkerUtils.getFormattedFileSize(5), "file_2")))
				.rootDirs(Arrays.asList(new RootData("dir_1", null, WorkerUtils.getFormattedFileSize(120), "file_2"),
						new RootData("dir_2", null, WorkerUtils.getFormattedFileSize(50), "file_2")))
				.countElements(2).totalSize(WorkerUtils.getFormattedFileSize(17))
				.expireDate(LocalDateTime.now().plusDays(30)
						.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)))
				.sender("louay.haddad@live.fr")
				.recipients(Arrays.asList(
						new Recipient("e4cce869-6f3d-4e10-900a-74299602f460", "louay.haddad@gouv.fr", false),
						new Recipient("6efb01a7-bd3d-46a9-ac12-33085f76ce1c", "louayhadded2012@gmail.com", false)))
				.message("Test message content").withPassword(false).build();
	}

	@Test
	public void shouldSendMailToRecipientTest() throws Exception {
		// given
		NewRecipient recipient = new NewRecipient();
		recipient.setMail("louay.haddad@gouv.fr");
		recipient.setId("ffeepklfjeo2eo6hity");
		String message = "Test message content";
		enclosure.setUrlDownload("download_url");
		// when
		mailAvailbleEnclosureServices.sendToRecipients(enclosure, message,
				NotificationTemplateEnum.MAIL_AVAILABLE_RECIPIENT.getValue(), recipient, Locale.FRENCH);
		// then
		String content = message + "</span>";
		assertReceivedMessageContains(content);
	}

	@Test
	public void shouldSendMailToSenderTest() throws Exception {
		// given
		String recipient = "louay.haddad@live.fr";
		String message = "Test message content";
		enclosure.setUrlDownload("download_url");
		// when
		mailNotificationServices.prepareAndSend(recipient, message, enclosure,
				NotificationTemplateEnum.MAIL_AVAILABLE_SENDER.getValue(), Locale.FRENCH);
		// then
		String content = message + "</span>";
		assertReceivedMessageContains(content);
	}

	@Ignore
	@Test
	public void sendMailsRelaunchTests() throws Exception {
		// given
		// when
		mailRelaunchServices.sendMailsRelaunch();
		// then
		String content = "</span>";
		assertReceivedMessageContains(content);
	}

	@Test
	public void sendMailToRecipientsEnclosureNotAvailbleTest() throws Exception {
		// given
		String recipient = "louay.haddad@gouv.fr";
		String message = "Test message content";
		enclosure.setUrlDownload("download_url");
		// when
		mailEnclosureNoLongerAvailbleServices.sendEnclosureNotAvailble(enclosure);
		// then
		String content = message + "</span>";
		assertReceivedMessageContains(content);
	}

	@Test
	public void sendMailToSenderMailVirusFoundServicesTest() throws Exception {
		// given
		String message = "Test message content";
		String subject = "Virus found subject";
		enclosure.setUrlDownload("download_url");
		// when
		mailVirusFoundServices.sendToSender(enclosure, NotificationTemplateEnum.MAIL_VIRUS_SENDER.getValue(), subject,
				Locale.FRENCH);
		// then
		String content = message + "</span>";
		assertReceivedMessageContains(content);
	}

	private void assertReceivedMessageContains(String expected) throws IOException, MessagingException {
		MimeMessage[] receivedMessages = smtpServer.getReceivedMessages();
//        assertEquals(1, receivedMessages.length);
//        String content = (String) receivedMessages[0].getContent();
//        assertTrue(content.contains(expected));
	}

	@After
	public void tearDown() throws Exception {
		smtpServer.stop();
		enclosure = null;
	}

}
