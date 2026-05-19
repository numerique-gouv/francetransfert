/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { describe, it, expect } from 'vitest';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { of } from 'rxjs';

import { FileEncryptionService, EncryptStreamInput } from './file-encryption.service';
import { SodiumService } from '../sodium/sodium.service';
import { TempEncryptedStorageService } from '../temp-encrypted-storage/temp-encrypted-storage.service';

// Vitest runs in Node, so there is no Angular DI here — we instantiate the
// service directly with its real crypto/OPFS deps (OPFS is absent in Node
// and the storage service falls back to in-memory automatically) and a
// no-op LoggingService stub (no backend to talk to).
function buildService(): FileEncryptionService {
  const sodiumService = new SodiumService();
  const tempStorage = new TempEncryptedStorageService();
  const logging = { logInfo: (_: string) => of(null) };
  return new FileEncryptionService(sodiumService, tempStorage, logging as never);
}

async function loadSamplePng(): Promise<Uint8Array> {
  // Same fixture the dev server serves at /assets/images/test.png — read
  // from disk so the test is hermetic and doesn't need an HTTP server.
  const path = join(__dirname, '..', '..', '..', 'assets', 'images', 'test.png');
  const buf = await readFile(path);
  return new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength);
}

async function readStreamToBytes(stream: ReadableStream<Uint8Array>): Promise<Uint8Array> {
  const reader = stream.getReader();
  const parts: Uint8Array[] = [];
  let total = 0;
  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    if (value) {
      parts.push(value);
      total += value.byteLength;
    }
  }
  const merged = new Uint8Array(total);
  let offset = 0;
  for (const part of parts) {
    merged.set(part, offset);
    offset += part.byteLength;
  }
  return merged;
}

// createDecryptTransformStream zeroes the pli key in its flush step, so any
// caller that wants to decrypt after encrypting hands the decryptor a clone.
function clonePliKey(pliKey: Uint8Array): Uint8Array {
  return new Uint8Array(pliKey);
}

describe('FileEncryptionService (Node)', () => {
  it('round-trips a single file through stream-encrypt → stream-decrypt', async () => {
    const service = buildService();
    const pngBytes = await loadSamplePng();
    expect(pngBytes.byteLength).toBeGreaterThan(0);

    const pngFile = new File([pngBytes], 'test.png', { type: 'image/png', lastModified: 1_700_000_000_000 });
    const pliKey = await service.generatePliKey();

    const input: EncryptStreamInput = {
      stream: pngFile.stream() as unknown as ReadableStream<Uint8Array>,
      size: pngFile.size,
      name: pngFile.name,
      lastModified: pngFile.lastModified,
    };
    const result = await service.encryptStreamWithPliKey(input, pliKey);
    expect(result.encryptedFile.originalSize).toBe(pngBytes.byteLength);

    // Ciphertext must be longer than plaintext: 24-byte secretstream header +
    // one 17-byte poly1305 tag per chunk. If it were equal-or-shorter the
    // encryptor would be silently passing plaintext through.
    const ciphertextBytes = new Uint8Array(await result.encryptedFile.encryptedFile.arrayBuffer());
    expect(ciphertextBytes.byteLength).toBeGreaterThan(pngBytes.byteLength);

    // Reuse the production decrypt path — same TransformStream the recipient
    // browser uses — to prove encrypt/decrypt are inverses.
    const decryptStream = (result.encryptedFile.encryptedFile.stream() as unknown as ReadableStream<Uint8Array>)
      .pipeThrough(service.createDecryptTransformStream(clonePliKey(pliKey)));
    const decryptedBytes = await readStreamToBytes(decryptStream);

    expect(decryptedBytes.byteLength).toBe(pngBytes.byteLength);
    expect(decryptedBytes).toEqual(pngBytes);
  });

  it('round-trips a client-zip stream through stream-encrypt → stream-decrypt', async () => {
    const service = buildService();
    const pngBytes = await loadSamplePng();
    const pngFile = new File([pngBytes], 'test.png', { type: 'image/png', lastModified: 1_700_000_000_000 });
    const noteBytes = new TextEncoder().encode('hello from france transfert E2EE round-trip\n');
    const noteFile = new File([noteBytes], 'note.txt', { type: 'text/plain', lastModified: 1_700_000_000_000 });

    // client-zip works in Node 20+ the same way it does in the browser —
    // it only depends on ReadableStream, which is a Node global.
    const clientZip = await import('client-zip');
    const entries = [
      { name: 'test.png', lastModified: new Date(pngFile.lastModified), input: pngFile },
      { name: 'note.txt', lastModified: new Date(noteFile.lastModified), input: noteFile },
    ];

    // tee() the zip stream: one branch feeds the encryptor, the other
    // captures the "expected plaintext" so the comparison is against the
    // exact bytes the encryptor saw (rebuilding the zip separately would
    // be brittle if client-zip ever tweaks its byte layout).
    const zipResponse = clientZip.downloadZip(entries);
    expect(zipResponse.body).not.toBeNull();
    const [streamForEncrypt, streamForExpected] = (zipResponse.body as ReadableStream<Uint8Array>).tee();
    const expectedZipBytes = await readStreamToBytes(streamForExpected);
    expect(expectedZipBytes.byteLength).toBeGreaterThan(pngBytes.byteLength);

    // predictLength gives the exact final zip size up front, which is what
    // the production code passes as `size` so the progress meter and the
    // OPFS quota check stay accurate without buffering the zip.
    const predictedSize = Number(clientZip.predictLength(entries));
    expect(predictedSize).toBe(expectedZipBytes.byteLength);

    const pliKey = await service.generatePliKey();
    const input: EncryptStreamInput = {
      stream: streamForEncrypt,
      size: predictedSize,
      name: 'round-trip.zip',
    };
    const progressTicks: number[] = [];
    const result = await service.encryptStreamWithPliKey(input, pliKey, (p) => progressTicks.push(p));
    expect(progressTicks[0]).toBe(0);
    expect(progressTicks[progressTicks.length - 1]).toBe(100);
    expect(result.encryptedFile.originalSize).toBe(predictedSize);

    const decryptedBytes = await readStreamToBytes(
      (result.encryptedFile.encryptedFile.stream() as unknown as ReadableStream<Uint8Array>)
        .pipeThrough(service.createDecryptTransformStream(clonePliKey(pliKey))),
    );

    expect(decryptedBytes.byteLength).toBe(expectedZipBytes.byteLength);
    expect(decryptedBytes).toEqual(expectedZipBytes);
  });

  it('exports and re-imports a pli key as base64url without loss', async () => {
    const service = buildService();
    const original = await service.generatePliKey();
    const b64 = await service.exportPliKey(original);
    expect(b64).not.toMatch(/[+/=]/); // base64url is URL-safe, no `=` padding.
    const restored = await service.importPliKey(b64);
    expect(restored).toEqual(original);
  });
});
