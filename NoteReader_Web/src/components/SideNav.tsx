import React from 'react';
import { Home, History, Bookmark, Search, Settings, Plus, BookOpen } from 'lucide-react';
import { ActiveNavTab } from '../types';
import { USER_PROFILE } from '../data/mockData';

interface SideNavProps {
  activeTab: ActiveNavTab;
  onSelectTab: (tab: ActiveNavTab) => void;
  onOpenNewNotebookModal?: () => void;
  bookmarksCount?: number;
}

export const SideNav: React.FC<SideNavProps> = ({
  activeTab,
  onSelectTab,
  onOpenNewNotebookModal,
  bookmarksCount = 0,
}) => {
  const navItems = [
    { id: 'home' as ActiveNavTab, label: 'Home', icon: Home },
    { id: 'recent' as ActiveNavTab, label: 'Recent', icon: History },
    { id: 'bookmarks' as ActiveNavTab, label: 'Bookmarks', icon: Bookmark, badge: bookmarksCount },
    { id: 'search' as ActiveNavTab, label: 'Search', icon: Search },
  ];

  return (
    <nav
      id="side-nav-bar"
      className="hidden md:flex flex-col fixed left-0 top-0 h-full w-[280px] bg-[#dadad8] dark:bg-[#2f3130] py-8 z-40 border-r border-[#c1c8c5]/40 transition-colors duration-200"
    >
      {/* Profile / Header */}
      <div className="px-6 mb-8 flex items-center gap-4">
        <div className="w-12 h-12 rounded-full bg-[#3e6259] text-white flex items-center justify-center overflow-hidden border border-[#c1c8c5]/30 shadow-sm shrink-0">
          <img
            src={USER_PROFILE.avatarUrl}
            alt="User Profile"
            className="w-full h-full object-cover"
            referrerPolicy="no-referrer"
          />
        </div>
        <div>
          <h1 className="text-[22px] font-bold text-[#264a42] dark:text-[#a8cfc3] tracking-tight flex items-center gap-2">
            NoteReader
          </h1>
          <p className="text-[#414846] dark:text-[#c1c8c5] text-[12px] font-semibold tracking-wider uppercase font-ibm mt-0.5">
            {USER_PROFILE.subtitle}
          </p>
        </div>
      </div>

      {/* Navigation List */}
      <ul className="flex flex-col gap-1.5 px-4 flex-grow">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <li key={item.id}>
              <button
                id={`nav-${item.id}`}
                onClick={() => onSelectTab(item.id)}
                className={`w-full flex items-center gap-4 px-4 py-3 rounded-lg text-left transition-colors duration-200 group relative ${
                  isActive
                    ? 'bg-[#e8e8e6] dark:bg-[#383c3a] text-[#264a42] dark:text-[#a8cfc3] font-bold border-r-4 border-[#264a42] dark:border-[#a8cfc3]'
                    : 'text-[#414846] dark:text-[#e2e2e1] hover:bg-[#e8e8e6]/60 dark:hover:bg-[#383c3a]/50 font-normal'
                }`}
              >
                <Icon
                  className={`w-5 h-5 transition-transform duration-200 group-hover:scale-105 ${
                    isActive ? 'text-[#264a42] dark:text-[#a8cfc3] fill-[#264a42]/20' : 'text-[#717976]'
                  }`}
                />
                <span className="text-[15px] font-inter">{item.label}</span>
                {item.badge !== undefined && item.badge > 0 && (
                  <span className="ml-auto text-[11px] font-mono px-2 py-0.5 rounded-full bg-[#3e6259]/15 text-[#264a42] dark:bg-[#a8cfc3]/20 dark:text-[#a8cfc3] font-semibold">
                    {item.badge}
                  </span>
                )}
              </button>
            </li>
          );
        })}

        {/* Quick New Notebook in side rail */}
        {onOpenNewNotebookModal && (
          <li className="mt-4 px-1">
            <button
              id="sidebar-new-notebook-btn"
              onClick={onOpenNewNotebookModal}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg bg-[#3e6259]/10 hover:bg-[#3e6259]/20 text-[#264a42] dark:text-[#a8cfc3] text-xs font-semibold font-ibm tracking-wider uppercase transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>New Notebook</span>
            </button>
          </li>
        )}

        {/* Settings Tab at bottom */}
        <li className="mt-auto">
          <button
            id="nav-settings"
            onClick={() => onSelectTab('settings')}
            className={`w-full flex items-center gap-4 px-4 py-3 rounded-lg text-left transition-colors duration-200 ${
              activeTab === 'settings'
                ? 'bg-[#e8e8e6] dark:bg-[#383c3a] text-[#264a42] dark:text-[#a8cfc3] font-bold border-r-4 border-[#264a42] dark:border-[#a8cfc3]'
                : 'text-[#414846] dark:text-[#e2e2e1] hover:bg-[#e8e8e6]/60 dark:hover:bg-[#383c3a]/50'
            }`}
          >
            <Settings className="w-5 h-5 text-[#717976]" />
            <span className="text-[15px] font-inter">Settings</span>
          </button>
        </li>
      </ul>
    </nav>
  );
};
