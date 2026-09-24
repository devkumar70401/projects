import { describe, it, expect, beforeEach } from 'vitest';
import { DatabaseSync } from 'node:sqlite';
import bcrypt from 'bcryptjs';
import { AuthService } from '../src/services/authService.js';

describe('Auth Service', () => {
  let db: DatabaseSync;

  beforeEach(() => {
    db = new DatabaseSync(':memory:');
    db.exec(`
      CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        role TEXT NOT NULL DEFAULT 'owner',
        created_at TEXT NOT NULL DEFAULT (datetime('now'))
      );
    `);

    const hash = bcrypt.hashSync('correctpass123', 10);
    db.prepare('INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)').run('owner1', hash, 'owner');
  });

  it('authenticates user with correct credentials and returns JWT token', () => {
    const { user, token } = AuthService.login(db, 'owner1', 'correctpass123');
    expect(user.username).toBe('owner1');
    expect(user.role).toBe('owner');
    expect(token).toBeDefined();

    const decoded = AuthService.verifyToken(token);
    expect(decoded.username).toBe('owner1');
    expect(decoded.id).toBe(user.id);
  });

  it('rejects authentication with incorrect password', () => {
    expect(() => AuthService.login(db, 'owner1', 'wrongpassword')).toThrow('Invalid username or password.');
  });

  it('rejects authentication with non-existent username', () => {
    expect(() => AuthService.login(db, 'ghost_user', 'anypass')).toThrow('Invalid username or password.');
  });
});
