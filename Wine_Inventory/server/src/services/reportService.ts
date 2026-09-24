import { DatabaseSync } from 'node:sqlite';
import ExcelJS from 'exceljs';
import PdfPrinter from 'pdfmake';
import { TDocumentDefinitions } from 'pdfmake/interfaces.js';

export interface DashboardSummary {
  date: string;
  total_sales_value: number;
  total_stock_value: number;
  total_aamad_qty: number;
  total_bikri_qty: number;
  top_selling_brands: { brand_name: string; total_bikri: number; sales_value: number }[];
  low_stock_items: { shop_name: string; brand_name: string; volume_ml: number; remaining: number; rate: number }[];
  shop_sales: { shop_id: number; shop_name: string; total_sales_value: number; total_bikri_qty: number }[];
  sales_trend: { date: string; sales_value: number; bikri_qty: number }[];
}

export class ReportService {
  static getDashboardSummary(db: DatabaseSync, date?: string, shopId?: number): DashboardSummary {
    const targetDate = date || new Date().toISOString().split('T')[0];

    // 1. Today's sales value & quantities
    let todaySql = `
      SELECT 
        COALESCE(SUM(bikri * rate), 0) as total_sales_value,
        COALESCE(SUM(aamad), 0) as total_aamad_qty,
        COALESCE(SUM(bikri), 0) as total_bikri_qty
      FROM inventory_records
      WHERE record_date = ?
    `;
    const todayParams: any[] = [targetDate];
    if (shopId) {
      todaySql += ' AND shop_id = ?';
      todayParams.push(shopId);
    }
    const todayStats = db.prepare(todaySql).get(...todayParams) as {
      total_sales_value: number;
      total_aamad_qty: number;
      total_bikri_qty: number;
    };

    // 2. Current stock on hand value (latest records per shop, brand, size)
    let stockSql = `
      SELECT COALESCE(SUM(ir.remaining * ir.rate), 0) as total_stock_value
      FROM inventory_records ir
      INNER JOIN (
        SELECT shop_id, brand_id, size_id, MAX(record_date) as max_date
        FROM inventory_records
        WHERE record_date <= ?
        ${shopId ? 'AND shop_id = ' + shopId : ''}
        GROUP BY shop_id, brand_id, size_id
      ) latest ON ir.shop_id = latest.shop_id 
              AND ir.brand_id = latest.brand_id 
              AND ir.size_id = latest.size_id 
              AND ir.record_date = latest.max_date
    `;
    const stockStats = db.prepare(stockSql).get(targetDate) as { total_stock_value: number };

    // 3. Top selling brands (last 30 days or specified date)
    let topBrandsSql = `
      SELECT b.name as brand_name,
             COALESCE(SUM(ir.bikri), 0) as total_bikri,
             COALESCE(SUM(ir.bikri * ir.rate), 0) as sales_value
      FROM inventory_records ir
      JOIN brands b ON ir.brand_id = b.id
      WHERE ir.record_date <= ? AND ir.record_date >= date(?, '-30 days')
      ${shopId ? 'AND ir.shop_id = ' + shopId : ''}
      GROUP BY b.id, b.name
      HAVING total_bikri > 0
      ORDER BY total_bikri DESC
      LIMIT 10
    `;
    const topBrands = db.prepare(topBrandsSql).all(targetDate, targetDate) as unknown as {
      brand_name: string;
      total_bikri: number;
      sales_value: number;
    }[];

    // 4. Low stock items (remaining <= 5 from most recent record)
    let lowStockSql = `
      SELECT s.name as shop_name, b.name as brand_name, sz.volume_ml, ir.remaining, ir.rate
      FROM inventory_records ir
      JOIN shops s ON ir.shop_id = s.id
      JOIN brands b ON ir.brand_id = b.id
      JOIN sizes sz ON ir.size_id = sz.id
      INNER JOIN (
        SELECT shop_id, brand_id, size_id, MAX(record_date) as max_date
        FROM inventory_records
        WHERE record_date <= ?
        ${shopId ? 'AND shop_id = ' + shopId : ''}
        GROUP BY shop_id, brand_id, size_id
      ) latest ON ir.shop_id = latest.shop_id 
              AND ir.brand_id = latest.brand_id 
              AND ir.size_id = latest.size_id 
              AND ir.record_date = latest.max_date
      WHERE ir.remaining <= 10
      ORDER BY ir.remaining ASC, b.display_order ASC
      LIMIT 15
    `;
    const lowStock = db.prepare(lowStockSql).all(targetDate) as unknown as {
      shop_name: string;
      brand_name: string;
      volume_ml: number;
      remaining: number;
      rate: number;
    }[];

    // 5. Shop-wise sales comparison
    const shopSalesSql = `
      SELECT s.id as shop_id, s.name as shop_name,
             COALESCE(SUM(ir.bikri * ir.rate), 0) as total_sales_value,
             COALESCE(SUM(ir.bikri), 0) as total_bikri_qty
      FROM shops s
      LEFT JOIN inventory_records ir ON s.id = ir.shop_id AND ir.record_date = ?
      WHERE s.active = 1
      GROUP BY s.id, s.name
      ORDER BY s.id ASC
    `;
    const shopSales = db.prepare(shopSalesSql).all(targetDate) as unknown as {
      shop_id: number;
      shop_name: string;
      total_sales_value: number;
      total_bikri_qty: number;
    }[];

    // 6. Sales trend (last 30 days)
    let trendSql = `
      SELECT record_date as date,
             COALESCE(SUM(bikri * rate), 0) as sales_value,
             COALESCE(SUM(bikri), 0) as bikri_qty
      FROM inventory_records
      WHERE record_date <= ? AND record_date >= date(?, '-30 days')
      ${shopId ? 'AND shop_id = ' + shopId : ''}
      GROUP BY record_date
      ORDER BY record_date ASC
    `;
    const salesTrend = db.prepare(trendSql).all(targetDate, targetDate) as unknown as {
      date: string;
      sales_value: number;
      bikri_qty: number;
    }[];

    return {
      date: targetDate,
      total_sales_value: todayStats?.total_sales_value ?? 0,
      total_stock_value: stockStats?.total_stock_value ?? 0,
      total_aamad_qty: todayStats?.total_aamad_qty ?? 0,
      total_bikri_qty: todayStats?.total_bikri_qty ?? 0,
      top_selling_brands: topBrands,
      low_stock_items: lowStock,
      shop_sales: shopSales,
      sales_trend: salesTrend,
    };
  }

