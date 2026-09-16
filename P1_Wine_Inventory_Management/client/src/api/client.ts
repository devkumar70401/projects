const API_BASE = '/api';

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const defaultHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
    credentials: 'include',
  });

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    const error: any = new Error(data.error || 'Network request failed');
    error.status = response.status;
    error.errors = data.errors;
    throw error;
  }

  return data as T;
}

export const authApi = {
  login: (username: string, password: string) =>
    request<{ success: boolean; user: any; token: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  logout: () => request<{ success: boolean }>('/auth/logout', { method: 'POST' }),
  me: () => request<{ user: any }>('/auth/me'),
};

export const shopsApi = {
  getShops: (all = false) => request<{ shops: any[] }>(`/shops?all=${all}`),
  createShop: (name: string) =>
    request<{ shop: any }>('/shops', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
  updateShop: (id: number, data: { name?: string; active?: number }) =>
    request<{ shop: any }>(`/shops/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
};

export const brandsApi = {
  getBrands: (all = false) => request<{ brands: any[] }>(`/brands?all=${all}`),
  createBrand: (name: string, display_order?: number) =>
    request<{ brand: any }>('/brands', {
      method: 'POST',
      body: JSON.stringify({ name, display_order }),
    }),
  updateBrand: (id: number, data: { name?: string; display_order?: number; active?: number }) =>
    request<{ brand: any }>(`/brands/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  reorder: (orders: { id: number; display_order: number }[]) =>
    request<{ success: boolean; brands: any[] }>('/brands/reorder', {
      method: 'POST',
      body: JSON.stringify({ orders }),
    }),
};

export const ratesApi = {
  getRates: (date?: string) =>
    request<{ brands: any[]; sizes: any[]; grid: any[] }>(`/rates${date ? `?date=${date}` : ''}`),
  setRate: (brand_id: number, size_id: number, rate: number, effective_from?: string) =>
    request<any>('/rates', {
      method: 'POST',
      body: JSON.stringify({ brand_id, size_id, rate, effective_from }),
    }),
  getHistory: (brand_id: number, size_id: number) =>
    request<{ history: any[] }>(`/rates/history/${brand_id}/${size_id}`),
};

export const inventoryApi = {
  getSheet: (shop_id: number, date: string) =>
    request<{ shop: any; date: string; sizes: any[]; sheet: any[] }>(
      `/inventory/sheet?shop_id=${shop_id}&date=${date}`
    ),
  saveSheet: (payload: { shop_id: number; date: string; rows: any[] }) =>
    request<{ success: boolean; message: string; saved_count?: number }>('/inventory/sheet', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getHistory: (params: { shop_id?: number; brand_id?: number; size_id?: number; start_date?: string; end_date?: string }) => {
    const search = new URLSearchParams();
    if (params.shop_id) search.append('shop_id', String(params.shop_id));
    if (params.brand_id) search.append('brand_id', String(params.brand_id));
    if (params.size_id) search.append('size_id', String(params.size_id));
    if (params.start_date) search.append('start_date', params.start_date);
    if (params.end_date) search.append('end_date', params.end_date);
    return request<{ records: any[] }>(`/inventory/history?${search.toString()}`);
  },
  getOpeningStock: (shop_id?: number) =>
    request<{ opening_stock: any[] }>(`/inventory/opening-stock${shop_id ? `?shop_id=${shop_id}` : ''}`),
  saveOpeningStock: (shop_id: number, entries: any[], as_of_date?: string) =>
    request<{ success: boolean; message: string }>('/inventory/opening-stock', {
      method: 'POST',
      body: JSON.stringify({ shop_id, entries, as_of_date }),
    }),
};

export const dashboardApi = {
  getSummary: (date?: string, shop_id?: number) => {
    const search = new URLSearchParams();
    if (date) search.append('date', date);
    if (shop_id) search.append('shop_id', String(shop_id));
    return request<{ summary: any }>(`/dashboard?${search.toString()}`);
  },
};

export const reportsApi = {
  getData: (params: { shop_id?: number; brand_id?: number; size_id?: number; start_date?: string; end_date?: string }) => {
    const search = new URLSearchParams();
    if (params.shop_id) search.append('shop_id', String(params.shop_id));
    if (params.brand_id) search.append('brand_id', String(params.brand_id));
    if (params.size_id) search.append('size_id', String(params.size_id));
    if (params.start_date) search.append('start_date', params.start_date);
    if (params.end_date) search.append('end_date', params.end_date);
    return request<{ records: any[] }>(`/reports/data?${search.toString()}`);
  },
  getExcelUrl: (params: { shop_id?: number; brand_id?: number; size_id?: number; start_date?: string; end_date?: string }) => {
    const search = new URLSearchParams();
    if (params.shop_id) search.append('shop_id', String(params.shop_id));
    if (params.brand_id) search.append('brand_id', String(params.brand_id));
    if (params.size_id) search.append('size_id', String(params.size_id));
    if (params.start_date) search.append('start_date', params.start_date);
    if (params.end_date) search.append('end_date', params.end_date);
    return `/api/reports/export/excel?${search.toString()}`;
  },
  getCsvUrl: (params: { shop_id?: number; brand_id?: number; size_id?: number; start_date?: string; end_date?: string }) => {
    const search = new URLSearchParams();
    if (params.shop_id) search.append('shop_id', String(params.shop_id));
    if (params.brand_id) search.append('brand_id', String(params.brand_id));
    if (params.size_id) search.append('size_id', String(params.size_id));
    if (params.start_date) search.append('start_date', params.start_date);
    if (params.end_date) search.append('end_date', params.end_date);
    return `/api/reports/export/csv?${search.toString()}`;
  },
  getPdfUrl: (shop_id: number, date: string) => `/api/reports/export/pdf?shop_id=${shop_id}&date=${date}`,
};
