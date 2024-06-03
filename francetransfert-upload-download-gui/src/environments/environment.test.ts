/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
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
      rate: 'api-private/upload-module/satisfaction',
      validateMail: 'api-private/upload-module/validate-mail',
      config: 'api-private/upload-module/config'
    },
    download: {
      download: 'api-private/download-module/download-info',
      downloadUrl: 'api-private/download-module/generate-download-url',
      downloadInfosPublic: 'api-private/download-module/download-info-public',
      downloadUrlPublic: 'api-private/download-module/generate-download-url-public',
      validatePassword: 'api-private/download-module/validate-password',
      rate: 'api-private/download-module/satisfaction',
      downloadConnect: 'api-private/download-module/download-info-connect',
    },
    admin: {
      deleteFile: 'api-private/upload-module/delete-file',
      updateExpiredDate: 'api-private/upload-module/update-expired-date',
      fileInfos: 'api-private/upload-module/file-info',
      fileInfosConnect: 'api-private/upload-module/file-info-connect',
      getPlisSent: 'api-private/upload-module/get-plis-sent',
      getPlisReceived: 'api-private/upload-module/get-plis-received'
    },
    captcha: {
      url: '/captcha/captcha/'
    }
  },
  version: '3.12.9'
};