  /**
   * Generates an Excel workbook for the filtered records
   */
  static async generateExcelReport(records: any[]): Promise<Buffer> {
    const workbook = new ExcelJS.Workbook();
    workbook.creator = 'Liquor Inventory Manager';
    workbook.created = new Date();

    const sheet = workbook.addWorksheet('दैनिक स्टॉक रजिस्टर');

    // Setup columns
    sheet.columns = [
      { header: 'दिनांक (Date)', key: 'record_date', width: 14 },
      { header: 'दुकान (Shop)', key: 'shop_name', width: 22 },
      { header: 'ब्रांड (Brand)', key: 'brand_name', width: 30 },
      { header: 'साइज़ (Size)', key: 'volume_ml', width: 12 },
      { header: 'पिछला शेष (Opening)', key: 'previous_balance', width: 18 },
      { header: 'आमद (Aamad)', key: 'aamad', width: 14 },
      { header: 'योग (Total)', key: 'total', width: 14 },
      { header: 'बिक्री (Bikri)', key: 'bikri', width: 14 },
      { header: 'शेष (Closing)', key: 'remaining', width: 16 },
      { header: 'दर (Rate)', key: 'rate', width: 14 },
      { header: 'स्टॉक कीमत (Stock Value)', key: 'value', width: 22 },
      { header: 'बिक्री कीमत (Sales Value)', key: 'sales_value', width: 22 },
    ];

    // Style header row
    const headerRow = sheet.getRow(1);
    headerRow.font = { bold: true, color: { argb: 'FFFFFFFF' } };
    headerRow.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF1E293B' }, // Slate-800
    };
    headerRow.alignment = { horizontal: 'center', vertical: 'middle' };

