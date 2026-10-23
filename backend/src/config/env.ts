import dotenv from 'dotenv';
import path from 'path';
import fs from 'fs';

let isEnvLoaded = false;

/**
 * Reliably loads the backend/.env file regardless of current working directory.
 * Never prints password or sensitive credential values.
 */
export function loadEnv(): void {
  if (isEnvLoaded) return;

  const candidatePaths = [
    // Relative to this file when compiled in dist/config or source in src/config
    path.resolve(__dirname, '../../.env'),
    // Relative if compiled or bundled directly into dist or src
    path.resolve(__dirname, '../.env'),
    // Terminal CWD is backend/
    path.resolve(process.cwd(), '.env'),
    // Terminal CWD is project root (e.g. E:\SAMRAKSHA)
    path.resolve(process.cwd(), 'backend/.env')
  ];

  for (const candidate of candidatePaths) {
    try {
      if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        dotenv.config({ path: candidate });
        isEnvLoaded = true;
        return;
      }
    } catch {
      // Continue checking next candidate
    }
  }

  // Fallback to default dotenv lookup
  dotenv.config();
  isEnvLoaded = true;
}

// Auto-load on initial module evaluation
loadEnv();
