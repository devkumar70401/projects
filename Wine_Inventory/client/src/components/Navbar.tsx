import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.js';
import {
  ClipboardList,
  LayoutDashboard,
  Database,
  FileSpreadsheet,
  LogOut,
  Store,
  User,
} from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useAuth();
  const location = useLocation();

  const navItems = [
    {
      name: 'दैनिक रजिस्टर (Register)',
      path: '/',
      icon: ClipboardList,
    },
    {
      name: 'डैशबोर्ड (Dashboard)',
      path: '/dashboard',
      icon: LayoutDashboard,
    },
    {
      name: 'मास्टर डेटा (Master Data)',
      path: '/master-data',
      icon: Database,
    },
    {
      name: 'रिपोर्ट्स व एक्सपोर्ट (Reports)',
      path: '/reports',
      icon: FileSpreadsheet,
    },
  ];

  return (
    <header className="bg-slate-900 text-white shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo / Brand Header */}
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-lg bg-red-600 flex items-center justify-center text-white shadow-inner">
              <Store className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg tracking-wide font-devanagari text-white">
                  S.A. वाइन शॉप रजिस्टर
                </span>
                <span className="bg-red-500/20 text-red-300 text-xs px-2 py-0.5 rounded font-mono border border-red-500/30">
                  Web v1.0
                </span>
              </div>
              <p className="text-xs text-slate-400">Liquor Daily Stock & Sales Management</p>
            </div>
          </div>

          {/* Nav Links */}
          <nav className="hidden md:flex items-center space-x-1">
            {navItems.map(item => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center space-x-2 px-3.5 py-2 rounded-md text-sm font-medium transition-colors font-devanagari ${
                    isActive
                      ? 'bg-red-600 text-white shadow-sm'
                      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.name}</span>
                </Link>
              );
            })}
          </nav>

          {/* User Session Info & Logout */}
          <div className="flex items-center space-x-4">
            <div className="hidden sm:flex items-center space-x-2 bg-slate-800/80 px-3 py-1.5 rounded-md border border-slate-700">
              <User className="w-4 h-4 text-emerald-400" />
              <div className="text-xs">
                <span className="font-medium text-slate-200">{user?.username || 'Owner'}</span>
                <span className="text-slate-400 ml-1.5 uppercase font-mono text-[10px] bg-slate-700 px-1.5 py-0.5 rounded">
                  {user?.role || 'owner'}
                </span>
              </div>
            </div>

            <button
              onClick={() => logout()}
              title="Logout"
              className="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium text-slate-300 bg-slate-800 hover:bg-red-600 hover:text-white rounded-md transition-colors border border-slate-700 hover:border-red-600"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">लॉगआउट</span>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile nav bar */}
      <div className="md:hidden flex overflow-x-auto bg-slate-950 border-t border-slate-800 px-2 py-1 space-x-1">
        {navItems.map(item => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center space-x-1 px-2.5 py-1.5 rounded text-xs whitespace-nowrap font-devanagari ${
                isActive ? 'bg-red-600 text-white' : 'text-slate-300'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{item.name.split(' ')[0]}</span>
            </Link>
          );
        })}
      </div>
    </header>
  );
}
