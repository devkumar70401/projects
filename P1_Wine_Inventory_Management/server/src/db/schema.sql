CREATE TABLE IF NOT EXISTS shops (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS brands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS sizes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    volume_ml INTEGER NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS rates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    brand_id INTEGER NOT NULL REFERENCES brands(id),
    size_id INTEGER NOT NULL REFERENCES sizes(id),
    rate REAL NOT NULL CHECK (rate >= 0),
    effective_from TEXT NOT NULL DEFAULT (date('now')),
    UNIQUE(brand_id, size_id, effective_from)
);

CREATE TABLE IF NOT EXISTS opening_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id INTEGER NOT NULL REFERENCES shops(id),
    brand_id INTEGER NOT NULL REFERENCES brands(id),
    size_id INTEGER NOT NULL REFERENCES sizes(id),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    as_of_date TEXT NOT NULL,
    UNIQUE(shop_id, brand_id, size_id)
);

CREATE TABLE IF NOT EXISTS inventory_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    record_date TEXT NOT NULL,          -- 'YYYY-MM-DD'
    shop_id INTEGER NOT NULL REFERENCES shops(id),
    brand_id INTEGER NOT NULL REFERENCES brands(id),
    size_id INTEGER NOT NULL REFERENCES sizes(id),
    previous_balance INTEGER NOT NULL CHECK (previous_balance >= 0),
    aamad INTEGER NOT NULL DEFAULT 0 CHECK (aamad >= 0),
    total INTEGER NOT NULL,             -- = previous_balance + aamad
    bikri INTEGER NOT NULL DEFAULT 0 CHECK (bikri >= 0),
    remaining INTEGER NOT NULL,         -- = total - bikri
    rate REAL NOT NULL,                 -- SNAPSHOT of the rate at save time
    value REAL NOT NULL,                -- stock-on-hand value (remaining * rate)
    finalized INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(record_date, shop_id, brand_id, size_id)
);

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'owner', -- 'owner' | 'staff'
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_lookup ON inventory_records(shop_id, brand_id, size_id, record_date);
CREATE INDEX IF NOT EXISTS idx_inventory_date ON inventory_records(record_date);
