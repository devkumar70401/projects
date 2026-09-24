import { Request, Response, NextFunction } from 'express';
import { AuthService, UserSession } from '../services/authService.js';

export interface AuthRequest extends Request {
  user?: UserSession;
}

export function requireAuth(req: AuthRequest, res: Response, next: NextFunction): void {
  // Check cookie or Authorization header
  let token = req.cookies?.token;

  if (!token && req.headers.authorization) {
    const parts = req.headers.authorization.split(' ');
    if (parts.length === 2 && parts[0] === 'Bearer') {
      token = parts[1];
    }
  }

  if (!token) {
    res.status(401).json({ error: 'Unauthorized. Please log in.' });
    return;
  }

  try {
    const user = AuthService.verifyToken(token);
    req.user = user;
    next();
  } catch (err: any) {
    res.status(401).json({ error: err.message || 'Unauthorized.' });
  }
}
