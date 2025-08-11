/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import Flow from '@flowjs/flow.js';
import { environment as env } from '../../../environments/environment';
export const FLOW_CONFIG: Flow.FlowOptions = {
  target: env.host ? `${env.host}${env.apis.upload.upload}` : '',
  chunkSize: 1024 * 1024 * 5, // 5 Mo
  testChunks: true,
  maxChunkRetries: 5,
  chunkRetryInterval: 30000,
  prioritizeFirstAndLastChunk: true,
  allowDuplicateUploads: false,
  simultaneousUploads: 3,
  //withCredentials : true,
  generateUniqueIdentifier: (file) => {
    var uuid = Math.random().toString(36).slice(-6).replace(/[^0-9a-zA-Z_-]/img, '');
    const relativePath = file.relativePath || file.webkitRelativePath || file.fileName || file.name;
    return file.size + '-' + relativePath.replace(/[^0-9a-zA-Z_-]/img, '') + '-' + uuid;
  }
};
