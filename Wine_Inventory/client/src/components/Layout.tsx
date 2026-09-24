import { ReactNode } from 'react';
import Navbar from './Navbar.js';

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-100 flex flex-col">
      <Navbar />
      <main className="flex-1 w-full max-w-7xl mx-auto px-2 sm:px-4 lg:px-6 py-4">
        {children}
      </main>
      <footer className="bg-slate-900 border-t border-slate-800 py-3 text-center text-xs text-slate-500">
        Liquor Inventory Manager (Web Edition) — S.A. वाइन शॉप इन्वेंटरी सिस्टम &copy; {new Date().getFullYear()}
      </footer>
    </div>
  );
}
