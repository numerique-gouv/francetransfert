// Dev-server proxy (`npm run start` / `ng serve`): the app calls relative /api-private/... URLs,
// like in production behind nginx, and the dev server forwards them to the local backends.
// Override the targets when the APIs run elsewhere, e.g. in docker-compose:
//   UPLOAD_API_URL=http://upload-api:8080 DOWNLOAD_API_URL=http://download-api:8081 npm run start
const uploadApi = process.env.UPLOAD_API_URL ?? 'http://127.0.0.1:8080';
const downloadApi = process.env.DOWNLOAD_API_URL ?? 'http://127.0.0.1:8081';

const to = target => ({ target, secure: false, changeOrigin: true, logLevel: 'warn' });

export default {
  '/api-private/upload-module': to(uploadApi),
  '/api-private/confirmation-module': to(uploadApi),
  '/api-private/download-module': to(downloadApi),
};
