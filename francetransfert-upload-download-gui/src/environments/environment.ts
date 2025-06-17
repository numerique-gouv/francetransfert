/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  host: 'http://localhost:',
  apis: {
    config: '8080/api-private/upload-module/config',
    logout: '8080/api-private/upload-module/logout',
    upload: {
      upload: '8080/api-private/upload-module/upload',
      tree: '8080/api-private/upload-module/sender-info',
      confirmationCode: '8080/api-private/upload-module/validate-code',
      generateCode: '8080/api-private/confirmation-module/generate-code',
      validateCode: '8080/api-private/confirmation-module/validate-code',
      rate: '8080/api-private/upload-module/satisfaction',
      validateMail: '8080/api-private/upload-module/validate-mail',
      config: '8080/api-private/upload-module/config',
      allowedSenderMail: '8080/api-private/upload-module/allowed-sender-mail',
      formulaireContact: '8080/api-private/upload-module/sender-contact'
    },
    download: {
      download: '8081/api-private/download-module/download-info',
      downloadUrl: '8081/api-private/download-module/generate-download-url',
      downloadInfosPublic: '8081/api-private/download-module/download-info-public',
      downloadUrlPublic: '8081/api-private/download-module/generate-download-url-public',
      validatePassword: '8081/api-private/download-module/validate-password',
      rate: '8081/api-private/download-module/satisfaction',
      downloadConnect: '8081/api-private/download-module/download-info-connect',
    },
    admin: {
      deleteFile: '8080/api-private/upload-module/delete-file',
      updateExpiredDate: '8080/api-private/upload-module/update-expired-date',
      fileInfos: '8080/api-private/upload-module/file-info',
      fileInfosConnect: '8080/api-private/upload-module/file-info-connect',
      fileInfosReciever: '8080/api-private/upload-module/file-info-reciever',
      addNewRecipient: '8080/api-private/upload-module/add-recipient',
      deleteRecipient: '8080/api-private/upload-module/delete-recipient',
      getPlisSent: '8080/api-private/upload-module/get-plis-sent',
      getPlisReceived: '8080/api-private/upload-module/get-plis-received',
      resendLink: '8080/api-private/upload-module/resend-download-link',
      export: '8080/api-private/upload-module/get-export',
      urlExport: '8080/api-private/upload-module/get-url-export',
    },
    captcha: {
      url: 'https://test-francetransfert.aot.agency/captcha/captcha/'
    }
  },
  version: '4.0.0'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
