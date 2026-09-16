import React, { useState, useEffect } from 'react';
import { Home, History, Bookmark, Search, Settings, Menu, X, Plus } from 'lucide-react';
import { ActiveNavTab, Notebook, DocumentItem } from './types';
import { INITIAL_NOTEBOOKS, USER_PROFILE } from './data/mockData';
import { SideNav } from './components/SideNav';
import { ShelfView } from './components/ShelfView';
import { NotebookDetailView } from './components/NotebookDetailView';
import { ReaderView } from './components/ReaderView';
import { RecentView } from './components/RecentView';
import { BookmarksView } from './components/BookmarksView';
import { GlobalSearchView } from './components/GlobalSearchView';
import { SettingsView } from './components/SettingsView';
import { NewNotebookModal } from './components/NewNotebookModal';

export function App() {
  // Persistence for notebooks
  const [notebooks, setNotebooks] = useState<Notebook[]>(() => {
    const saved = localStorage.getItem('notereader_notebooks');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      } catch (e) {
        console.error('Failed to parse saved notebooks', e);
      }
    }
    return INITIAL_NOTEBOOKS;
  });

  // Navigation and active item states
  const [activeNavTab, setActiveNavTab] = useState<ActiveNavTab>('home');
  const [selectedNotebook, setSelectedNotebook] = useState<Notebook | null>(null);
  const [selectedDocument, setSelectedDocument] = useState<DocumentItem | null>(null);
  const [isNewNotebookModalOpen, setIsNewNotebookModalOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Sync to local storage
  useEffect(() => {
    localStorage.setItem('notereader_notebooks', JSON.stringify(notebooks));
  }, [notebooks]);

  // Keep selectedNotebook reference in sync with latest notebooks state
  useEffect(() => {
    if (selectedNotebook) {
      const fresh = notebooks.find((n) => n.id === selectedNotebook.id);
      if (fresh) {
        setSelectedNotebook(fresh);
      }
    }
  }, [notebooks]);

  // Count total bookmarks
  const totalBookmarksCount = notebooks.reduce(
    (acc, nb) =>
      acc +
      nb.sections.reduce(
        (sAcc, sec) => sAcc + sec.documents.reduce((dAcc, doc) => dAcc + doc.bookmarks.length, 0),
        0
      ),
    0
  );

  // Navigation handlers
  const handleSelectTab = (tab: ActiveNavTab) => {
    setActiveNavTab(tab);
    setSelectedNotebook(null);
    setSelectedDocument(null);
    setMobileMenuOpen(false);
  };

  const handleSelectNotebook = (notebook: Notebook) => {
    setSelectedNotebook(notebook);
    setSelectedDocument(null);
  };

  const handleSelectDocument = (doc: DocumentItem, notebook: Notebook) => {
    setSelectedNotebook(notebook);
    setSelectedDocument(doc);
  };

  const handleSelectDocumentAndPage = (doc: DocumentItem, notebook: Notebook, page: number) => {
    const updatedDoc = { ...doc, currentPage: page };
    setSelectedNotebook(notebook);
    setSelectedDocument(updatedDoc);
  };

  // Progress update handler
  const handleUpdateProgress = (docId: string, page: number, progress: number) => {
    setNotebooks((prev) =>
      prev.map((nb) => ({
        ...nb,
        sections: nb.sections.map((sec) => ({
          ...sec,
          documents: sec.documents.map((d) => {
            if (d.id === docId) {
              return {
                ...d,
                currentPage: page,
                progressPercentage: progress,
                lastRead: 'Just now',
              };
            }
            return d;
          }),
        })),
      }))
    );

    if (selectedDocument && selectedDocument.id === docId) {
      setSelectedDocument((prev) =>
        prev
          ? {
              ...prev,
              currentPage: page,
              progressPercentage: progress,
              lastRead: 'Just now',
            }
          : null
      );
    }
  };

  // Bookmark toggle handler
  const handleToggleBookmark = (docId: string, page: number) => {
    setNotebooks((prev) =>
      prev.map((nb) => ({
        ...nb,
        sections: nb.sections.map((sec) => ({
          ...sec,
          documents: sec.documents.map((d) => {
            if (d.id === docId) {
              const existingIdx = d.bookmarks.findIndex((b) => b.pageNumber === page);
              let updatedBookmarks = [...d.bookmarks];
              let isBookmarked = d.bookmarked;

              if (existingIdx >= 0) {
                updatedBookmarks.splice(existingIdx, 1);
                isBookmarked = updatedBookmarks.length > 0;
              } else {
                updatedBookmarks.push({
                  id: `bm-${Date.now()}`,
                  pageNumber: page,
                  title: `Page ${page} excerpt`,
                  snippet: d.content.sections[0]?.paragraphs[0]?.slice(0, 100) || 'Bookmark excerpt...',
                  timestamp: 'Just now',
                });
                isBookmarked = true;
              }

              return {
                ...d,
                bookmarked: isBookmarked,
                bookmarks: updatedBookmarks,
              };
            }
            return d;
          }),
        })),
      }))
    );

    if (selectedDocument && selectedDocument.id === docId) {
      setSelectedDocument((prev) => {
        if (!prev) return null;
        const existingIdx = prev.bookmarks.findIndex((b) => b.pageNumber === page);
        let updated = [...prev.bookmarks];
        if (existingIdx >= 0) {
          updated.splice(existingIdx, 1);
        } else {
          updated.push({
            id: `bm-${Date.now()}`,
            pageNumber: page,
            title: `Page ${page} excerpt`,
            snippet: prev.content.sections[0]?.paragraphs[0]?.slice(0, 100) || 'Bookmark excerpt...',
            timestamp: 'Just now',
          });
        }
        return {
          ...prev,
          bookmarked: updated.length > 0,
          bookmarks: updated,
        };
      });
    }
  };

  // Add Section handler
  const handleAddSection = (notebookId: string, sectionName: string) => {
    const newSection = {
      id: `sec-${Date.now()}`,
      name: sectionName,
      documents: [],
    };
    setNotebooks((prev) =>
      prev.map((nb) => (nb.id === notebookId ? { ...nb, sections: [...nb.sections, newSection] } : nb))
    );
  };

  // Add Document handler
  const handleAddDocument = (
    notebookId: string,
    sectionId: string,
    docData: Partial<DocumentItem>
  ) => {
    const newDoc: DocumentItem = {
      id: `doc-${Date.now()}`,
      title: docData.title || 'Untitled Document',
      type: docData.type || 'md',
      lastRead: 'Just now',
      progressPercentage: 0,
      currentPage: 1,
      totalPages: docData.totalPages || 10,
      bookmarked: false,
      bookmarks: [],
      toc: docData.toc || [{ id: 'toc-1', title: docData.title || 'Introduction', level: 1, pageNumber: 1 }],
      content: docData.content || {
        sections: [
          {
            id: 'sec-init',
            heading: docData.title,
            paragraphs: ['Your research and notes begin here.'],
          },
        ],
      },
    };

    setNotebooks((prev) =>
      prev.map((nb) => {
        if (nb.id === notebookId) {
          return {
            ...nb,
            pagesCount: nb.pagesCount + newDoc.totalPages,
            sections: nb.sections.map((sec) => {
              if (sec.id === sectionId) {
                return { ...sec, documents: [newDoc, ...sec.documents] };
              }
              return sec;
            }),
          };
        }
        return nb;
      })
    );
  };

  // Delete Document handler
  const handleDeleteDocument = (notebookId: string, sectionId: string, docId: string) => {
    setNotebooks((prev) =>
      prev.map((nb) => {
        if (nb.id === notebookId) {
          return {
            ...nb,
            sections: nb.sections.map((sec) => {
              if (sec.id === sectionId) {
                return { ...sec, documents: sec.documents.filter((d) => d.id !== docId) };
              }
              return sec;
            }),
          };
        }
        return nb;
      })
    );
  };

  // Create Notebook handler
  const handleCreateNotebook = (notebookData: Partial<Notebook>) => {
    const newNb: Notebook = {
      id: `nb-${Date.now()}`,
      title: notebookData.title || 'Untitled',
      color: notebookData.color || '#31685a',
      textColor: notebookData.textColor || '#ffffff',
      pagesCount: 0,
      description: notebookData.description || '',
      updatedAt: 'Just now',
      sections: notebookData.sections || [
        {
          id: `sec-${Date.now()}`,
          name: 'General',
          isDefault: true,
          documents: [],
        },
      ],
    };
    setNotebooks((prev) => [newNb, ...prev]);
  };

  // Delete Notebook handler
  const handleDeleteNotebook = (notebookId: string) => {
    setNotebooks((prev) => prev.filter((nb) => nb.id !== notebookId));
    if (selectedNotebook && selectedNotebook.id === notebookId) {
      setSelectedNotebook(null);
    }
  };

  // Reset sample data
  const handleResetSampleData = () => {
    localStorage.removeItem('notereader_notebooks');
    setNotebooks(INITIAL_NOTEBOOKS);
    setSelectedNotebook(null);
    setSelectedDocument(null);
    setActiveNavTab('home');
  };

  // If a document is open, render the Reader View directly (full screen immersive)
  if (selectedDocument && selectedNotebook) {
    return (
      <ReaderView
        document={selectedDocument}
        notebook={selectedNotebook}
        onBack={() => setSelectedDocument(null)}
        onUpdateProgress={handleUpdateProgress}
        onToggleBookmark={handleToggleBookmark}
      />
    );
  }

  return (
    <div className="flex h-screen w-full bg-[#f9f9f7] dark:bg-[#1a1c1b] text-[#1a1c1b] dark:text-[#f1f1ef] font-inter overflow-hidden">
      {/* Desktop Left Sidebar */}
      <SideNav
        activeTab={activeNavTab}
        onSelectTab={handleSelectTab}
        onOpenNewNotebookModal={() => setIsNewNotebookModalOpen(true)}
        bookmarksCount={totalBookmarksCount}
      />

      {/* Main Content Area (offset by 280px on desktop) */}
      <div className="flex-1 md:ml-[280px] flex flex-col h-full overflow-y-auto">
        {/* Mobile Header Bar */}
        <div className="md:hidden flex items-center justify-between px-5 py-3.5 bg-[#dadad8] dark:bg-[#2f3130] border-b border-[#c1c8c5]/40 shrink-0">
          <div className="flex items-center gap-3">
            <img
              src={USER_PROFILE.avatarUrl}
              alt="User"
              className="w-8 h-8 rounded-full object-cover border border-black/10"
              referrerPolicy="no-referrer"
            />
            <span className="font-bold text-[#264a42] dark:text-[#a8cfc3] text-lg">
              NoteReader
            </span>
          </div>
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2 text-[#414846] dark:text-[#c1c8c5]"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {/* Mobile Slide-down Drawer */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-[#dadad8] dark:bg-[#2f3130] border-b border-[#c1c8c5]/40 p-4 space-y-2 z-50">
            <button
              onClick={() => handleSelectTab('home')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-left text-sm ${
                activeNavTab === 'home'
                  ? 'bg-[#e8e8e6] text-[#264a42] font-bold'
                  : 'text-[#414846] dark:text-[#c1c8c5]'
              }`}
            >
              <Home className="w-4 h-4" />
              <span>Shelf</span>
            </button>
            <button
              onClick={() => handleSelectTab('recent')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-left text-sm ${
                activeNavTab === 'recent'
                  ? 'bg-[#e8e8e6] text-[#264a42] font-bold'
                  : 'text-[#414846] dark:text-[#c1c8c5]'
              }`}
            >
              <History className="w-4 h-4" />
              <span>Recent</span>
            </button>
            <button
              onClick={() => handleSelectTab('bookmarks')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-left text-sm ${
                activeNavTab === 'bookmarks'
                  ? 'bg-[#e8e8e6] text-[#264a42] font-bold'
                  : 'text-[#414846] dark:text-[#c1c8c5]'
              }`}
            >
              <Bookmark className="w-4 h-4" />
              <span>Bookmarks ({totalBookmarksCount})</span>
            </button>
            <button
              onClick={() => handleSelectTab('search')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-left text-sm ${
                activeNavTab === 'search'
                  ? 'bg-[#e8e8e6] text-[#264a42] font-bold'
                  : 'text-[#414846] dark:text-[#c1c8c5]'
              }`}
            >
              <Search className="w-4 h-4" />
              <span>Search</span>
            </button>
            <button
              onClick={() => handleSelectTab('settings')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-left text-sm ${
                activeNavTab === 'settings'
                  ? 'bg-[#e8e8e6] text-[#264a42] font-bold'
                  : 'text-[#414846] dark:text-[#c1c8c5]'
              }`}
            >
              <Settings className="w-4 h-4" />
              <span>Settings</span>
            </button>
          </div>
        )}

        {/* VIEW ROUTER */}
        {selectedNotebook ? (
          <NotebookDetailView
            notebook={selectedNotebook}
            onBackToShelf={() => setSelectedNotebook(null)}
            onSelectDocument={handleSelectDocument}
            onAddSection={handleAddSection}
            onAddDocument={handleAddDocument}
            onDeleteDocument={handleDeleteDocument}
          />
        ) : activeNavTab === 'home' ? (
          <ShelfView
            notebooks={notebooks}
            onSelectNotebook={handleSelectNotebook}
            onOpenNewNotebookModal={() => setIsNewNotebookModalOpen(true)}
            onDeleteNotebook={handleDeleteNotebook}
          />
        ) : activeNavTab === 'recent' ? (
          <RecentView
            notebooks={notebooks}
            onSelectDocument={handleSelectDocument}
          />
        ) : activeNavTab === 'bookmarks' ? (
          <BookmarksView
            notebooks={notebooks}
            onSelectDocumentAndPage={handleSelectDocumentAndPage}
            onRemoveBookmark={(docId, page) => handleToggleBookmark(docId, page)}
          />
        ) : activeNavTab === 'search' ? (
          <GlobalSearchView
            notebooks={notebooks}
            onSelectDocument={handleSelectDocument}
            onSelectNotebook={handleSelectNotebook}
          />
        ) : (
          <SettingsView onResetSampleData={handleResetSampleData} />
        )}
      </div>

      {/* New Notebook Modal */}
      <NewNotebookModal
        isOpen={isNewNotebookModalOpen}
        onClose={() => setIsNewNotebookModalOpen(false)}
        onCreateNotebook={handleCreateNotebook}
      />
    </div>
  );
}

export default App;
