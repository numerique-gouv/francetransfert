/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';

export class OpfsStorageUnavailableError extends Error {
  constructor(message: string, public readonly cause?: unknown) {
    super(message);
    this.name = 'OpfsStorageUnavailableError';
  }
}

export class OpfsStorageWriteError extends Error {
  constructor(message: string, public readonly cause?: unknown) {
    super(message);
    this.name = 'OpfsStorageWriteError';
  }
}

type OpfsDirectory = {
  getFileHandle: (name: string, options: { create: boolean }) => Promise<{
    createWritable: () => Promise<{ write: (chunk: BufferSource | Blob | string) => Promise<void>; close: () => Promise<void>; abort: () => Promise<void> }>;
    getFile: () => Promise<File>;
  }>;
  entries: () => AsyncIterableIterator<[string, unknown]>;
  removeEntry: (name: string) => Promise<void>;
};

@Injectable({
  providedIn: 'root'
})
export class TempEncryptedStorageService {
  private readonly opfsTempPrefix = 'ft-encrypted-';
  private readonly opfsTempFileNames = new Set<string>();
  private staleSweepDone = false;

  async writeEncryptedFile(
    originalName: string,
    writeContent: (writeChunk: (chunk: Uint8Array) => Promise<void>) => Promise<void>
  ): Promise<File | null> {
    const opfsRoot = await this.getOpfsRootDirectory();
    if (!opfsRoot) {
      return null;
    }
    await this.ensureStaleSweep(opfsRoot);

    const fileName = this.buildOpfsTempFileName(originalName);
    let fileHandle: Awaited<ReturnType<OpfsDirectory['getFileHandle']>>;
    let writable: Awaited<ReturnType<Awaited<ReturnType<OpfsDirectory['getFileHandle']>>['createWritable']>>;
    try {
      fileHandle = await opfsRoot.getFileHandle(fileName, { create: true });
      writable = await fileHandle.createWritable();
    } catch (error) {
      console.error('Unable to initialize OPFS writer', error);
      throw new OpfsStorageWriteError('Unable to initialize OPFS writer', error);
    }
    this.opfsTempFileNames.add(fileName);

    try {
      await writeContent(async (chunk: Uint8Array) => {
        try {
          await writable.write(chunk as unknown as BufferSource);
        } catch (error) {
          console.error('Unable to write encrypted chunk to OPFS', error);
          throw new OpfsStorageWriteError('Unable to write encrypted chunk to OPFS', error);
        }
      });
      await writable.close();

      const opfsFile = await fileHandle.getFile();
      return new File([opfsFile], originalName, {
        type: 'application/octet-stream',
        lastModified: Date.now()
      });
    } catch (error) {
      try {
        console.error('Unable to abort OPFS writer', error);
        await writable.abort();
      } catch (error) {
        console.error('Unable to abort OPFS writer', error);
        // Ignore abort failures.
      }
      throw error;
    }
  }

  async cleanupTemporaryEncryptedFiles(): Promise<void> {
    let opfsRoot: OpfsDirectory | null = null;
    try {
      opfsRoot = await this.getOpfsRootDirectory();
    } catch {
      return;
    }
    if (!opfsRoot || this.opfsTempFileNames.size === 0) {
      return;
    }
    for (const fileName of Array.from(this.opfsTempFileNames)) {
      try {
        await opfsRoot.removeEntry(fileName);
      } catch {
        // Ignore deletion failures (file may already be gone).
      } finally {
        this.opfsTempFileNames.delete(fileName);
      }
    }
  }

  private async getOpfsRootDirectory(): Promise<OpfsDirectory | null> {
    const storage = (navigator as Navigator & { storage?: StorageManager & { getDirectory?: () => Promise<unknown> } }).storage;
    if (!storage?.getDirectory) {
      return null;
    }
    try {
      return await storage.getDirectory() as unknown as OpfsDirectory;
    } catch (error) {
      throw new OpfsStorageUnavailableError('Unable to access OPFS root directory', error);
    }
  }

  private async ensureStaleSweep(opfsRoot: OpfsDirectory): Promise<void> {
    if (this.staleSweepDone) {
      return;
    }
    this.staleSweepDone = true;
    try {
      for await (const [name] of opfsRoot.entries()) {
        if (name.startsWith(this.opfsTempPrefix) && !this.opfsTempFileNames.has(name)) {
          await opfsRoot.removeEntry(name);
        }
      }
    } catch {
      // Ignore stale cleanup failures.
    }
  }

  isStorageFallbackError(error: unknown): boolean {
    console.error('isStorageFallbackError', error);
    return error instanceof OpfsStorageUnavailableError || error instanceof OpfsStorageWriteError;
  }

  async isOpfsAccessible(): Promise<boolean> {
    try {
      const opfsRoot = await this.getOpfsRootDirectory();
      return opfsRoot !== null;
    } catch {
      return false;
    }
  }

  private buildOpfsTempFileName(originalName: string): string {
    const safeName = originalName.replace(/[^a-zA-Z0-9._-]/g, '_');
    const randomSuffix = Math.random().toString(36).slice(2, 10);
    return `${this.opfsTempPrefix}${Date.now()}-${randomSuffix}-${safeName}`;
  }
}
