import { DatabaseSync } from 'node:sqlite';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { User } from '../models/types.js';

const JWT_SECRET = process.env.JWT_SECRET || 'liquor_inventory_secret_jwt_key_2026';
const TOKEN_EXPIRY = '12h';

export interface UserSession {
  id: number;
  username: string;
  role: 'owner' | 'staff';
}

export class AuthService {
  static login(db: DatabaseSync, username: string, password: string): { user: UserSession; token: string } {
    if (!username || !password) {
      throw new Error('Username and password are required.');
    }

    const stmt = db.prepare('SELECT id, username, password_hash, role FROM users WHERE username = ?');
    const user = stmt.get(username.trim()) as User | undefined;

    if (!user) {
      throw new Error('Invalid username or password.');
    }

    const isValid = bcrypt.compareSync(password, user.password_hash);
    if (!isValid) {
      throw new Error('Invalid username or password.');
    }

    const session: UserSession = {
      id: user.id,
      username: user.username,
      role: user.role,
    };

    const token = jwt.sign(session, JWT_SECRET, { expiresIn: TOKEN_EXPIRY });

    return { user: session, token };
  }

  static verifyToken(token: string): UserSession {
    try {
      const decoded = jwt.verify(token, JWT_SECRET) as UserSession;
      return decoded;
    } catch {
      throw new Error('Invalid or expired authentication token.');
    }
  }

  static getUserById(db: DatabaseSync, id: number): UserSession | null {
    const stmt = db.prepare('SELECT id, username, role FROM users WHERE id = ?');
    const user = stmt.get(id) as { id: number; username: string; role: 'owner' | 'staff' } | undefined;
    return user ?? null;
  }
}
