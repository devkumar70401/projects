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

export interface Rate {
  id: number;
  brand_id: number;
  size_id: number;
  rate: number;
  effective_from: string;
}

export interface OpeningStock {
  id: number;
  shop_id: number;
  brand_id: number;
  size_id: number;
  quantity: number;
  as_of_date: string;
}

export interface InventoryRecord {
  id: number;
  record_date: string;
  shop_id: number;
  brand_id: number;
  size_id: number;
  previous_balance: number;
  aamad: number;
  total: number;
  bikri: number;
  remaining: number;
  rate: number;
  value: number;
  finalized: number;
  created_at: string;
  updated_at: string;
}

export interface User {
  id: number;
  username: string;
  password_hash: string;
  role: 'owner' | 'staff';
  created_at: string;
}

// Register Grid View DTO
export interface RegisterCellData {
  brand_id: number;
  brand_name: string;
  size_id: number;
  volume_ml: number;
  previous_balance: number | null; // null if opening stock missing
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
  rate?: number; // optional override rate
}

export interface ValidationErrorItem {
  brand_id: number;
  brand_name: string;
  size_id: number;
  volume_ml: number;
  message: string;
}
