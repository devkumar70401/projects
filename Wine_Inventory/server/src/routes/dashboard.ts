import { Router } from 'express';
import { getDatabase } from '../db/connection.js';
import { ReportService } from '../services/reportService.js';

const router = Router();

// GET /api/dashboard - get operational metrics and charts
router.get('/', (req, res) => {
  const db = getDatabase();
  const date = req.query.date as string | undefined;
  const shopId = req.query.shop_id ? Number(req.query.shop_id) : undefined;

  try {
    const summary = ReportService.getDashboardSummary(db, date, shopId);
    res.json({ summary });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to fetch dashboard metrics.' });
  }
});

export default router;
