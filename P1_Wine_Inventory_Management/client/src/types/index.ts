export interface UserSession {
  id: number;
  username: string;
  role: 'owner' | 'staff';
}

export interface Shop {
  id: number;
  name: string;
  active: number;
  created_at: string;
}

export interface Brand {
  id: number;
  name: string;
  display_order: number;
  active: number;
  created_at: string;
}

export interface Size {
  id: number;
  volume_ml: number;
}

export interface RegisterCellData {
  brand_id: number;
  brand_name: string;
  size_id: number;
  volume_ml: number;
  previous_balance: number | null;
  aamad: number;
  total: number;
  bikri: number;
  remaining: number;
  rate: number;
  value: number;
  record_id?: number;
  has_opening_stock: boolean;
}

export interface RegisterRowData {
  brand_id: number;
  brand_name: string;
  display_order: number;
  sizes: {
    [volume_ml: number]: RegisterCellData;
  };
}

export interface SaveRegisterRowPayload {
  brand_id: number;
  size_id: number;
  aamad: number;
  bikri: number;
  rate?: number;
}

export interface ValidationErrorItem {
  brand_id: number;
  brand_name: string;
  size_id: number;
  volume_ml: number;
  message: string;
}

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

export interface OpeningStockItem {
  id?: number;
  shop_id: number;
  brand_id: number;
  size_id: number;
  quantity: number;
  as_of_date: string;
  brand_name?: string;
  shop_name?: string;
  volume_ml?: number;
}
