import { Router, Response } from 'express';
import { getDatabase } from '../db/connection.js';
import { AuthService } from '../services/authService.js';
import { requireAuth, AuthRequest } from '../middleware/requireAuth.js';

const router = Router();

router.post('/login', (req, res) => {
  const { username, password } = req.body;
  const db = getDatabase();

  try {
    const { user, token } = AuthService.login(db, username, password);

    // Set httpOnly cookie
    res.cookie('token', token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: 12 * 60 * 60 * 1000, // 12 hours
    });

    res.json({
      success: true,
      user,
      token,
    });
  } catch (err: any) {
    res.status(400).json({ error: err.message || 'Login failed.' });
  }
});

router.post('/logout', (req, res) => {
  res.clearCookie('token', {
    httpOnly: true,
    sameSite: 'lax',
    secure: process.env.NODE_ENV === 'production',
  });
  res.json({ success: true, message: 'Logged out successfully.' });
});

router.get('/me', requireAuth, (req: AuthRequest, res: Response) => {
  const db = getDatabase();
  if (!req.user) {
    res.status(401).json({ error: 'Not authenticated' });
    return;
  }
  const user = AuthService.getUserById(db, req.user.id);
  if (!user) {
    res.status(401).json({ error: 'User no longer exists' });
    return;
  }
  res.json({ user });
});

export default router;
