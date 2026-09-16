import { Router } from 'express';
import { getDatabase } from '../db/connection.js';
import { InventoryService } from '../services/inventoryService.js';
import { ReportService } from '../services/reportService.js';

const router = Router();

// GET /api/reports/data - get filtered historical data
router.get('/data', (req, res) => {
  const db = getDatabase();
  const shopId = req.query.shop_id ? Number(req.query.shop_id) : undefined;
  const brandId = req.query.brand_id ? Number(req.query.brand_id) : undefined;
  const sizeId = req.query.size_id ? Number(req.query.size_id) : undefined;
  const startDate = req.query.start_date as string | undefined;
  const endDate = req.query.end_date as string | undefined;

  try {
    const records = InventoryService.getRecordHistory(db, shopId, brandId, sizeId, startDate, endDate);
    res.json({ records });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to fetch report records.' });
  }
});

// GET /api/reports/export/excel
router.get('/export/excel', async (req, res) => {
  const db = getDatabase();
  const shopId = req.query.shop_id ? Number(req.query.shop_id) : undefined;
  const brandId = req.query.brand_id ? Number(req.query.brand_id) : undefined;
  const sizeId = req.query.size_id ? Number(req.query.size_id) : undefined;
  const startDate = req.query.start_date as string | undefined;
  const endDate = req.query.end_date as string | undefined;

  try {
    const records = InventoryService.getRecordHistory(db, shopId, brandId, sizeId, startDate, endDate);
    const buffer = await ReportService.generateExcelReport(records);

    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', `attachment; filename="liquor_inventory_report_${new Date().toISOString().split('T')[0]}.xlsx"`);
    res.send(buffer);
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to generate Excel report.' });
  }
});

// GET /api/reports/export/csv
router.get('/export/csv', (req, res) => {
  const db = getDatabase();
  const shopId = req.query.shop_id ? Number(req.query.shop_id) : undefined;
  const brandId = req.query.brand_id ? Number(req.query.brand_id) : undefined;
  const sizeId = req.query.size_id ? Number(req.query.size_id) : undefined;
  const startDate = req.query.start_date as string | undefined;
  const endDate = req.query.end_date as string | undefined;

  try {
    const records = InventoryService.getRecordHistory(db, shopId, brandId, sizeId, startDate, endDate);
    const csvContent = ReportService.generateCsvReport(records);

    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', `attachment; filename="liquor_inventory_report_${new Date().toISOString().split('T')[0]}.csv"`);
    res.send(csvContent);
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to generate CSV report.' });
  }
});

// GET /api/reports/export/pdf - register sheet layout PDF
router.get('/export/pdf', async (req, res) => {
  const db = getDatabase();
  const shopId = Number(req.query.shop_id) || 1;
  const date = (req.query.date as string) || new Date().toISOString().split('T')[0];

  try {
    const shop = db.prepare('SELECT name FROM shops WHERE id = ?').get(shopId) as { name: string } | undefined;
    const shopName = shop?.name || `Shop #${shopId}`;
    const sheet = InventoryService.getDailySheet(db, shopId, date);

    const pdfBuffer = await ReportService.generatePdfReport(shopName, date, sheet);

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="register_sheet_${date}.pdf"`);
    res.send(pdfBuffer);
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to generate PDF report.' });
  }
});

export default router;
