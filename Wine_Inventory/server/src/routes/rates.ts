import { Router } from 'express';
import { getDatabase } from '../db/connection.js';
import { RateService } from '../services/rateService.js';

const router = Router();

// GET /api/rates - get grid of all brands x sizes current rates
router.get('/', (req, res) => {
  const db = getDatabase();
  const date = req.query.date as string | undefined;

  const brands = db.prepare('SELECT id, name, display_order, active FROM brands WHERE active = 1 ORDER BY display_order ASC').all() as any[];
  const sizes = db.prepare('SELECT id, volume_ml FROM sizes ORDER BY volume_ml ASC').all() as any[];
  const currentRates = RateService.getAllCurrentRates(db, date);

  const grid = brands.map(b => {
    const sizeRates: { [volume_ml: number]: number } = {};
    sizes.forEach(s => {
      sizeRates[s.volume_ml] = currentRates.get(`${b.id}_${s.id}`) ?? 0;
    });
    return {
      brand_id: b.id,
      brand_name: b.name,
      display_order: b.display_order,
      rates: sizeRates,
    };
  });

  res.json({ brands, sizes, grid });
});

// POST /api/rates - set rate for brand x size
router.post('/', (req, res) => {
  const { brand_id, size_id, rate, effective_from } = req.body;
  if (!brand_id || !size_id || rate === undefined) {
    res.status(400).json({ error: 'brand_id, size_id, and rate are required.' });
    return;
  }

  const db = getDatabase();
  try {
    RateService.setRate(db, Number(brand_id), Number(size_id), Number(rate), effective_from);
    res.json({
      success: true,
      message: 'Rate updated successfully (recorded in rate history).',
      brand_id,
      size_id,
      rate: Number(rate),
      effective_from: effective_from || new Date().toISOString().split('T')[0],
    });
  } catch (err: any) {
    res.status(400).json({ error: err.message || 'Failed to update rate.' });
  }
});

// GET /api/rates/history/:brand_id/:size_id
router.get('/history/:brand_id/:size_id', (req, res) => {
  const brandId = Number(req.params.brand_id);
  const sizeId = Number(req.params.size_id);
  const db = getDatabase();

  const history = db.prepare(`
    SELECT r.*, b.name as brand_name, s.volume_ml
    FROM rates r
    JOIN brands b ON r.brand_id = b.id
    JOIN sizes s ON r.size_id = s.id
    WHERE r.brand_id = ? AND r.size_id = ?
    ORDER BY r.effective_from DESC, r.id DESC
  `).all(brandId, sizeId);

  res.json({ history });
});

export default router;
