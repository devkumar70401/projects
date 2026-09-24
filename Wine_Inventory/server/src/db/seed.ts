import bcrypt from 'bcryptjs';
import { fileURLToPath } from 'url';
import { getDatabase, runTransaction } from './connection.js';
import { runMigrations } from './migrate.js';

export function runSeed(dbPath?: string) {
  runMigrations(dbPath);
  const db = getDatabase(dbPath);

  runTransaction(db, () => {
    console.log('Seeding initial data...');

    // 1. Sizes (180, 375, 750)
    const insertSize = db.prepare('INSERT OR IGNORE INTO sizes (volume_ml) VALUES (?)');
    [180, 375, 750].forEach(size => insertSize.run(size));

    // 2. Shops
    const insertShop = db.prepare('INSERT OR IGNORE INTO shops (name, active) VALUES (?, ?)');
    const shops = [
      { name: 'S.A. वाइन शॉप (Main)', active: 1 },
      { name: 'वाइन शॉप - शाखा 2', active: 1 },
      { name: 'वाइन शॉप - शाखा 3', active: 1 }
    ];
    shops.forEach(s => insertShop.run(s.name, s.active));

    // 3. Brands (Display order preserved as per paper register)
    const brandList = [
      '100 Pipers (100 पाइपर्स)',
      'Signature Whisky (सिग्नेचर वि.)',
      'Black Dog (ब्लैक डॉग)',
      'Royal Stag (रॉयल स्टैग)',
      'Royal Challenge (रोयल चैलेंज)',
      'Imperial Blue (इम्पीरियल ब्लू)',
      "McDowell's No. 1 (मेकडावल No. 1)",
      "McDowell's Whisky (मेकडावल व्हिस्की)",
      'Green Label (ग्रीन लेबल)',
      "McDowell's Rum (मेकडावल रम)",
      "McDowell's Super Special (मेकडावल सुपर स्पेशल)",
      'Bagpiper (बैग पाइपर)',
      'Old Monk (ओल्ड मोंक)',
      'Morpheus Brandy (मोरफियस ब्रांडी)',
      'Magic Moments Vodka (मैजिक मोमेंट वोदका)',
      "McDowell's Supremo Brandy (मेकडावल सुप्रीमो ब्रांडी)",
      '8 PM Black (8 PM ब्लैक)',
      'Royal Green (रोयल ग्रीन)',
      'White & Blue (वाइट एण्ड ब्लू)',
      'Sterling Reserve B7 (स्टारलिंग-B7)',
      '8 PM Whisky (8 पी.एम.)',
      "Officer's Choice (आफिसर च्वाईस)",
      'Royal Stag Whisky (रोयल स्टेग व्हिस्की)',
      'Old Monk Rum (ओल्ड मोंक रम)',
      '8 PM Rum (8 पी.एम. रम)',
      "Officer's Choice Blue (आफिसर च्वाईस ब्लू)",
      'Inspira Whisky (इन्सपीरिया व्हिस्की)',
      'Royal Stag Barrel (रोयल स्टेग बेरल)',
      "McDowell's Choice (मेकडावल च्वॉइस)",
      'Bacardi Limon (बैकडी लिमोन)',
      'Bacardi Orange (बैकडी ओरेंज)',
      'Old Monk Gold (ओल्डमोंक गोल्ड)',
      'Blenders Pride (ब्लेंडर स्प्राइड)',
      'All Seasons (ऑल सीजन्स)',
      'Party Special (पार्टी स्पेशल)',
      'Smirnoff Vodka (स्मिरनॉफ़ वोदका)',
      'Kingfisher Can (किंगफिशर कैन)',
      'Budweiser (बुडवाइजर)',
      'Tuborg (टयूबोर्ग)',
      'Carlsberg (कार्ल्सबर्ग)'
    ];

    const insertBrand = db.prepare('INSERT OR IGNORE INTO brands (name, display_order, active) VALUES (?, ?, ?)');
    brandList.forEach((b, idx) => insertBrand.run(b, idx + 1, 1));

    // Fetch IDs
    const dbShops = db.prepare('SELECT id, name FROM shops').all() as unknown as { id: number; name: string }[];
    const dbBrands = db.prepare('SELECT id, name, display_order FROM brands').all() as unknown as { id: number; name: string; display_order: number }[];
    const dbSizes = db.prepare('SELECT id, volume_ml FROM sizes ORDER BY volume_ml ASC').all() as unknown as { id: number; volume_ml: number }[];

    // 4. Rates
    const insertRate = db.prepare("INSERT OR IGNORE INTO rates (brand_id, size_id, rate, effective_from) VALUES (?, ?, ?, date('now'))");
    
    // Sample base price mapping based on typical liquor tier
    const getBaseRate = (brandName: string, volume: number): number => {
      let base180 = 140;
      if (brandName.includes('100 Pipers') || brandName.includes('Black Dog')) base180 = 360;
      else if (brandName.includes('Signature') || brandName.includes('Blenders Pride')) base180 = 210;
      else if (brandName.includes('Royal Stag') || brandName.includes('Royal Challenge') || brandName.includes('All Seasons')) base180 = 160;
      else if (brandName.includes('Imperial Blue') || brandName.includes('Old Monk') || brandName.includes('8 PM')) base180 = 130;
      else if (brandName.includes('Officer')) base180 = 120;
      else if (brandName.includes('Kingfisher') || brandName.includes('Budweiser') || brandName.includes('Tuborg')) base180 = 150;

      if (volume === 180) return base180;
      if (volume === 375) return Math.round(base180 * 1.95 / 10) * 10;
      return Math.round(base180 * 3.8 / 10) * 10;
    };

    for (const brand of dbBrands) {
      for (const size of dbSizes) {
        const rate = getBaseRate(brand.name, size.volume_ml);
        insertRate.run(brand.id, size.id, rate);
      }
    }

    // 5. Opening Stock (as of 2026-08-01)
    const insertOpening = db.prepare('INSERT OR IGNORE INTO opening_stock (shop_id, brand_id, size_id, quantity, as_of_date) VALUES (?, ?, ?, ?, ?)');
    for (const shop of dbShops) {
      for (const brand of dbBrands) {
        for (const size of dbSizes) {
          const qty = 20 + ((brand.id * 7 + size.volume_ml + shop.id * 11) % 60);
          insertOpening.run(shop.id, brand.id, size.id, qty, '2026-08-01');
        }
      }
    }

    // 6. Admin User
    const adminPassword = process.env.SEED_ADMIN_PASSWORD || 'admin123';
    const passwordHash = bcrypt.hashSync(adminPassword, 10);
    const insertUser = db.prepare('INSERT OR IGNORE INTO users (username, password_hash, role) VALUES (?, ?, ?)');
    insertUser.run('admin', passwordHash, 'owner');

    console.log(`Seeding complete. Admin user: admin / ${adminPassword}`);
  });
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  runSeed();
}
