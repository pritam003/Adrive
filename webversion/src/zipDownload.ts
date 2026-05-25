// Streaming ZIP download using `client-zip` library style - implemented inline to avoid extra dep.
// Builds a STORED (uncompressed) zip on the fly. This means the zip size = sum of file sizes + overhead.
// We stream entries one at a time to keep memory low.

import type { DriveFile } from './types';
import { getReadSasCached } from './api';

// CRC32 table
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let j = 0; j < 8; j++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[i] = c >>> 0;
  }
  return t;
})();

function crc32(buf: Uint8Array, crc = 0xffffffff): number {
  let c = crc;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return c >>> 0;
}

const encoder = new TextEncoder();
function dosTime(d = new Date()): { time: number; date: number } {
  const time = (d.getHours() << 11) | (d.getMinutes() << 5) | Math.floor(d.getSeconds() / 2);
  const date = ((d.getFullYear() - 1980) << 9) | ((d.getMonth() + 1) << 5) | d.getDate();
  return { time, date };
}

interface CentralEntry {
  name: Uint8Array;
  crc: number;
  size: number;
  offset: number;
  time: number;
  date: number;
}

export async function downloadAsZip(
  files: DriveFile[],
  zipName: string,
  onProgress?: (loadedBytes: number, totalBytes: number) => void
): Promise<void> {
  // We'll build the zip as a ReadableStream and pipe into a download.
  const total = files.reduce((s, f) => s + f.size, 0);
  let loaded = 0;
  const central: CentralEntry[] = [];
  let offset = 0;

  const stream = new ReadableStream<Uint8Array>({
    async start(controller) {
      try {
        for (const file of files) {
          const url = await getReadSasCached(file.name);
          const resp = await fetch(url);
          if (!resp.ok || !resp.body) throw new Error(`Failed to fetch ${file.displayName}`);

          const safeName = file.displayName.replace(/^\/+/, '');
          const nameBytes = encoder.encode(safeName);
          const { time, date } = dosTime();

          // Two-pass: read into memory to compute CRC (simpler than data descriptors).
          // For very large files, we still buffer fully. Acceptable for typical Drive use.
          const reader = resp.body.getReader();
          const chunks: Uint8Array[] = [];
          let crc = 0xffffffff;
          let size = 0;
          while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            chunks.push(value);
            crc = crc32(value, crc);
            size += value.length;
            loaded += value.length;
            if (onProgress) onProgress(loaded, total);
          }
          crc = (crc ^ 0xffffffff) >>> 0;

          // Local file header
          const header = new Uint8Array(30 + nameBytes.length);
          const hv = new DataView(header.buffer);
          hv.setUint32(0, 0x04034b50, true);     // signature
          hv.setUint16(4, 20, true);              // version
          hv.setUint16(6, 0, true);               // flags
          hv.setUint16(8, 0, true);               // method = stored
          hv.setUint16(10, time, true);
          hv.setUint16(12, date, true);
          hv.setUint32(14, crc, true);
          hv.setUint32(18, size, true);           // compressed
          hv.setUint32(22, size, true);           // uncompressed
          hv.setUint16(26, nameBytes.length, true);
          hv.setUint16(28, 0, true);              // extra
          header.set(nameBytes, 30);
          controller.enqueue(header);

          for (const c of chunks) controller.enqueue(c);

          central.push({ name: nameBytes, crc, size, offset, time, date });
          offset += header.length + size;
        }

        // Central directory
        let cdSize = 0;
        const cdBuffers: Uint8Array[] = [];
        for (const e of central) {
          const rec = new Uint8Array(46 + e.name.length);
          const v = new DataView(rec.buffer);
          v.setUint32(0, 0x02014b50, true);  // central sig
          v.setUint16(4, 20, true);           // version made by
          v.setUint16(6, 20, true);           // version needed
          v.setUint16(8, 0, true);            // flags
          v.setUint16(10, 0, true);           // method
          v.setUint16(12, e.time, true);
          v.setUint16(14, e.date, true);
          v.setUint32(16, e.crc, true);
          v.setUint32(20, e.size, true);
          v.setUint32(24, e.size, true);
          v.setUint16(28, e.name.length, true);
          v.setUint16(30, 0, true);           // extra
          v.setUint16(32, 0, true);           // comment
          v.setUint16(34, 0, true);           // disk
          v.setUint16(36, 0, true);           // internal attr
          v.setUint32(38, 0, true);           // external attr
          v.setUint32(42, e.offset, true);
          rec.set(e.name, 46);
          cdBuffers.push(rec);
          cdSize += rec.length;
          controller.enqueue(rec);
        }

        // End of central directory
        const eocd = new Uint8Array(22);
        const ev = new DataView(eocd.buffer);
        ev.setUint32(0, 0x06054b50, true);
        ev.setUint16(4, 0, true);
        ev.setUint16(6, 0, true);
        ev.setUint16(8, central.length, true);
        ev.setUint16(10, central.length, true);
        ev.setUint32(12, cdSize, true);
        ev.setUint32(16, offset, true);
        ev.setUint16(20, 0, true);
        controller.enqueue(eocd);

        controller.close();
        void cdBuffers; // suppress unused
      } catch (e) {
        controller.error(e);
      }
    },
  });

  // Pipe stream to a Blob via response, then download
  const resp = new Response(stream, { headers: { 'Content-Type': 'application/zip' } });
  const blob = await resp.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = zipName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 60000);
}