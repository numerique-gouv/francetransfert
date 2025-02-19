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
  chunkRetryInterval: 5000,
  prioritizeFirstAndLastChunk: true,
  allowDuplicateUploads: false,
  simultaneousUploads: 3
  //withCredentials : true,
};
