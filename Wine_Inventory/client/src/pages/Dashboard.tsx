import { useState, useEffect } from 'react';
import { DashboardSummary, Shop } from '../types/index.js';
import { dashboardApi, shopsApi } from '../api/client.js';
import {
  IndianRupee,
  TrendingUp,
  PackageCheck,
  AlertTriangle,
  Store,
  Calendar,
  Wine,
  BarChart3,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
  CartesianGrid,
  Cell,
} from 'recharts';
import { format } from 'date-fns';

export default function Dashboard() {
  const [shops, setShops] = useState<Shop[]>([]);
  const [selectedShopId, setSelectedShopId] = useState<number | undefined>(undefined);
  const [selectedDate, setSelectedDate] = useState<string>(format(new Date(), 'yyyy-MM-dd'));
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    shopsApi.getShops().then(res => setShops(res.shops));
  }, []);

  useEffect(() => {
    setLoading(true);
    dashboardApi
      .getSummary(selectedDate, selectedShopId)
      .then(res => setSummary(res.summary))
      .finally(() => setLoading(false));
  }, [selectedDate, selectedShopId]);

  return (
    <div className="space-y-4">
      {/* Header & Filter Controls */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-extrabold text-slate-800 font-devanagari">
              व्यापार डैशबोर्ड (Operational Dashboard)
            </h1>
            <p className="text-xs text-slate-500">
              दुकानों की दैनिक बिक्री, स्टॉक मूल्य, और मांग रुझान
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {/* Shop Filter */}
            <div className="flex items-center space-x-2 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-300">
              <Store className="w-4 h-4 text-red-600" />
              <select
                value={selectedShopId || ''}
                onChange={e => setSelectedShopId(e.target.value ? Number(e.target.value) : undefined)}
                className="bg-transparent text-sm font-bold text-slate-800 focus:outline-none font-devanagari cursor-pointer"
              >
                <option value="">सभी दुकानें (All Shops)</option>
                {shops.map(s => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Date Filter */}
            <div className="flex items-center space-x-2 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-300">
              <Calendar className="w-4 h-4 text-red-600" />
              <input
                type="date"
                value={selectedDate}
                onChange={e => setSelectedDate(e.target.value)}
                className="bg-white border border-slate-300 rounded px-2 py-0.5 text-xs font-mono font-bold text-slate-800"
              />
            </div>
          </div>
        </div>
      </div>

      {loading || !summary ? (
        <div className="bg-white rounded-xl p-12 text-center text-slate-500 font-devanagari">
          डैशबोर्ड डेटा लोड हो रहा है...
        </div>
      ) : (
        <>
          {/* Top Metric KPI Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
            {/* KPI 1: Today Sales */}
            <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500 font-devanagari">आज की कुल बिक्री (Today Sales)</p>
                <h3 className="text-2xl font-extrabold text-emerald-600 font-mono mt-1">
                  ₹{summary.total_sales_value.toLocaleString()}
                </h3>
                <p className="text-[11px] text-slate-400 mt-1 font-mono">
                  {summary.total_bikri_qty} बोतलें बिकीं
                </p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <IndianRupee className="w-6 h-6" />
              </div>
            </div>

            {/* KPI 2: Stock on Hand Value */}
            <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500 font-devanagari">स्टॉक कुल मूल्य (Stock Value)</p>
                <h3 className="text-2xl font-extrabold text-amber-600 font-mono mt-1">
                  ₹{summary.total_stock_value.toLocaleString()}
                </h3>
                <p className="text-[11px] text-slate-400 mt-1">
                  उपलब्ध माल की कुल लागत
                </p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                <Wine className="w-6 h-6" />
              </div>
            </div>

            {/* KPI 3: Stock In (Aamad) */}
            <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500 font-devanagari">आज प्राप्त आमद (Stock In)</p>
                <h3 className="text-2xl font-extrabold text-blue-600 font-mono mt-1">
                  {summary.total_aamad_qty}
                </h3>
                <p className="text-[11px] text-slate-400 mt-1">
                  आज दर्ज की गई कुल आमद
                </p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <PackageCheck className="w-6 h-6" />
              </div>
            </div>

            {/* KPI 4: Top Demand */}
            <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500 font-devanagari">शीर्ष बिकने वाला ब्रांड</p>
                <h3 className="text-base font-bold text-slate-800 font-devanagari mt-1 truncate max-w-[150px]">
                  {summary.top_selling_brands[0]?.brand_name || 'उपलब्ध नहीं'}
                </h3>
                <p className="text-[11px] text-slate-400 mt-1 font-mono">
                  {summary.top_selling_brands[0]?.total_bikri || 0} बोतलें बिकीं
                </p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
                <TrendingUp className="w-6 h-6" />
              </div>
            </div>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Chart 1: Top Selling Brands */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <div className="flex items-center space-x-2 mb-3">
                <BarChart3 className="w-5 h-5 text-red-600" />
                <h3 className="font-bold text-sm text-slate-800 font-devanagari">
                  शीर्ष बिकने वाले ब्रांड्स (Top Selling Brands by Qty)
                </h3>
              </div>
              <div className="h-64">
                {summary.top_selling_brands.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={summary.top_selling_brands.map(b => ({
                        name: b.brand_name.split('(')[0].trim(),
                        bikri: b.total_bikri,
                      }))}
                      layout="vertical"
                      margin={{ top: 5, right: 20, left: 30, bottom: 5 }}
                    >
                      <XAxis type="number" fontSize={11} />
                      <YAxis dataKey="name" type="category" width={90} fontSize={10} />
                      <Tooltip />
                      <Bar dataKey="bikri" fill="#2563eb" radius={[0, 4, 4, 0]}>
                        {summary.top_selling_brands.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={index === 0 ? '#dc2626' : '#2563eb'} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-slate-400 text-xs font-devanagari">
                    कोई बिक्री डेटा उपलब्ध नहीं है
                  </div>
                )}
              </div>
            </div>

            {/* Chart 2: Shop Sales Comparison */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <div className="flex items-center space-x-2 mb-3">
                <Store className="w-5 h-5 text-red-600" />
                <h3 className="font-bold text-sm text-slate-800 font-devanagari">
                  दुकान-वार बिक्री तुलना (Shop-wise Sales Comparison)
                </h3>
              </div>
              <div className="h-64">
                {summary.shop_sales.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={summary.shop_sales.map(s => ({
                        name: s.shop_name.split('(')[0].trim(),
                        sales: s.total_sales_value,
                      }))}
                      margin={{ top: 10, right: 10, left: 10, bottom: 20 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="name" fontSize={11} />
                      <YAxis fontSize={11} tickFormatter={v => `₹${v}`} />
                      <Tooltip formatter={(val: any) => [`₹${Number(val).toLocaleString()}`, 'बिक्री (Sales)']} />
                      <Bar dataKey="sales" fill="#10b981" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-slate-400 text-xs font-devanagari">
                    कोई डेटा उपलब्ध नहीं है
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Lower Section: 30-Day Sales Trend & Low Stock Table */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            {/* Sales Trend Line Chart (2 Cols) */}
            <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h3 className="font-bold text-sm text-slate-800 font-devanagari mb-3">
                दैनिक बिक्री रुझान (30-Day Sales Trend)
              </h3>
              <div className="h-64">
                {summary.sales_trend.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={summary.sales_trend} margin={{ top: 10, right: 20, left: 10, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="date" fontSize={10} />
                      <YAxis fontSize={10} tickFormatter={v => `₹${v}`} />
                      <Tooltip formatter={(val: any) => [`₹${Number(val).toLocaleString()}`, 'बिक्री (Sales)']} />
                      <Line
                        type="monotone"
                        dataKey="sales_value"
                        stroke="#dc2626"
                        strokeWidth={2.5}
                        dot={{ r: 3 }}
                        activeDot={{ r: 6 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-slate-400 text-xs font-devanagari">
                    कोई रुझान डेटा उपलब्ध नहीं है
                  </div>
                )}
              </div>
            </div>

            {/* Low Stock Items List (1 Col) */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex flex-col">
              <div className="flex items-center space-x-2 mb-3">
                <AlertTriangle className="w-5 h-5 text-amber-500" />
                <h3 className="font-bold text-sm text-slate-800 font-devanagari">
                  कम स्टॉक चेतावनी (&le; 10 बोतलें)
                </h3>
              </div>

              <div className="overflow-y-auto max-h-64 flex-1 space-y-2">
                {summary.low_stock_items.length > 0 ? (
                  summary.low_stock_items.map((item, idx) => (
                    <div
                      key={idx}
                      className="p-2 bg-amber-50/70 border border-amber-200 rounded-lg flex items-center justify-between text-xs"
                    >
                      <div>
                        <span className="font-semibold text-slate-900 font-devanagari block truncate max-w-[150px]">
                          {item.brand_name}
                        </span>
                        <span className="text-[10px] text-slate-500 font-mono">
                          {item.shop_name} &bull; {item.volume_ml} ml
                        </span>
                      </div>
                      <span className="font-mono font-bold text-red-600 bg-red-100 px-2 py-0.5 rounded text-xs">
                        {item.remaining} शेष
                      </span>
                    </div>
                  ))
                ) : (
                  <div className="text-center text-slate-400 text-xs py-8 font-devanagari">
                    कोई भी उत्पाद कम स्टॉक में नहीं है
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