    // Add rows
    records.forEach(r => {
      const salesVal = (r.bikri || 0) * (r.rate || 0);
      const row = sheet.addRow({
        record_date: r.record_date,
        shop_name: r.shop_name,
        brand_name: r.brand_name,
        volume_ml: `${r.volume_ml} ml`,
        previous_balance: r.previous_balance,
        aamad: r.aamad,
        total: r.total,
        bikri: r.bikri,
        remaining: r.remaining,
        rate: r.rate,
        value: r.value,
        sales_value: salesVal,
      });

      // Color coding styling on specific cells
      row.getCell('previous_balance').font = { color: { argb: 'FFDC2626' } }; // Red
      row.getCell('remaining').font = { color: { argb: 'FFDC2626' } }; // Red
      row.getCell('aamad').font = { color: { argb: 'FF2563EB' } }; // Blue
      row.getCell('bikri').font = { color: { argb: 'FF2563EB' } }; // Blue
    });

    const buffer = await workbook.xlsx.writeBuffer();
    return Buffer.from(buffer);
  }

  /**
   * Generates a CSV text string
   */
  static generateCsvReport(records: any[]): string {
    const headers = [
      'Date',
      'Shop',
      'Brand',
      'Size_ml',
      'Pichla_Shesh',
      'Aamad',
      'Yog',
      'Bikri',
      'Shesh',
      'Dar',
      'Stock_Keemat',
      'Sales_Keemat',
    ];

    const rows = records.map(r => {
      const salesVal = (r.bikri || 0) * (r.rate || 0);
      return [
        `"${r.record_date}"`,
        `"${r.shop_name}"`,
        `"${r.brand_name}"`,
        r.volume_ml,
        r.previous_balance,
        r.aamad,
        r.total,
        r.bikri,
        r.remaining,
        r.rate,
        r.value,
        salesVal,
      ].join(',');
    });

    return [headers.join(','), ...rows].join('\n');
  }

  /**
   * Generates a PDF report formatted like the physical register using pdfmake
   */
  static async generatePdfReport(
    shopName: string,
    date: string,
    sheet: any[]
  ): Promise<Buffer> {
    // Fonts configuration (Standard standard Helvetica font fallback)
    const fonts = {
      Helvetica: {
        normal: 'Helvetica',
        bold: 'Helvetica-Bold',
        italics: 'Helvetica-Oblique',
        bolditalics: 'Helvetica-BoldOblique',
      },
    };

    const printer = new PdfPrinter(fonts);

    // Build register table body
    const tableBody: any[] = [];

    // Header Row 1: Brand | 180ml | 375ml | 750ml
    tableBody.push([
      { text: 'Brand', style: 'tableHeader', rowSpan: 2, alignment: 'left' },
      { text: '180 ml', style: 'tableHeaderGroup', colSpan: 7, alignment: 'center' },
      {}, {}, {}, {}, {}, {},
      { text: '375 ml', style: 'tableHeaderGroup', colSpan: 7, alignment: 'center' },
      {}, {}, {}, {}, {}, {},
      { text: '750 ml', style: 'tableHeaderGroup', colSpan: 7, alignment: 'center' },
      {}, {}, {}, {}, {}, {},
    ]);

    // Header Row 2: sub-columns
    const subHeaders = ['P.S', 'Aam', 'Yog', 'Bik', 'She', 'Dar', 'Val'];
    const row2 = [
      '',
      ...subHeaders.map(h => ({ text: h, style: 'tableSubHeader' })),
      ...subHeaders.map(h => ({ text: h, style: 'tableSubHeader' })),
      ...subHeaders.map(h => ({ text: h, style: 'tableSubHeader' })),
    ];
    tableBody.push(row2);

    // Data rows
    let grandTotalSales = 0;
    let grandTotalStockVal = 0;

    sheet.forEach(brandRow => {
      const rowCells: any[] = [
        { text: brandRow.brand_name.replace(/\s*\(.*?\)/g, ''), fontSize: 7, bold: true },
      ];

      [180, 375, 750].forEach(vol => {
        const cell = brandRow.sizes?.[vol] || {};
        const pShesh = cell.previous_balance !== null ? cell.previous_balance : '-';
        const aamad = cell.aamad || 0;
        const total = cell.total || 0;
        const bikri = cell.bikri || 0;
        const shesh = cell.remaining || 0;
        const dar = cell.rate || 0;
        const val = cell.value || 0;

        grandTotalSales += bikri * dar;
        grandTotalStockVal += val;

        rowCells.push(
          { text: String(pShesh), color: '#dc2626', fontSize: 6.5, alignment: 'right' },
          { text: String(aamad), color: '#2563eb', fontSize: 6.5, alignment: 'right' },
          { text: String(total), color: '#475569', fontSize: 6.5, alignment: 'right' },
          { text: String(bikri), color: '#2563eb', fontSize: 6.5, alignment: 'right' },
          { text: String(shesh), color: '#dc2626', fontSize: 6.5, alignment: 'right' },
          { text: String(dar), fontSize: 6.5, alignment: 'right' },
          { text: String(val), fontSize: 6.5, alignment: 'right' }
        );
      });

      tableBody.push(rowCells);
    });

    const docDefinition: TDocumentDefinitions = {
      pageOrientation: 'landscape',
      pageSize: 'A4',
      pageMargins: [20, 20, 20, 20],
      defaultStyle: {
        font: 'Helvetica',
        fontSize: 8,
      },
      content: [
        {
          columns: [
            { text: `Daily Stock Register / दैनिक स्टॉक रजिस्टर`, fontSize: 13, bold: true },
            { text: `Shop: ${shopName} | Date: ${date}`, alignment: 'right', fontSize: 10, bold: true },
          ],
          margin: [0, 0, 0, 10],
        },
        {
          table: {
            headerRows: 2,
            widths: [
              80,
              18, 18, 18, 18, 18, 20, 26,
              18, 18, 18, 18, 18, 20, 26,
              18, 18, 18, 18, 18, 20, 26,
            ],
            body: tableBody,
          },
          layout: {
            hLineWidth: (i: number) => (i === 0 || i === 2 ? 1 : 0.5),
            vLineWidth: () => 0.5,
            hLineColor: () => '#cbd5e1',
            vLineColor: () => '#cbd5e1',
            paddingLeft: () => 2,
            paddingRight: () => 2,
            paddingTop: () => 2,
            paddingBottom: () => 2,
          },
        },
        {
          columns: [
            { text: `Total Stock Value: Rs. ${grandTotalStockVal.toLocaleString()}`, bold: true, fontSize: 9 },
            { text: `Total Today Sales: Rs. ${grandTotalSales.toLocaleString()}`, bold: true, alignment: 'right', fontSize: 9 },
          ],
          margin: [0, 10, 0, 0],
        },
      ],
      styles: {
        tableHeader: {
          bold: true,
          fontSize: 7.5,
          color: '#0f172a',
          fillColor: '#f1f5f9',
        },
        tableHeaderGroup: {
          bold: true,
          fontSize: 7.5,
          color: '#0f172a',
          fillColor: '#e2e8f0',
        },
        tableSubHeader: {
          bold: true,
          fontSize: 6.5,
          color: '#475569',
          alignment: 'center',
          fillColor: '#f8fafc',
        },
      },
    };

    const pdfDoc = printer.createPdfKitDocument(docDefinition);
    return new Promise((resolve, reject) => {
      const chunks: Uint8Array[] = [];
      pdfDoc.on('data', chunk => chunks.push(chunk));
      pdfDoc.on('end', () => resolve(Buffer.concat(chunks)));
      pdfDoc.on('error', reject);
      pdfDoc.end();
    });
  }
}
