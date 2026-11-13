/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

// Same imports as admin.component.ts / admin.service.ts: guards the CommonJS interop of
// pdfmake + its bundled fonts under the esbuild/Vite pipeline (it broke `ng serve` once).
import pdfMake from 'pdfmake/build/pdfmake';
import pdfFonts from 'pdfmake/build/vfs_fonts';

describe('pdfmake setup', () => {
  it('exposes the Roboto virtual file system', () => {
    expect(Object.keys(pdfFonts)).toContain('Roboto-Regular.ttf');
  });

  it('generates a PDF with the bundled fonts', async () => {
    (pdfMake as any).vfs = pdfFonts;
    const base64 = await new Promise<string>(resolve =>
      pdfMake.createPdf({ content: ['France transfert'] } as any).getBase64((data: string) => resolve(data)));
    expect(atob(base64).startsWith('%PDF')).toBe(true);
  });
});
