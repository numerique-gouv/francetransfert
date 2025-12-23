/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

export const environment = {
  production: true,
  host: '/',
  apis: {
    config: 'api-private/upload-module/config',
    logout: 'api-private/upload-module/logout',
    upload: {
      upload: 'api-private/upload-module/upload',
      tree: 'api-private/upload-module/sender-info',
      confirmationCode: 'api-private/upload-module/validate-code',
      generateCode: 'api-private/confirmation-module/generate-code',
      validateCode: 'api-private/confirmation-module/validate-code',
      rate: 'api-private/upload-module/satisfaction',
      validateMail: 'api-private/upload-module/validate-mail',
      config: 'api-private/upload-module/config',
      allowedSenderMail: 'api-private/upload-module/allowed-sender-mail',
      formulaireContact: 'api-private/upload-module/sender-contact'
    },
    download: {
      download: 'api-private/download-module/download-info',
      downloadUrl: 'api-private/download-module/generate-download-url',
      downloadInfosPublic: 'api-private/download-module/download-info-public',
      downloadUrlPublic: 'api-private/download-module/generate-download-url-public',
      validatePassword: 'api-private/download-module/validate-password',
      rate: 'api-private/download-module/satisfaction'
    },
    admin: {
      deleteFile: 'api-private/upload-module/delete-file',
      updateExpiredDate: 'api-private/upload-module/update-expired-date',
      fileInfos: 'api-private/upload-module/file-info',
      fileInfosConnect: 'api-private/upload-module/file-info-connect',
      addNewRecipient: 'api-private/upload-module/add-recipient',
      deleteRecipient: 'api-private/upload-module/delete-recipient',
      getPlisSent: 'api-private/upload-module/get-plis-sent',
      downloadConnect: 'api-private/download-module/download-info-connect',
      export: 'api-private/upload-module/get-export',
      
    },
    captcha: {
      url: '/captcha/captcha/'
    }
  },
  version: '4.0.3'
};
