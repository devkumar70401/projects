import React, { useState, useEffect, useRef } from 'react';
import {
  ArrowLeft,
  BookOpen,
  Search,
  Bookmark as BookmarkIcon,
  Type,
  MoreVertical,
  ChevronLeft,
  ChevronRight,
  Sliders,
  Check,
  CheckCircle,
  Play,
  Copy,
  Printer,
  RotateCcw,
  X,
  BookmarkPlus,
  Share2
} from 'lucide-react';
import { DocumentItem, Notebook, ReaderSettings, ThemeMode, Typeface, Bookmark } from '../types';

interface ReaderViewProps {
  document: DocumentItem;
  notebook: Notebook;
  onBack: () => void;
  onUpdateProgress: (docId: string, page: number, progress: number) => void;
  onToggleBookmark: (docId: string, page: number) => void;
}

export const ReaderView: React.FC<ReaderViewProps> = ({
  document,
  notebook,
  onBack,
  onUpdateProgress,
  onToggleBookmark,
}) => {
  // Appearance settings state (loaded from local storage or defaults)
  const [settings, setSettings] = useState<ReaderSettings>(() => {
    const saved = localStorage.getItem('notereader_settings');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        // ignore
      }
    }
    return {
      theme: 'light',
      fontSize: 18,
      typeface: 'Literata',
      lineHeight: 'relaxed',
      marginWidth: 'standard',
    };
  });

  // Reading state
  const [currentPage, setCurrentPage] = useState<number>(document.currentPage || 1);
  const totalPages = document.totalPages || 15;
  const [isImmersive, setIsImmersive] = useState<boolean>(false);

  // Drawers & Modals state
  const [showAppearanceSheet, setShowAppearanceSheet] = useState<boolean>(false);
  const [showTocDrawer, setShowTocDrawer] = useState<boolean>(false);
  const [showBookmarksDrawer, setShowBookmarksDrawer] = useState<boolean>(false);
  const [showSearchModal, setShowSearchModal] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [showMoreMenu, setShowMoreMenu] = useState<boolean>(false);
  const [showBookmarkToast, setShowBookmarkToast] = useState<string | null>(null);

  // Interactive Jupyter execution simulation
  const [codeCells, setCodeCells] = useState(
    document.content.sections.flatMap((s) => s.codeCells || [])
  );
  const [cellRunningId, setCellRunningId] = useState<string | null>(null);

  // Ref for reading container
  const canvasRef = useRef<HTMLDivElement>(null);

  // Sync settings to localStorage
  useEffect(() => {
    localStorage.setItem('notereader_settings', JSON.stringify(settings));
  }, [settings]);

  // Handle page change & notify parent of progress
  const handlePageChange = (newPage: number) => {
    const clamped = Math.max(1, Math.min(totalPages, newPage));
    setCurrentPage(clamped);
    const progress = Math.round((clamped / totalPages) * 100);
    onUpdateProgress(document.id, clamped, progress);
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.key === 'ArrowRight' || e.key === 'PageDown' || e.key === ' ') {
        handlePageChange(currentPage + 1);
      } else if (e.key === 'ArrowLeft' || e.key === 'PageUp') {
        handlePageChange(currentPage - 1);
      } else if (e.key === 'Escape') {
        setShowAppearanceSheet(false);
        setShowTocDrawer(false);
        setShowBookmarksDrawer(false);
        setShowSearchModal(false);
        setShowMoreMenu(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentPage, totalPages]);

  // Toggle Immersive Mode on Canvas Click (if not selecting text)
  const handleCanvasClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    if (
      target.closest('button') ||
      target.closest('input') ||
      target.closest('pre') ||
      target.closest('code') ||
      target.closest('a') ||
      target.closest('.interactive-control')
    ) {
      return;
    }
    const selection = window.getSelection();
    if (!selection || selection.toString().length === 0) {
      setIsImmersive((prev) => !prev);
    }
  };

  // Run a Jupyter code cell
  const handleRunCodeCell = (cellId: string) => {
    setCellRunningId(cellId);
    setTimeout(() => {
      setCellRunningId(null);
      setShowBookmarkToast('Executed code cell output updated!');
      setTimeout(() => setShowBookmarkToast(null), 2500);
    }, 600);
  };

  // Bookmarking toggle
  const isCurrentPageBookmarked = document.bookmarks.some(
    (b) => b.pageNumber === currentPage
  ) || document.bookmarked;

  const handleToggleCurrentBookmark = () => {
    onToggleBookmark(document.id, currentPage);
    setShowBookmarkToast(
      isCurrentPageBookmarked ? 'Bookmark removed' : `Bookmarked page ${currentPage}`
    );
    setTimeout(() => setShowBookmarkToast(null), 2000);
  };

  // Appearance Theme styling classes
  const getThemeWrapperClasses = () => {
    switch (settings.theme) {
      case 'sepia':
        return 'bg-[#F5EEDC] text-[#3D3828] theme-sepia';
      case 'dark':
        return 'bg-[#1a1c1b] text-[#f1f1ef] theme-dark';
      case 'light':
      default:
        return 'bg-[#f9f9f7] text-[#1a1c1b] theme-light';
    }
  };

  const getHeaderFooterClasses = () => {
    switch (settings.theme) {
      case 'sepia':
        return 'bg-[#F5EEDC]/90 text-[#3D3828] border-[#E3D5B1]';
      case 'dark':
        return 'bg-[#1a1c1b]/90 text-[#f1f1ef] border-[#414846]';
      case 'light':
      default:
        return 'bg-[#f9f9f7]/90 text-[#1a1c1b] border-[#c1c8c5]/40';
    }
  };

  const getQuoteCardClasses = () => {
    switch (settings.theme) {
      case 'sepia':
        return 'bg-[#FDF9EE] border-l-4 border-[#264a42] text-[#3D3828]/90';
      case 'dark':
        return 'bg-[#242726] border-l-4 border-[#a8cfc3] text-[#f1f1ef]/90';
      case 'light':
      default:
        return 'bg-[#ffffff] border-l-4 border-[#264a42] text-[#414846] shadow-sm';
    }
  };

  const getTypefaceClass = () => {
    switch (settings.typeface) {
      case 'Inter':
        return 'font-inter';
      case 'IBM Plex Sans':
        return 'font-ibm';
      case 'Literata':
      default:
        return 'font-literata';
    }
  };

  // In-document search matches count
  const allDocText = document.content.sections
    .flatMap((s) => [s.heading || '', ...s.paragraphs])
    .join(' ');
  const searchMatchesCount =
    searchQuery.trim().length > 1
      ? (allDocText.toLowerCase().match(new RegExp(searchQuery.toLowerCase(), 'g')) || []).length
      : 0;

  return (
    <div
      id="reader-view-container"
      className={`min-h-screen w-full relative select-text transition-colors duration-300 ${getThemeWrapperClasses()}`}
    >
      {/* Toast Notification */}
      {showBookmarkToast && (
        <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 bg-[#264a42] text-white text-xs font-semibold px-4 py-2 rounded-full shadow-lg flex items-center gap-2 animate-fade-in pointer-events-none">
          <Check className="w-3.5 h-3.5" />
          <span>{showBookmarkToast}</span>
        </div>
      )}

      {/* TOP BAR (Reader Chrome) */}
      <header
        id="reader-top-bar"
        className={`fixed top-0 left-0 right-0 z-40 border-b backdrop-blur-md transition-all duration-300 flex justify-between items-center w-full px-4 md:px-8 py-3.5 ${getHeaderFooterClasses()} ${
          isImmersive ? '-translate-y-full opacity-0 pointer-events-none' : 'translate-y-0 opacity-100'
        }`}
      >
        {/* Left: Back Arrow and Title */}
        <div className="flex items-center gap-3 max-w-[60%]">
          <button
            id="reader-back-btn"
            onClick={onBack}
            aria-label="Back to notebook"
            className="p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="flex flex-col truncate">
            <span className="text-[11px] font-ibm uppercase font-semibold text-[#717976] dark:text-[#a8cfc3] tracking-wider truncate">
              {notebook.title}
            </span>
            <h1 className="text-[16px] md:text-[17px] font-bold font-inter truncate">
              {document.title}
            </h1>
          </div>
        </div>

        {/* Right: Actions Cluster */}
        <div className="flex items-center gap-1 md:gap-2">
          {/* Table of Contents */}
          <button
            id="reader-toc-btn"
            onClick={() => setShowTocDrawer(true)}
            className="p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors flex items-center gap-1.5"
            title="Table of Contents"
          >
            <BookOpen className="w-5 h-5 text-[#717976] dark:text-[#c1c8c5]" />
            <span className="hidden lg:inline text-xs font-ibm font-semibold uppercase text-[#717976] dark:text-[#c1c8c5]">
              TOC
            </span>
          </button>

          {/* In-doc Search */}
          <button
            id="reader-search-btn"
            onClick={() => setShowSearchModal(true)}
            className="p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
            title="Search in document"
          >
            <Search className="w-5 h-5 text-[#717976] dark:text-[#c1c8c5]" />
          </button>

          {/* Bookmark Toggle */}
          <button
            id="reader-bookmark-toggle-btn"
            onClick={handleToggleCurrentBookmark}
            className={`p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors ${
              isCurrentPageBookmarked ? 'text-[#264a42] dark:text-[#a8cfc3]' : 'text-[#717976]'
            }`}
            title={isCurrentPageBookmarked ? 'Remove Bookmark' : 'Add Bookmark'}
          >
            <BookmarkIcon
              className="w-5 h-5"
              fill={isCurrentPageBookmarked ? 'currentColor' : 'none'}
            />
          </button>

          <div className="w-px h-5 bg-[#c1c8c5]/50 mx-1 hidden sm:block" />

          {/* Appearance Settings */}
          <button
            id="reader-appearance-btn"
            onClick={() => setShowAppearanceSheet(true)}
            className={`p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors relative ${
              showAppearanceSheet
                ? 'bg-black/10 dark:bg-white/15 text-[#264a42] dark:text-[#a8cfc3]'
                : 'text-[#717976] dark:text-[#c1c8c5]'
            }`}
            title="Appearance settings"
          >
            <Type className="w-5 h-5" />
            {settings.theme === 'sepia' && (
              <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-[#264a42] rounded-full" />
            )}
          </button>

          {/* More Options */}
          <div className="relative">
            <button
              id="reader-more-btn"
              onClick={() => setShowMoreMenu(!showMoreMenu)}
              className="p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors text-[#717976] dark:text-[#c1c8c5]"
              title="More options"
            >
              <MoreVertical className="w-5 h-5" />
            </button>

            {showMoreMenu && (
              <>
                <div
                  className="fixed inset-0 z-30"
                  onClick={() => setShowMoreMenu(false)}
                />
                <div className="absolute right-0 top-full mt-1.5 w-48 bg-white dark:bg-[#2f3130] rounded-xl shadow-xl border border-[#c1c8c5]/40 py-1.5 z-40 text-xs font-inter">
                  <button
                    onClick={() => {
                      setShowMoreMenu(false);
                      setIsImmersive(true);
                    }}
                    className="w-full px-3 py-2 text-left flex items-center gap-2 hover:bg-[#f4f4f2] dark:hover:bg-[#383c3a] text-[#1a1c1b] dark:text-[#f1f1ef]"
                  >
                    <Sliders className="w-4 h-4 text-[#3e6259]" />
                    <span>Enter Immersive Mode</span>
                  </button>
                  <button
                    onClick={() => {
                      setShowMoreMenu(false);
                      handlePageChange(1);
                    }}
                    className="w-full px-3 py-2 text-left flex items-center gap-2 hover:bg-[#f4f4f2] dark:hover:bg-[#383c3a] text-[#1a1c1b] dark:text-[#f1f1ef]"
                  >
                    <RotateCcw className="w-4 h-4 text-[#717976]" />
                    <span>Reset Reading to Start</span>
                  </button>
                  <button
                    onClick={() => {
                      setShowMoreMenu(false);
                      window.print();
                    }}
                    className="w-full px-3 py-2 text-left flex items-center gap-2 hover:bg-[#f4f4f2] dark:hover:bg-[#383c3a] text-[#1a1c1b] dark:text-[#f1f1ef]"
                  >
                    <Printer className="w-4 h-4 text-[#717976]" />
                    <span>Print Document</span>
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </header>

      {/* GESTURE / TAP ZONES (Tap left for prev page, center for toggle chrome, right for next page) */}
      <div className="fixed inset-0 z-10 flex pointer-events-none">
        <div
          onClick={() => handlePageChange(currentPage - 1)}
          className="w-[15%] md:w-[20%] h-full pointer-events-auto cursor-w-resize"
          title="Previous Page"
        />
        <div
          onClick={handleCanvasClick}
          className="w-[70%] md:w-[60%] h-full pointer-events-auto"
          title="Toggle reader chrome"
        />
        <div
          onClick={() => handlePageChange(currentPage + 1)}
          className="w-[15%] md:w-[20%] h-full pointer-events-auto cursor-e-resize"
          title="Next Page"
        />
      </div>

      {/* MAIN READING CANVAS */}
      <main
        ref={canvasRef}
        onClick={handleCanvasClick}
        className="w-full max-w-[800px] mx-auto px-6 md:px-12 pt-28 pb-40 z-20 relative cursor-text transition-all duration-300"
      >
        <article className={`space-y-8 ${getTypefaceClass()}`}>
          {/* Header & Badges */}
          <header className="mb-10">
            <div className="flex items-center gap-3 mb-4">
              {document.type === 'pdf' && (
                <span className="px-2 py-0.5 bg-[#E55B3C] text-white font-ibm text-[11px] font-semibold tracking-wider rounded uppercase">
                  PDF
                </span>
              )}
              {document.type === 'md' && (
                <span className="px-2 py-0.5 bg-[#7C5EB1] text-white font-ibm text-[11px] font-semibold tracking-wider rounded uppercase">
                  MD
                </span>
              )}
              {document.type === 'ipynb' && (
                <span className="px-2 py-0.5 bg-[#D99026] text-white font-ibm text-[11px] font-semibold tracking-wider rounded uppercase">
                  IPYNB
                </span>
              )}
              {document.content.arxivId && (
                <span className="font-mono text-[13px] text-[#717976] dark:text-[#a8cfc3]">
                  {document.content.arxivId}
                </span>
              )}
            </div>

            <h2 className="text-[32px] md:text-[40px] font-bold tracking-tight font-inter leading-tight">
              {document.title.replace(/\.(pdf|md|ipynb)$/, '')}
            </h2>

            {/* Author and Published Metadata */}
            <div className="flex flex-wrap items-center gap-3 text-sm text-[#717976] dark:text-[#c1c8c5] mt-3 pb-4 border-b border-[#c1c8c5]/30">
              {document.content.authors && <span>{document.content.authors}</span>}
              {document.content.publishedDate && (
                <>
                  <span>•</span>
                  <span>{document.content.publishedDate}</span>
                </>
              )}
              {document.content.readingTime && (
                <>
                  <span>•</span>
                  <span>{document.content.readingTime}</span>
                </>
              )}
            </div>
          </header>

          {/* Document Content Sections */}
          {document.content.sections.map((sec, secIdx) => (
            <div key={sec.id || secIdx} className="space-y-6">
              {sec.heading && (
                <h3 className="text-[24px] font-bold font-inter mt-10 mb-4 text-[#1a1c1b] dark:text-[#f1f1ef]">
                  {sec.heading}
                </h3>
              )}

              {/* Paragraphs with Drop Caps on the first paragraph of first section */}
              {sec.paragraphs.map((pText, pIdx) => {
                const isFirst = secIdx === 0 && pIdx === 0;
                return (
                  <p
                    key={pIdx}
                    style={{ fontSize: `${settings.fontSize}px`, lineHeight: settings.lineHeight === 'relaxed' ? '1.75' : '1.5' }}
                    className={`leading-relaxed ${
                      isFirst
                        ? 'first-letter:text-5xl first-letter:font-bold first-letter:float-left first-letter:mr-3 first-letter:mt-1 first-letter:text-[#264a42] dark:first-letter:text-[#a8cfc3]'
                        : ''
                    }`}
                  >
                    {pText}
                  </p>
                );
              })}

              {/* Blockquote / Callout */}
              {sec.quote && (
                <blockquote className={`my-8 p-6 rounded-r-lg ${getQuoteCardClasses()}`}>
                  <p className="italic text-lg mb-2">
                    "{sec.quote.text}"
                  </p>
                  {(sec.quote.author || sec.quote.citation) && (
                    <div className="text-xs font-ibm uppercase tracking-wider font-semibold text-[#717976] dark:text-[#a8cfc3] mt-2">
                      — {sec.quote.author} {sec.quote.citation ? `, ${sec.quote.citation}` : ''}
                    </div>
                  )}
                </blockquote>
              )}

              {/* Technical Figure Diagram */}
              {sec.figure && (
                <figure className="my-8 p-4 bg-[#f4f4f2] dark:bg-[#272a29] rounded-lg border border-[#c1c8c5]/50 interactive-control">
                  <div className="w-full h-72 md:h-80 bg-cover bg-center rounded-md border border-[#c1c8c5]/40 shadow-xs overflow-hidden">
                    <img
                      src={sec.figure.src}
                      alt={sec.figure.alt}
                      className="w-full h-full object-contain bg-white dark:bg-[#202221]"
                      referrerPolicy="no-referrer"
                    />
                  </div>
                  <figcaption className="font-mono text-xs text-[#717976] dark:text-[#c1c8c5] mt-3 text-center">
                    {sec.figure.caption}
                  </figcaption>
                </figure>
              )}

              {/* Interactive Jupyter Code Cells */}
              {sec.codeCells && sec.codeCells.map((cell) => (
                <div key={cell.id} className="my-6 space-y-3 interactive-control">
                  {/* Code Input Cell */}
                  <div className="flex items-start gap-3">
                    <div className="w-14 shrink-0 text-right font-mono text-xs text-[#717976] dark:text-[#a8cfc3] mt-3">
                      In [{cell.inputNumber}]:
                    </div>
                    <div className="flex-grow bg-[#f4f4f2] dark:bg-[#272a29] rounded-lg border border-[#c1c8c5]/50 p-4 overflow-x-auto shadow-xs relative group">
                      <button
                        onClick={() => handleRunCodeCell(cell.id)}
                        className="absolute top-3 right-3 p-1.5 rounded-md bg-[#264a42] text-white opacity-80 hover:opacity-100 transition-all flex items-center gap-1 text-[11px] font-mono shadow-xs"
                        title="Execute Code Cell"
                      >
                        <Play className={`w-3 h-3 ${cellRunningId === cell.id ? 'animate-spin' : ''}`} />
                        <span>Run</span>
                      </button>
                      <pre className="font-mono text-[13px] text-[#1a1c1b] dark:text-[#f1f1ef] leading-relaxed">
                        <code>{cell.code}</code>
                      </pre>
                    </div>
                  </div>

                  {/* Code Output View (Image plot or Text values) */}
                  {cell.outputType === 'image' && cell.outputImage && (
                    <div className="flex items-start gap-3">
                      <div className="w-14 shrink-0" />
                      <div className="flex-grow bg-white dark:bg-[#202221] rounded-lg border border-[#c1c8c5]/40 p-4 flex flex-col items-center shadow-xs">
                        <img
                          src={cell.outputImage}
                          alt={cell.outputAlt || 'Plot output'}
                          className="max-w-full h-auto rounded"
                          referrerPolicy="no-referrer"
                        />
                        <span className="font-mono text-[11px] text-[#717976] mt-2">
                          Matplotlib Visual Output [Plot 1/1]
                        </span>
                      </div>
                    </div>
                  )}

                  {cell.outputText && (
                    <div className="flex items-start gap-3">
                      <div className="w-14 shrink-0 font-mono text-xs text-[#717976] text-right mt-1">
                        Out [{cell.inputNumber}]:
                      </div>
                      <div className="flex-grow px-3 py-2 bg-[#eeeeec] dark:bg-[#242726] rounded font-mono text-[13px] text-[#1a1c1b] dark:text-[#f1f1ef]">
                        <pre>{cell.outputText}</pre>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          ))}
        </article>
      </main>

      {/* BOTTOM BAR (Reader Chrome Scrubber) */}
      <footer
        id="reader-bottom-bar"
        className={`fixed bottom-0 left-0 right-0 z-40 border-t backdrop-blur-md h-16 px-4 md:px-8 flex items-center justify-between transition-all duration-300 ${getHeaderFooterClasses()} ${
          isImmersive ? 'translate-y-full opacity-0 pointer-events-none' : 'translate-y-0 opacity-100'
        }`}
      >
        <div className="w-full max-w-4xl mx-auto flex items-center gap-4 md:gap-8">
          {/* Left: Page Indicator */}
          <div className="shrink-0 w-24 text-left font-mono text-[13px] font-semibold text-[#717976] dark:text-[#c1c8c5]">
            Pg {currentPage} / {totalPages}
          </div>

          {/* Center: Scrubber / Slider with Bookmark Ticks */}
          <div className="flex-1 flex items-center gap-3">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              aria-label="Previous Page"
              className="p-1 text-[#717976] hover:text-[#264a42] dark:hover:text-[#a8cfc3] transition-colors"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>

            {/* Slider track with bookmarks tick marks */}
            <div className="flex-1 h-2 bg-[#e2e2e1] dark:bg-[#414846] rounded-full relative cursor-pointer group flex items-center">
              {/* Progress Fill */}
              <div
                className="absolute top-0 left-0 h-full bg-[#264a42] dark:bg-[#a8cfc3] rounded-full transition-all duration-150"
                style={{ width: `${(currentPage / totalPages) * 100}%` }}
              />

              {/* Bookmark Tick marks on track */}
              {document.bookmarks.map((bm) => (
                <div
                  key={bm.id}
                  style={{ left: `${(bm.pageNumber / totalPages) * 100}%` }}
                  className="absolute w-1 h-3 bg-[#E55B3C] rounded-full pointer-events-none z-10 -translate-x-1/2"
                  title={`Bookmark at page ${bm.pageNumber}`}
                />
              ))}

              {/* Interactive Native Range input over track */}
              <input
                id="reader-page-scrubber"
                type="range"
                min={1}
                max={totalPages}
                value={currentPage}
                onChange={(e) => handlePageChange(parseInt(e.target.value, 10))}
                className="absolute inset-0 opacity-0 cursor-pointer w-full h-full z-20"
              />

              {/* Visual Thumb */}
              <div
                style={{ left: `${(currentPage / totalPages) * 100}%` }}
                className="absolute w-4 h-4 bg-white dark:bg-[#386e60] shadow-md border border-[#c1c8c5] rounded-full pointer-events-none -translate-x-1/2 group-hover:scale-125 transition-transform"
              />
            </div>

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              aria-label="Next Page"
              className="p-1 text-[#717976] hover:text-[#264a42] dark:hover:text-[#a8cfc3] transition-colors"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>

          {/* Right: Bookmarks Drawer Trigger */}
          <div className="shrink-0 flex justify-end">
            <button
              id="reader-open-bookmarks-btn"
              onClick={() => setShowBookmarksDrawer(true)}
              className="flex items-center gap-1.5 text-[#264a42] dark:text-[#a8cfc3] hover:bg-[#264a42]/10 px-3 py-1.5 rounded-lg transition-colors font-ibm text-xs font-semibold uppercase"
            >
              <BookmarkIcon className="w-4 h-4" />
              <span className="hidden sm:inline">Bookmarks ({document.bookmarks.length})</span>
            </button>
          </div>
        </div>
      </footer>

      {/* Floating Progress Line (Absolute Bottom Edge) */}
      <div
        className="fixed bottom-0 left-0 h-[2px] bg-[#264a42] dark:bg-[#a8cfc3] z-50 transition-all duration-200 pointer-events-none"
        style={{ width: `${(currentPage / totalPages) * 100}%` }}
      />

      {/* APPEARANCE SETTINGS SLIDE-UP SHEET (Matching Mockup 11 & 12) */}
      {showAppearanceSheet && (
        <>
          <div
            id="settings-scrim"
            onClick={() => setShowAppearanceSheet(false)}
            className="fixed inset-0 bg-black/30 z-50 animate-fade-in backdrop-blur-xs"
          />
          <div className="fixed bottom-0 left-0 right-0 z-50 flex justify-center animate-slide-up pointer-events-auto">
            <div className="bg-[#f9f9f7] dark:bg-[#242726] rounded-t-3xl shadow-2xl w-full max-w-[620px] border-t border-x border-[#c1c8c5]/40 pb-8 px-6 md:px-8 pt-4">
              {/* Drag Handle */}
              <div className="w-full flex justify-center pb-4">
                <div className="w-12 h-1.5 bg-[#c1c8c5] rounded-full" />
              </div>

              <div className="flex items-center justify-between mb-6">
                <h3 className="text-[18px] font-bold text-[#1a1c1b] dark:text-[#f1f1ef] font-inter">
                  Appearance Settings
                </h3>
                <button
                  onClick={() => setShowAppearanceSheet(false)}
                  className="p-1 rounded-full text-[#717976] hover:bg-black/5"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Color Theme Selection */}
              <div className="mb-6">
                <div className="text-xs font-semibold font-ibm text-[#717976] dark:text-[#c1c8c5] uppercase tracking-wider mb-2.5">
                  Color Theme
                </div>
                <div className="grid grid-cols-3 gap-3">
                  {/* Light Theme */}
                  <button
                    onClick={() => setSettings((s) => ({ ...s, theme: 'light' }))}
                    className={`h-20 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all relative ${
                      settings.theme === 'light'
                        ? 'border-2 border-[#264a42] bg-white shadow-sm'
                        : 'border-[#c1c8c5]/60 bg-white/70 hover:bg-white'
                    }`}
                  >
                    <div className="w-6 h-6 rounded-full border border-[#c1c8c5] bg-[#f9f9f7]" />
                    <span className="font-mono text-xs font-semibold text-[#1a1c1b]">Light</span>
                    {settings.theme === 'light' && (
                      <CheckCircle className="w-4 h-4 text-[#264a42] absolute top-2 right-2" />
                    )}
                  </button>

                  {/* Sepia Theme */}
                  <button
                    onClick={() => setSettings((s) => ({ ...s, theme: 'sepia' }))}
                    className={`h-20 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all relative ${
                      settings.theme === 'sepia'
                        ? 'border-2 border-[#264a42] bg-[#FDF9EE] shadow-sm'
                        : 'border-[#E3D5B1] bg-[#F5EEDC] hover:bg-[#FDF9EE]'
                    }`}
                  >
                    <div className="w-6 h-6 rounded-full border border-[#E3D5B1] bg-[#F5EEDC]" />
                    <span className="font-mono text-xs font-semibold text-[#3D3828]">Sepia</span>
                    {settings.theme === 'sepia' && (
                      <CheckCircle className="w-4 h-4 text-[#264a42] absolute top-2 right-2" />
                    )}
                  </button>

                  {/* Dark Theme */}
                  <button
                    onClick={() => setSettings((s) => ({ ...s, theme: 'dark' }))}
                    className={`h-20 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all relative ${
                      settings.theme === 'dark'
                        ? 'border-2 border-[#a8cfc3] bg-[#242726] shadow-sm'
                        : 'border-[#414846] bg-[#1a1c1b] hover:bg-[#242726]'
                    }`}
                  >
                    <div className="w-6 h-6 rounded-full border border-[#414846] bg-[#1a1c1b]" />
                    <span className="font-mono text-xs font-semibold text-[#f1f1ef]">Dark</span>
                    {settings.theme === 'dark' && (
                      <CheckCircle className="w-4 h-4 text-[#a8cfc3] absolute top-2 right-2" />
                    )}
                  </button>
                </div>
              </div>

              {/* Font Size Slider */}
              <div className="mb-6 bg-[#eeeeec] dark:bg-[#2f3130] rounded-xl p-4">
                <div className="flex justify-between items-center mb-3">
                  <span className="text-xs font-semibold font-ibm text-[#717976] dark:text-[#c1c8c5] uppercase tracking-wider">
                    Font Size
                  </span>
                  <span className="font-mono text-xs font-bold text-[#1a1c1b] dark:text-[#f1f1ef]">
                    {settings.fontSize}px
                  </span>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-xs font-bold text-[#717976]">A-</span>
                  <input
                    id="font-size-slider"
                    type="range"
                    min={14}
                    max={32}
                    value={settings.fontSize}
                    onChange={(e) =>
                      setSettings((s) => ({ ...s, fontSize: parseInt(e.target.value, 10) }))
                    }
                    className="flex-1 accent-[#264a42] cursor-pointer"
                  />
                  <span className="text-base font-bold text-[#717976]">A+</span>
                </div>
              </div>

              {/* Typeface Selection */}
              <div>
                <div className="text-xs font-semibold font-ibm text-[#717976] dark:text-[#c1c8c5] uppercase tracking-wider mb-2.5">
                  Typeface
                </div>
                <div className="grid grid-cols-3 gap-2.5">
                  {(['Literata', 'Inter', 'IBM Plex Sans'] as Typeface[]).map((tf) => (
                    <button
                      key={tf}
                      onClick={() => setSettings((s) => ({ ...s, typeface: tf }))}
                      className={`py-3 px-3 rounded-xl border flex items-center justify-center gap-2 transition-all ${
                        settings.typeface === tf
                          ? 'border-2 border-[#264a42] bg-[#c3ebdf]/30 text-[#264a42] dark:text-[#a8cfc3] font-bold shadow-xs'
                          : 'border-[#c1c8c5]/60 bg-white dark:bg-[#2f3130] text-[#717976] hover:border-[#717976]'
                      }`}
                    >
                      <span className="text-base">Aa</span>
                      <span className="text-xs truncate">{tf}</span>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </>
      )}

      {/* TABLE OF CONTENTS DRAWER */}
      {showTocDrawer && (
        <div className="fixed inset-0 z-50 flex">
          <div
            className="fixed inset-0 bg-black/30 backdrop-blur-xs"
            onClick={() => setShowTocDrawer(false)}
          />
          <div className="relative w-full max-w-sm bg-white dark:bg-[#242726] h-full shadow-2xl z-10 flex flex-col border-r border-[#c1c8c5]/40 animate-fade-in">
            <div className="p-5 border-b border-[#c1c8c5]/40 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-[#264a42] dark:text-[#a8cfc3]" />
                <h3 className="font-bold text-base font-inter text-[#1a1c1b] dark:text-[#f1f1ef]">
                  Table of Contents
                </h3>
              </div>
              <button
                onClick={() => setShowTocDrawer(false)}
                className="p-1 rounded-full hover:bg-black/5 text-[#717976]"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-1.5">
              {document.toc.map((item) => (
                <button
                  key={item.id}
                  onClick={() => {
                    handlePageChange(item.pageNumber);
                    setShowTocDrawer(false);
                  }}
                  className={`w-full text-left p-3 rounded-lg flex items-center justify-between text-sm transition-colors ${
                    currentPage >= item.pageNumber &&
                    (item === document.toc[document.toc.length - 1] ||
                      currentPage < (document.toc[document.toc.indexOf(item) + 1]?.pageNumber || 999))
                      ? 'bg-[#264a42]/10 text-[#264a42] dark:text-[#a8cfc3] font-bold'
                      : 'text-[#414846] dark:text-[#c1c8c5] hover:bg-[#f4f4f2] dark:hover:bg-[#2f3130]'
                  }`}
                  style={{ paddingLeft: `${item.level * 14}px` }}
                >
                  <span className="truncate pr-2">{item.title}</span>
                  <span className="font-mono text-xs text-[#717976] shrink-0">
                    p. {item.pageNumber}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* BOOKMARKS DRAWER */}
      {showBookmarksDrawer && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div
            className="fixed inset-0 bg-black/30 backdrop-blur-xs"
            onClick={() => setShowBookmarksDrawer(false)}
          />
          <div className="relative w-full max-w-md bg-white dark:bg-[#242726] h-full shadow-2xl z-10 flex flex-col border-l border-[#c1c8c5]/40 animate-fade-in">
            <div className="p-5 border-b border-[#c1c8c5]/40 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <BookmarkIcon className="w-5 h-5 text-[#264a42] dark:text-[#a8cfc3]" />
                <h3 className="font-bold text-base font-inter text-[#1a1c1b] dark:text-[#f1f1ef]">
                  Saved Bookmarks
                </h3>
              </div>
              <button
                onClick={() => setShowBookmarksDrawer(false)}
                className="p-1 rounded-full hover:bg-black/5 text-[#717976]"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-3">
              {document.bookmarks.length === 0 ? (
                <div className="text-center py-16 text-[#717976]">
                  <BookmarkPlus className="w-10 h-10 mx-auto mb-2 opacity-50" />
                  <p className="text-sm font-medium">No bookmarks in this document</p>
                  <p className="text-xs mt-1">Tap the bookmark icon on any page to save an anchor.</p>
                </div>
              ) : (
                document.bookmarks.map((bm) => (
                  <div
                    key={bm.id}
                    onClick={() => {
                      handlePageChange(bm.pageNumber);
                      setShowBookmarksDrawer(false);
                    }}
                    className="p-3.5 bg-[#f9f9f7] dark:bg-[#2f3130] rounded-xl border border-[#c1c8c5]/40 hover:border-[#264a42] cursor-pointer transition-all shadow-xs"
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="font-mono text-xs font-bold text-[#264a42] dark:text-[#a8cfc3]">
                        Page {bm.pageNumber}
                      </span>
                      <span className="text-[11px] text-[#717976]">{bm.timestamp}</span>
                    </div>
                    <h4 className="text-sm font-semibold text-[#1a1c1b] dark:text-[#f1f1ef] mb-1 font-inter">
                      {bm.title}
                    </h4>
                    <p className="text-xs text-[#717976] line-clamp-2 leading-relaxed">
                      "{bm.snippet}"
                    </p>
                  </div>
                ))
              )}
            </div>

            <div className="p-4 border-t border-[#c1c8c5]/40 bg-[#f9f9f7] dark:bg-[#202221]">
              <button
                onClick={handleToggleCurrentBookmark}
                className="w-full py-2.5 px-4 bg-[#264a42] text-white rounded-lg font-medium text-xs flex items-center justify-center gap-2 hover:bg-[#31685a] transition-colors"
              >
                <BookmarkPlus className="w-4 h-4" />
                <span>Bookmark Current Page ({currentPage})</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SEARCH MODAL */}
      {showSearchModal && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-20 p-4 bg-black/40 backdrop-blur-xs">
          <div className="bg-white dark:bg-[#242726] w-full max-w-lg rounded-2xl shadow-2xl border border-[#c1c8c5]/40 p-5">
            <div className="flex items-center gap-3 border-b border-[#c1c8c5]/40 pb-3">
              <Search className="w-5 h-5 text-[#717976]" />
              <input
                type="text"
                placeholder="Search across this document..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-1 bg-transparent text-sm text-[#1a1c1b] dark:text-[#f1f1ef] outline-none"
                autoFocus
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="text-xs text-[#717976] hover:text-[#1a1c1b]"
                >
                  Clear
                </button>
              )}
              <button
                onClick={() => setShowSearchModal(false)}
                className="p-1 rounded-full text-[#717976] hover:bg-black/5"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-4 max-h-60 overflow-y-auto space-y-2">
              {searchQuery.trim().length === 0 ? (
                <p className="text-xs text-[#717976] text-center py-6">
                  Type words to search within headings and paragraphs.
                </p>
              ) : searchMatchesCount === 0 ? (
                <p className="text-xs text-[#717976] text-center py-6">
                  No matching passages found for "{searchQuery}".
                </p>
              ) : (
                <div className="space-y-2">
                  <div className="text-xs font-mono text-[#264a42] dark:text-[#a8cfc3] font-semibold px-1">
                    Found {searchMatchesCount} matching instances:
                  </div>
                  {document.content.sections.map((sec, idx) => {
                    const matches = sec.paragraphs.filter((p) =>
                      p.toLowerCase().includes(searchQuery.toLowerCase())
                    );
                    if (matches.length === 0) return null;
                    return (
                      <div
                        key={idx}
                        onClick={() => setShowSearchModal(false)}
                        className="p-2.5 bg-[#f9f9f7] dark:bg-[#2f3130] rounded-lg text-xs cursor-pointer hover:bg-[#264a42]/10 transition-colors"
                      >
                        <div className="font-bold text-[#1a1c1b] dark:text-[#f1f1ef] mb-1">
                          {sec.heading || `Section ${idx + 1}`}
                        </div>
                        <div className="text-[#717976] line-clamp-2">
                          {matches[0]}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
