import { Router } from 'express';
import { getDatabase } from '../db/connection.js';
import { Shop } from '../models/types.js';

const router = Router();

// GET /api/shops - list shops
router.get('/', (req, res) => {
  const db = getDatabase();
  const includeInactive = req.query.all === 'true';
  const query = includeInactive
    ? 'SELECT * FROM shops ORDER BY id ASC'
    : 'SELECT * FROM shops WHERE active = 1 ORDER BY id ASC';
  const shops = db.prepare(query).all() as unknown as Shop[];
  res.json({ shops });
});

// POST /api/shops - create a new shop
router.post('/', (req, res) => {
  const { name } = req.body;
  if (!name || typeof name !== 'string' || !name.trim()) {
    res.status(400).json({ error: 'Shop name is required.' });
    return;
  }

  const db = getDatabase();
  try {
    const stmt = db.prepare('INSERT INTO shops (name, active) VALUES (?, 1)');
    const result = stmt.run(name.trim());
    const newShop = db.prepare('SELECT * FROM shops WHERE id = ?').get(Number(result.lastInsertRowid)) as unknown as Shop;
    res.status(201).json({ shop: newShop });
  } catch (err: any) {
    if (err.message?.includes('UNIQUE constraint failed')) {
      res.status(400).json({ error: `A shop with the name "${name.trim()}" already exists.` });
      return;
    }
    res.status(500).json({ error: err.message || 'Failed to create shop.' });
  }
});

// PUT /api/shops/:id - update shop name or active state
router.put('/:id', (req, res) => {
  const id = Number(req.params.id);
  const { name, active } = req.body;

  if (isNaN(id)) {
    res.status(400).json({ error: 'Invalid shop ID.' });
    return;
  }

  const db = getDatabase();
  const existing = db.prepare('SELECT * FROM shops WHERE id = ?').get(id) as unknown as Shop | undefined;
  if (!existing) {
    res.status(404).json({ error: 'Shop not found.' });
    return;
  }

  const updatedName = name !== undefined ? String(name).trim() : existing.name;
  const updatedActive = active !== undefined ? (active ? 1 : 0) : existing.active;

  try {
    db.prepare('UPDATE shops SET name = ?, active = ? WHERE id = ?').run(updatedName, updatedActive, id);
    const updated = db.prepare('SELECT * FROM shops WHERE id = ?').get(id) as unknown as Shop;
    res.json({ shop: updated });
  } catch (err: any) {
    if (err.message?.includes('UNIQUE constraint failed')) {
      res.status(400).json({ error: `A shop with the name "${updatedName}" already exists.` });
      return;
    }
    res.status(500).json({ error: err.message || 'Failed to update shop.' });
  }
});

export default router;
