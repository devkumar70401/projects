import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { getDatabase } from './connection.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export function runMigrations(dbPath?: string) {
  const db = getDatabase(dbPath);
  const schemaPath = path.resolve(__dirname, 'schema.sql');
  const schemaSql = fs.readFileSync(schemaPath, 'utf8');
  
  console.log('Running database migrations...');
  db.exec(schemaSql);
  console.log('Database migrations completed successfully.');
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  runMigrations();
}
