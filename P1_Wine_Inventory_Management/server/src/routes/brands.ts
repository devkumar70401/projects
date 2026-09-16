import { Router } from 'express';
import { getDatabase, runTransaction } from '../db/connection.js';
import { Brand } from '../models/types.js';

const router = Router();

// GET /api/brands - list brands
router.get('/', (req, res) => {
  const db = getDatabase();
  const includeInactive = req.query.all === 'true';
  const query = includeInactive
    ? 'SELECT * FROM brands ORDER BY display_order ASC, id ASC'
    : 'SELECT * FROM brands WHERE active = 1 ORDER BY display_order ASC, id ASC';
  const brands = db.prepare(query).all() as unknown as Brand[];
  res.json({ brands });
});

// POST /api/brands - create new brand
router.post('/', (req, res) => {
  const { name, display_order } = req.body;
  if (!name || typeof name !== 'string' || !name.trim()) {
    res.status(400).json({ error: 'Brand name is required.' });
    return;
  }

  const db = getDatabase();
  try {
    let order = Number(display_order);
    if (isNaN(order) || order <= 0) {
      const maxOrderRow = db.prepare('SELECT MAX(display_order) as max_ord FROM brands').get() as { max_ord: number | null };
      order = (maxOrderRow?.max_ord ?? 0) + 1;
    }

    const stmt = db.prepare('INSERT INTO brands (name, display_order, active) VALUES (?, ?, 1)');
    const result = stmt.run(name.trim(), order);
    const newBrand = db.prepare('SELECT * FROM brands WHERE id = ?').get(Number(result.lastInsertRowid)) as unknown as Brand;
    res.status(201).json({ brand: newBrand });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to create brand.' });
  }
});

// PUT /api/brands/:id - update brand
router.put('/:id', (req, res) => {
  const id = Number(req.params.id);
  const { name, display_order, active } = req.body;

  if (isNaN(id)) {
    res.status(400).json({ error: 'Invalid brand ID.' });
    return;
  }

  const db = getDatabase();
  const existing = db.prepare('SELECT * FROM brands WHERE id = ?').get(id) as unknown as Brand | undefined;
  if (!existing) {
    res.status(404).json({ error: 'Brand not found.' });
    return;
  }

  const updatedName = name !== undefined ? String(name).trim() : existing.name;
  const updatedOrder = display_order !== undefined ? Number(display_order) : existing.display_order;
  const updatedActive = active !== undefined ? (active ? 1 : 0) : existing.active;

  try {
    db.prepare('UPDATE brands SET name = ?, display_order = ?, active = ? WHERE id = ?').run(
      updatedName,
      updatedOrder,
      updatedActive,
      id
    );
    const updated = db.prepare('SELECT * FROM brands WHERE id = ?').get(id) as unknown as Brand;
    res.json({ brand: updated });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to update brand.' });
  }
});

// POST /api/brands/reorder - reorder multiple brands
router.post('/reorder', (req, res) => {
  const { orders } = req.body; // Array of { id: number, display_order: number }
  if (!Array.isArray(orders)) {
    res.status(400).json({ error: 'orders array is required.' });
    return;
  }

  const db = getDatabase();
  try {
    runTransaction(db, () => {
      const updateStmt = db.prepare('UPDATE brands SET display_order = ? WHERE id = ?');
      for (const item of orders) {
        updateStmt.run(Number(item.display_order), Number(item.id));
      }
    });
    const brands = db.prepare('SELECT * FROM brands ORDER BY display_order ASC, id ASC').all() as unknown as Brand[];
    res.json({ success: true, brands });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to reorder brands.' });
  }
});

export default router;
