import { useState, useEffect } from 'react';
import { Shop, Brand } from '../types/index.js';
import { shopsApi, brandsApi, ratesApi, inventoryApi } from '../api/client.js';
import toast from 'react-hot-toast';
import {
  Store,
  Wine,
  IndianRupee,
  PackagePlus,
  Plus,
  Edit2,
  ArrowUp,
  ArrowDown,
  Save,
  CheckCircle2,
  Power,
} from 'lucide-react';

export default function MasterData() {
  const [activeTab, setActiveTab] = useState<'shops' | 'brands' | 'rates' | 'opening_stock'>('shops');

  // Shops state
  const [shops, setShops] = useState<Shop[]>([]);
  const [newShopName, setNewShopName] = useState('');
  const [editingShop, setEditingShop] = useState<{ id: number; name: string } | null>(null);

  // Brands state
  const [brands, setBrands] = useState<Brand[]>([]);
  const [newBrandName, setNewBrandName] = useState('');
  const [editingBrand, setEditingBrand] = useState<{ id: number; name: string } | null>(null);

  // Rates state
  const [ratesGrid, setRatesGrid] = useState<any[]>([]);
  const [rateSizes, setRateSizes] = useState<any[]>([]);
  const [editingRate, setEditingRate] = useState<{ brand_id: number; brand_name: string; size_id: number; volume_ml: number; current_rate: number; new_rate: number } | null>(null);

  // Opening Stock state
  const [selectedStockShopId, setSelectedStockShopId] = useState<number>(1);
  const [stockEntries, setStockEntries] = useState<any[]>([]);
  const [stockDate, setStockDate] = useState('2026-08-01');
  const [savingStock, setSavingStock] = useState(false);

  const [loading, setLoading] = useState(false);

  // Fetch functions
  const loadShops = async () => {
    try {
      const res = await shopsApi.getShops(true);
      setShops(res.shops);
      if (res.shops.length > 0 && !selectedStockShopId) {
        setSelectedStockShopId(res.shops[0].id);
      }
    } catch (err: any) {
      toast.error(err.message || 'दुकानें लोड करने में असमर्थ');
    }
  };

  const loadBrands = async () => {
    try {
      const res = await brandsApi.getBrands(true);
      setBrands(res.brands);
    } catch (err: any) {
      toast.error(err.message || 'ब्रांड्स लोड करने में असमर्थ');
    }
  };

  const loadRates = async () => {
    try {
      const res = await ratesApi.getRates();
      setRatesGrid(res.grid);
      setRateSizes(res.sizes);
    } catch (err: any) {
      toast.error(err.message || 'दरें लोड करने में असमर्थ');
    }
  };

  const loadOpeningStock = async (shopId: number) => {
    try {
      const res = await inventoryApi.getOpeningStock(shopId);
      setStockEntries(res.opening_stock);
    } catch (err: any) {
      toast.error(err.message || 'आरंभिक स्टॉक लोड करने में असमर्थ');
    }
  };

  useEffect(() => {
    setLoading(true);
    Promise.all([loadShops(), loadBrands(), loadRates()]).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (activeTab === 'opening_stock' && selectedStockShopId) {
      loadOpeningStock(selectedStockShopId);
    }
  }, [activeTab, selectedStockShopId]);

  // Shop Handlers
  const handleAddShop = async () => {
    if (!newShopName.trim()) return;
    try {
      await shopsApi.createShop(newShopName.trim());
      setNewShopName('');
      toast.success('नई दुकान जुड़ गई');
      loadShops();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  const handleUpdateShop = async () => {
    if (!editingShop || !editingShop.name.trim()) return;
    try {
      await shopsApi.updateShop(editingShop.id, { name: editingShop.name.trim() });
      setEditingShop(null);
      toast.success('दुकान का नाम अपडेट हो गया');
      loadShops();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  const handleToggleShopActive = async (shop: Shop) => {
    try {
      await shopsApi.updateShop(shop.id, { active: shop.active ? 0 : 1 });
      toast.success(shop.active ? 'दुकान निष्क्रिय कर दी गई' : 'दुकान सक्रिय कर दी गई');
      loadShops();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  // Brand Handlers
  const handleAddBrand = async () => {
    if (!newBrandName.trim()) return;
    try {
      await brandsApi.createBrand(newBrandName.trim());
      setNewBrandName('');
      toast.success('नया ब्रांड जुड़ गया');
      loadBrands();
      loadRates();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  const handleUpdateBrand = async () => {
    if (!editingBrand || !editingBrand.name.trim()) return;
    try {
      await brandsApi.updateBrand(editingBrand.id, { name: editingBrand.name.trim() });
      setEditingBrand(null);
      toast.success('ब्रांड का नाम अपडेट हो गया');
      loadBrands();
      loadRates();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  const handleToggleBrandActive = async (brand: Brand) => {
    try {
      await brandsApi.updateBrand(brand.id, { active: brand.active ? 0 : 1 });
      toast.success(brand.active ? 'ब्रांड निष्क्रिय कर दिया गया' : 'ब्रांड सक्रिय कर दिया गया');
      loadBrands();
      loadRates();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  const handleMoveBrand = async (index: number, direction: 'up' | 'down') => {
    const targetIdx = direction === 'up' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= brands.length) return;

    const currentBrand = brands[index];
    const targetBrand = brands[targetIdx];

    const newOrders = [
      { id: currentBrand.id, display_order: targetBrand.display_order },
      { id: targetBrand.id, display_order: currentBrand.display_order },
    ];

    try {
      await brandsApi.reorder(newOrders);
      loadBrands();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  // Rate Update Handler
  const handleSaveRate = async () => {
    if (!editingRate || editingRate.new_rate < 0) {
      toast.error('वैध दर दर्ज करें');
      return;
    }

    try {
      await ratesApi.setRate(
        editingRate.brand_id,
        editingRate.size_id,
        editingRate.new_rate,
        new Date().toISOString().split('T')[0]
      );
      toast.success('दर अपडेट हो गई (इतिहास सुरक्षित)');
      setEditingRate(null);
      loadRates();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  // Opening Stock Save Handler
  const handleSaveOpeningStock = async () => {
    setSavingStock(true);
    try {
      const payloadEntries = stockEntries.map(e => ({
        brand_id: e.brand_id,
        size_id: e.size_id,
        quantity: Number(e.quantity) || 0,
        as_of_date: stockDate,
      }));

      await inventoryApi.saveOpeningStock(selectedStockShopId, payloadEntries, stockDate);
      toast.success('आरंभिक स्टॉक सफलतापूर्वक सेव हो गया!', {
        icon: <CheckCircle2 className="w-5 h-5 text-emerald-600" />,
      });
      loadOpeningStock(selectedStockShopId);
    } catch (err: any) {
      toast.error(err.message || 'आरंभिक स्टॉक सेव करने में असमर्थ');
    } finally {
      setSavingStock(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
        <h1 className="text-xl font-extrabold text-slate-800 font-devanagari">
          मास्टर डेटा प्रबंधन (Master Data Configuration)
        </h1>
        <p className="text-xs text-slate-500 mt-0.5">
          दुकानें, ब्रांड्स, बोतल दरें, एवं आरंभिक स्टॉक का प्रबंधन करें।
        </p>

        {/* Tab Navigation */}
        <div className="flex flex-wrap gap-2 mt-4 border-b border-slate-200 pb-2">
          <button
            onClick={() => setActiveTab('shops')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-semibold transition-colors font-devanagari ${
              activeTab === 'shops'
                ? 'bg-red-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <Store className="w-4 h-4" />
            <span>दुकानें (Shops)</span>
          </button>

          <button
            onClick={() => setActiveTab('brands')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-semibold transition-colors font-devanagari ${
              activeTab === 'brands'
                ? 'bg-red-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <Wine className="w-4 h-4" />
            <span>ब्रांड्स क्रम व सूची (Brands)</span>
          </button>

          <button
            onClick={() => setActiveTab('rates')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-semibold transition-colors font-devanagari ${
              activeTab === 'rates'
                ? 'bg-red-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <IndianRupee className="w-4 h-4" />
            <span>दर कॉन्फ़िगरेशन (Rates)</span>
          </button>

          <button
            onClick={() => setActiveTab('opening_stock')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-semibold transition-colors font-devanagari ${
              activeTab === 'opening_stock'
                ? 'bg-red-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <PackagePlus className="w-4 h-4" />
            <span>आरंभिक स्टॉक सेटअप (Opening Stock)</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="bg-white rounded-xl p-12 text-center text-slate-500 font-devanagari">
          डेटा लोड हो रहा है...
        </div>
      ) : (
        <>
          {/* TAB 1: SHOPS */}
          {activeTab === 'shops' && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 space-y-4">
              <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
                <h2 className="text-base font-bold text-slate-800 font-devanagari">
                  दुकानों की सूची ({shops.length})
                </h2>
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={newShopName}
                    onChange={e => setNewShopName(e.target.value)}
                    placeholder="नई दुकान का नाम (Shop Name)"
                    className="border border-slate-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-red-600 font-devanagari"
                  />
                  <button
                    onClick={handleAddShop}
                    className="flex items-center space-x-1 bg-red-600 hover:bg-red-700 text-white px-3 py-1.5 rounded-lg text-xs font-semibold font-devanagari transition-colors"
                  >
                    <Plus className="w-4 h-4" />
                    <span>जोड़ें</span>
                  </button>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm border-collapse">
                  <thead>
                    <tr className="bg-slate-100 text-slate-700 font-devanagari text-xs uppercase border-b border-slate-200">
                      <th className="px-4 py-2">ID</th>
                      <th className="px-4 py-2">दुकान का नाम (Shop Name)</th>
                      <th className="px-4 py-2">स्थिति (Status)</th>
                      <th className="px-4 py-2 text-right">क्रियाएं (Actions)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {shops.map(s => (
                      <tr key={s.id} className="hover:bg-slate-50">
                        <td className="px-4 py-2.5 font-mono text-xs text-slate-500">{s.id}</td>
                        <td className="px-4 py-2.5 font-semibold text-slate-800 font-devanagari">
                          {editingShop?.id === s.id ? (
                            <div className="flex items-center space-x-2">
                              <input
                                type="text"
                                value={editingShop.name}
                                onChange={e => setEditingShop({ ...editingShop, name: e.target.value })}
                                className="border border-slate-300 rounded px-2 py-1 text-sm font-devanagari"
                              />
                              <button
                                onClick={handleUpdateShop}
                                className="bg-emerald-600 text-white px-2 py-1 rounded text-xs"
                              >
                                सेव
                              </button>
                              <button
                                onClick={() => setEditingShop(null)}
                                className="bg-slate-300 text-slate-700 px-2 py-1 rounded text-xs"
                              >
                                रद्द
                              </button>
                            </div>
                          ) : (
                            s.name
                          )}
                        </td>
                        <td className="px-4 py-2.5">
                          <span
                            className={`px-2 py-0.5 rounded text-xs font-semibold font-devanagari ${
                              s.active
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-200 text-slate-600'
                            }`}
                          >
                            {s.active ? 'सक्रिय (Active)' : 'निष्क्रिय (Inactive)'}
                          </span>
                        </td>
                        <td className="px-4 py-2.5 text-right space-x-2">
                          <button
                            onClick={() => setEditingShop({ id: s.id, name: s.name })}
                            title="नाम बदलें"
                            className="p-1.5 text-slate-600 hover:bg-slate-200 rounded"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleToggleShopActive(s)}
                            title={s.active ? 'निष्क्रिय करें' : 'सक्रिय करें'}
                            className={`p-1.5 rounded transition-colors ${
                              s.active
                                ? 'text-red-600 hover:bg-red-50'
                                : 'text-emerald-600 hover:bg-emerald-50'
                            }`}
                          >
                            <Power className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 2: BRANDS */}
          {activeTab === 'brands' && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 space-y-4">
              <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
                <div>
                  <h2 className="text-base font-bold text-slate-800 font-devanagari">
                    ब्रांड्स सूची व प्रदर्शन क्रम ({brands.length})
                  </h2>
                  <p className="text-xs text-slate-500">
                    रजिस्टर तालिका में ब्रांड्स इसी क्रम (Top-to-Bottom) में दिखाई देंगे।
                  </p>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={newBrandName}
                    onChange={e => setNewBrandName(e.target.value)}
                    placeholder="नया ब्रांड (उदा. Signature Whisky)"
                    className="border border-slate-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-red-600 font-devanagari"
                  />
                  <button
                    onClick={handleAddBrand}
                    className="flex items-center space-x-1 bg-red-600 hover:bg-red-700 text-white px-3 py-1.5 rounded-lg text-xs font-semibold font-devanagari transition-colors"
                  >
                    <Plus className="w-4 h-4" />
                    <span>जोड़ें</span>
                  </button>
                </div>
              </div>

              <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
                <table className="w-full text-left text-sm border-collapse">
                  <thead className="sticky top-0 bg-slate-100 z-10">
                    <tr className="text-slate-700 font-devanagari text-xs uppercase border-b border-slate-200">
                      <th className="px-3 py-2 w-16">क्रम</th>
                      <th className="px-4 py-2">ब्रांड का नाम (Brand Name)</th>
                      <th className="px-3 py-2 w-24">स्थिति</th>
                      <th className="px-4 py-2 text-right w-44">क्रम बदलें / क्रियाएं</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {brands.map((b, idx) => (
                      <tr key={b.id} className="hover:bg-slate-50">
                        <td className="px-3 py-2 font-mono text-xs font-bold text-slate-500">
                          {idx + 1}
                        </td>
                        <td className="px-4 py-2 font-medium text-slate-900 font-devanagari">
                          {editingBrand?.id === b.id ? (
                            <div className="flex items-center space-x-2">
                              <input
                                type="text"
                                value={editingBrand.name}
                                onChange={e => setEditingBrand({ ...editingBrand, name: e.target.value })}
                                className="border border-slate-300 rounded px-2 py-1 text-sm font-devanagari w-72"
                              />
                              <button
                                onClick={handleUpdateBrand}
                                className="bg-emerald-600 text-white px-2 py-1 rounded text-xs"
                              >
                                सेव
                              </button>
                              <button
                                onClick={() => setEditingBrand(null)}
                                className="bg-slate-300 text-slate-700 px-2 py-1 rounded text-xs"
                              >
                                रद्द
                              </button>
                            </div>
                          ) : (
                            b.name
                          )}
                        </td>
                        <td className="px-3 py-2">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-semibold font-devanagari ${
                              b.active
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-200 text-slate-600'
                            }`}
                          >
                            {b.active ? 'सक्रिय' : 'निष्क्रिय'}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-right space-x-1">
                          <button
                            onClick={() => handleMoveBrand(idx, 'up')}
                            disabled={idx === 0}
                            title="ऊपर ले जाएं"
                            className="p-1 text-slate-600 hover:bg-slate-200 rounded disabled:opacity-20"
                          >
                            <ArrowUp className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleMoveBrand(idx, 'down')}
                            disabled={idx === brands.length - 1}
                            title="नीचे ले जाएं"
                            className="p-1 text-slate-600 hover:bg-slate-200 rounded disabled:opacity-20"
                          >
                            <ArrowDown className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => setEditingBrand({ id: b.id, name: b.name })}
                            title="संशोधन"
                            className="p-1 text-slate-600 hover:bg-slate-200 rounded"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleToggleBrandActive(b)}
                            title={b.active ? 'निष्क्रिय करें' : 'सक्रिय करें'}
                            className={`p-1 rounded transition-colors ${
                              b.active
                                ? 'text-red-600 hover:bg-red-50'
                                : 'text-emerald-600 hover:bg-emerald-50'
                            }`}
                          >
                            <Power className="w-3.5 h-3.5" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 3: RATES CONFIGURATION */}
          {activeTab === 'rates' && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 space-y-4">
              <div>
                <h2 className="text-base font-bold text-slate-800 font-devanagari">
                  बोतल दर तालिका (Current Rates by Brand & Size)
                </h2>
                <p className="text-xs text-slate-500">
                  किसी भी दर पर क्लिक करके उसे अपडेट करें। ऐतिहासिक रिकॉर्ड की दर सुरक्षित रहती है।
                </p>
              </div>

              {/* Edit Rate Modal / Box */}
              {editingRate && (
                <div className="p-4 bg-amber-50 rounded-xl border border-amber-300 flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <span className="font-bold text-amber-900 font-devanagari text-sm">
                      दर अपडेट करें: {editingRate.brand_name} ({editingRate.volume_ml} ml)
                    </span>
                    <p className="text-xs text-amber-700">
                      वर्तमान दर: ₹{editingRate.current_rate}
                    </p>
                  </div>
                  <div className="flex items-center space-x-2">
                    <label className="text-xs font-semibold text-slate-700 font-devanagari">नई दर (₹):</label>
                    <input
                      type="number"
                      min={0}
                      value={editingRate.new_rate}
                      onChange={e => setEditingRate({ ...editingRate, new_rate: Number(e.target.value) })}
                      className="w-24 px-2 py-1 border border-slate-300 rounded font-mono font-bold text-sm"
                    />
                    <button
                      onClick={handleSaveRate}
                      className="bg-emerald-600 text-white px-3 py-1 rounded-md text-xs font-semibold"
                    >
                      सेव करें
                    </button>
                    <button
                      onClick={() => setEditingRate(null)}
                      className="bg-slate-300 text-slate-700 px-3 py-1 rounded-md text-xs"
                    >
                      रद्द
                    </button>
                  </div>
                </div>
              )}

              <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
                <table className="w-full text-left text-sm border-collapse">
                  <thead className="sticky top-0 bg-slate-100 z-10">
                    <tr className="text-slate-700 font-devanagari text-xs uppercase border-b border-slate-200">
                      <th className="px-4 py-2.5">ब्रांड का नाम (Brand)</th>
                      {rateSizes.map(s => (
                        <th key={s.id} className="px-4 py-2.5 text-right font-mono">
                          {s.volume_ml} ml दर (₹)
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {ratesGrid.map(row => (
                      <tr key={row.brand_id} className="hover:bg-slate-50">
                        <td className="px-4 py-2 font-semibold text-slate-800 font-devanagari">
                          {row.brand_name}
                        </td>
                        {rateSizes.map(s => {
                          const currentRate = row.rates[s.volume_ml] || 0;
                          return (
                            <td key={s.id} className="px-4 py-2 text-right">
                              <button
                                onClick={() =>
                                  setEditingRate({
                                    brand_id: row.brand_id,
                                    brand_name: row.brand_name,
                                    size_id: s.id,
                                    volume_ml: s.volume_ml,
                                    current_rate: currentRate,
                                    new_rate: currentRate,
                                  })
                                }
                                title="दर बदलने के लिए क्लिक करें"
                                className="px-2.5 py-1 rounded bg-slate-100 hover:bg-amber-100 hover:text-amber-900 border border-slate-200 font-mono font-bold text-xs transition-colors"
                              >
                                ₹{currentRate}
                              </button>
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 4: OPENING STOCK SETUP */}
          {activeTab === 'opening_stock' && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 space-y-4">
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-base font-bold text-slate-800 font-devanagari">
                    आरंभिक स्टॉक सेटअप (Opening Stock Initialization)
                  </h2>
                  <p className="text-xs text-slate-500">
                    सिस्टम शुरू करते समय पहली बार का आरंभिक स्टॉक दर्ज करें। इसके बाद दैनिक शेष स्वतः आगे बढ़ेगा।
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                  <div className="flex items-center space-x-2 bg-slate-50 px-3 py-1 rounded-lg border border-slate-300">
                    <Store className="w-4 h-4 text-red-600" />
                    <select
                      value={selectedStockShopId}
                      onChange={e => setSelectedStockShopId(Number(e.target.value))}
                      className="bg-transparent text-sm font-bold text-slate-800 focus:outline-none cursor-pointer font-devanagari"
                    >
                      {shops.map(s => (
                        <option key={s.id} value={s.id}>
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="flex items-center space-x-2 bg-slate-50 px-3 py-1 rounded-lg border border-slate-300">
                    <span className="text-xs font-semibold text-slate-600 font-devanagari">प्रभावी तिथि:</span>
                    <input
                      type="date"
                      value={stockDate}
                      onChange={e => setStockDate(e.target.value)}
                      className="bg-white border border-slate-300 rounded px-2 py-0.5 text-xs font-mono font-bold"
                    />
                  </div>

                  <button
                    onClick={handleSaveOpeningStock}
                    disabled={savingStock}
                    className="flex items-center space-x-1.5 bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-1.5 rounded-lg text-xs font-bold font-devanagari transition-colors disabled:opacity-50"
                  >
                    <Save className="w-4 h-4" />
                    <span>{savingStock ? 'सेव हो रहा है...' : 'आरंभिक स्टॉक सेव करें'}</span>
                  </button>
                </div>
              </div>

              <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
                <table className="w-full text-left text-sm border-collapse">
                  <thead className="sticky top-0 bg-slate-100 z-10">
                    <tr className="text-slate-700 font-devanagari text-xs uppercase border-b border-slate-200">
                      <th className="px-4 py-2">ब्रांड (Brand Name)</th>
                      <th className="px-4 py-2">साइज़ (Size)</th>
                      <th className="px-4 py-2 text-right">आरंभिक स्टॉक मात्रा (Opening Qty)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {stockEntries.map((entry, idx) => (
                      <tr key={`${entry.brand_id}_${entry.size_id}`} className="hover:bg-slate-50">
                        <td className="px-4 py-1.5 font-semibold text-slate-800 font-devanagari">
                          {entry.brand_name}
                        </td>
                        <td className="px-4 py-1.5 font-mono text-xs text-slate-600">
                          {entry.volume_ml} ml
                        </td>
                        <td className="px-4 py-1.5 text-right">
                          <input
                            type="number"
                            min={0}
                            value={entry.quantity}
                            onChange={e => {
                              const val = Math.max(0, parseInt(e.target.value, 10) || 0);
                              setStockEntries(prev =>
                                prev.map((item, i) => (i === idx ? { ...item, quantity: val } : item))
                              );
                            }}
                            className="w-24 px-2 py-1 text-right border border-slate-300 rounded font-mono font-bold text-sm focus:ring-1 focus:ring-emerald-500 focus:outline-none"
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
