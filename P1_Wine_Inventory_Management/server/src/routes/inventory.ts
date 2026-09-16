import { Router } from 'express';
import { getDatabase, runTransaction } from '../db/connection.js';
import { InventoryService } from '../services/inventoryService.js';
import { SaveRegisterRowPayload } from '../models/types.js';

const router = Router();

// GET /api/inventory/sheet?shop_id=1&date=YYYY-MM-DD
router.get('/sheet', (req, res) => {
  const shopId = Number(req.query.shop_id);
  const date = (req.query.date as string) || new Date().toISOString().split('T')[0];

  if (isNaN(shopId) || shopId <= 0) {
    res.status(400).json({ error: 'Valid shop_id parameter is required.' });
    return;
  }

  const db = getDatabase();
  try {
    const sheet = InventoryService.getDailySheet(db, shopId, date);
    const shop = db.prepare('SELECT id, name FROM shops WHERE id = ?').get(shopId);
    const sizes = db.prepare('SELECT id, volume_ml FROM sizes ORDER BY volume_ml ASC').all();
    res.json({ shop, date, sizes, sheet });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to load daily sheet.' });
  }
});

// POST /api/inventory/sheet - Save whole daily sheet
router.post('/sheet', (req, res) => {
  const { shop_id, date, rows } = req.body;

  if (!shop_id || !date || !Array.isArray(rows)) {
    res.status(400).json({ error: 'shop_id, date, and rows array are required.' });
    return;
  }

  const db = getDatabase();
  const result = InventoryService.saveDailySheet(
    db,
    Number(shop_id),
    String(date),
    rows as SaveRegisterRowPayload[]
  );

  if (!result.success) {
    res.status(422).json({
      error: 'Validation failed on one or more register rows. No data was saved.',
      errors: result.errors,
    });
    return;
  }

  res.json({
    success: true,
    message: "आज का हिसाब सफलतापूर्वक सेव हो गया (Daily sheet saved successfully).",
    saved_count: result.saved_count,
  });
});

// GET /api/inventory/history
router.get('/history', (req, res) => {
  const shopId = req.query.shop_id ? Number(req.query.shop_id) : undefined;
  const brandId = req.query.brand_id ? Number(req.query.brand_id) : undefined;
  const sizeId = req.query.size_id ? Number(req.query.size_id) : undefined;
  const startDate = req.query.start_date as string | undefined;
  const endDate = req.query.end_date as string | undefined;

  const db = getDatabase();
  try {
    const records = InventoryService.getRecordHistory(db, shopId, brandId, sizeId, startDate, endDate);
    res.json({ records });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to fetch inventory history.' });
  }
});

// GET /api/inventory/opening-stock?shop_id=1
router.get('/opening-stock', (req, res) => {
  const shopId = req.query.shop_id ? Number(req.query.shop_id) : undefined;
  const db = getDatabase();

  let query = `
    SELECT os.*, b.name as brand_name, b.display_order, s.name as shop_name, sz.volume_ml
    FROM opening_stock os
    JOIN brands b ON os.brand_id = b.id
    JOIN shops s ON os.shop_id = s.id
    JOIN sizes sz ON os.size_id = sz.id
  `;
  const params: any[] = [];
  if (shopId) {
    query += ' WHERE os.shop_id = ?';
    params.push(shopId);
  }
  query += ' ORDER BY s.id ASC, b.display_order ASC, sz.volume_ml ASC';

  const items = db.prepare(query).all(...params);
  res.json({ opening_stock: items });
});

// POST /api/inventory/opening-stock - Set opening stock
router.post('/opening-stock', (req, res) => {
  const { shop_id, entries, as_of_date } = req.body;
  if (!shop_id || !Array.isArray(entries)) {
    res.status(400).json({ error: 'shop_id and entries array are required.' });
    return;
  }

  const defaultDate = as_of_date || '2026-08-01';
  const db = getDatabase();

  try {
    runTransaction(db, () => {
      const stmt = db.prepare(`
        INSERT INTO opening_stock (shop_id, brand_id, size_id, quantity, as_of_date)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(shop_id, brand_id, size_id) DO UPDATE SET
          quantity = excluded.quantity,
          as_of_date = excluded.as_of_date
      `);

      for (const entry of entries) {
        const qty = Number(entry.quantity);
        if (isNaN(qty) || qty < 0) {
          throw new Error(`Invalid opening stock quantity: ${entry.quantity}`);
        }
        stmt.run(
          Number(shop_id),
          Number(entry.brand_id),
          Number(entry.size_id),
          qty,
          entry.as_of_date || defaultDate
        );
      }
    });

    res.json({ success: true, message: 'Opening stock saved successfully.' });
  } catch (err: any) {
    res.status(400).json({ error: err.message || 'Failed to save opening stock.' });
  }
});

export default router;
