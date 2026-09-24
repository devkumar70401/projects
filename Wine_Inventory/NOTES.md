# Project Engineering Notes & Assumption Log

### Project: Liquor Inventory Manager (Web Edition)
### Client: Family-run Liquor Shop Business (3 Shops, ~35 Brands, 3 Bottle Sizes: 180ml, 375ml, 750ml)

---

## 1. Architectural Decisions

1. **Monolithic Single-Service Structure**:
   - Backend: Node.js (v24 LTS) + Express + TypeScript + SQLite (`better-sqlite3`).
   - Frontend: React 18 / Vite + TypeScript + Tailwind CSS + TanStack Table + Recharts.
   - In production, Express statically serves the built Vite frontend SPA (`client/dist`) with an API router mounted under `/api`.

2. **Database Schema & Immutability**:
   - Master tables: `shops`, `brands`, `sizes`, `rates`, `opening_stock`, `users`.
   - Transactional table: `inventory_records`.
   - `inventory_records` stores snapshot copies of `rate` and computed `value` for historical integrity. Any subsequent price or master data change will not alter past records.
   - Deletions of shops/brands are soft (`active = 0`) to preserve historical foreign key integrity.

3. **Formula & Calculation Logic**:
   - `total` (Yog) = `previous_balance` (Pichla Shesh) + `aamad` (Stock received)
   - `remaining` (Shesh) = `total` - `bikri` (Quantity sold)
   - Validation rule: `bikri <= total`. If `bikri > total`, the entire sheet save transaction is rejected with an explicit validation error identifying the offending brand and size.

4. **Accounting Assumption on Keemat (Value)**:
   - **TODO-CONFIRM (Section 8)**: Scanned paper register shows Keemat computed as `bikri * rate` on the daily sales line, with total sales at the bottom. However, inventory registers can also define stock value as `remaining * rate`.
   - Default formula implemented: `computeValue(previous, aamad, bikri, total, remaining, rate)` defaults to `remaining * rate` (stock-on-hand value) as instructed in Section 8, but isolated in a single function in `server/src/services/inventoryService.ts` and parameterized so it can be swapped to `bikri * rate` (daily sales value) with zero schema migration.
   - We also provide an explicit daily sales value metric (`bikri * rate`) in reports and dashboard aggregations.

5. **Carry Forward Logic**:
   - On selecting Date $D$, `carryForwardService` queries the latest `inventory_records` for $(Shop, Brand, Size)$ where `record_date < D`.
   - If no prior record exists, it falls back to `opening_stock`.
   - If no opening stock exists, returns `null` (UI flags "Opening stock not set" and blocks saving until opening stock is initialized).
   - Never silently assumes 0.

6. **Devanagari / Typography**:
   - Self-hosted / loaded Noto Sans Devanagari font for consistent Hindi rendering across all platforms (Linux/Windows/Mac/Mobile browsers).

