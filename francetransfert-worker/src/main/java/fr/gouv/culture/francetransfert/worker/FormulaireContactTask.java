/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.worker;

import fr.gouv.culture.francetransfert.core.model.FormulaireContactData;
import fr.gouv.culture.francetransfert.services.mail.notification.MailFormulaireContactServices;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FormulaireContactTask implements Runnable{
    private static final Logger LOGGER = LoggerFactory.getLogger(FormulaireContactTask.class);

    FormulaireContactData formuleContactData;
    MailFormulaireContactServices formulaireContactService;

    public FormulaireContactTask(FormulaireContactData formuleContactData, MailFormulaireContactServices formulaireContactService) {
        this.formulaireContactService = formulaireContactService;
        this.formuleContactData = formuleContactData;

    }

    public FormulaireContactTask(){

    }

    @Override
    public void run() {
        try {
            LOGGER.info("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: "
                    + Thread.currentThread().getId());
            formulaireContactService.sendMailContact(formuleContactData, NotificationTemplateEnum.Mail_contact.getValue());
        } catch (Exception e) {
            LOGGER.error("[Worker] error while creating formulaire Contact : " + e.getMessage(), e);
        }
        LOGGER.info("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: " + Thread.currentThread().getId()
                + " IS DEAD");
    }
}

