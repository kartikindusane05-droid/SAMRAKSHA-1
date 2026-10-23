const fs = require('fs');
const path = require('path');

const srcDir = path.resolve(__dirname, '../src/db/migrations');
const destDir = path.resolve(__dirname, '../dist/db/migrations');

try {
  if (fs.existsSync(srcDir)) {
    fs.mkdirSync(destDir, { recursive: true });
    fs.cpSync(srcDir, destDir, { recursive: true });
    console.log(`[build] Successfully copied database migrations to ${destDir}`);
  } else {
    console.warn(`[build] Migration source directory not found: ${srcDir}`);
  }
} catch (err) {
  console.error('[build] Failed to copy migrations:', err);
  process.exit(1);
}
