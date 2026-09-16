
    // ==========================================
    // 6C. CARD 8: UNIVERSAL MEDIA DOWNLOADER & HARVESTER
    // ==========================================
    private View createMediaDownloaderContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));

        // 1. Storage & Space Overview Card
        LinearLayout storageCard = createCardLayout(Color.parseColor("#F5F3FF"), Color.parseColor("#DDD6FE"));
        storageCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        storageCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout sHeaderRow = new LinearLayout(this);
        sHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        sHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView sTitle = new TextView(this);
        sTitle.setText("📁 MEDIASTORE: Movies/RoseMedia • Music/RoseMedia");
        sTitle.setTextSize(10);
        sTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sTitle.setTextColor(Color.parseColor("#7C3AED"));
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        sTitle.setLayoutParams(stLp);
        sHeaderRow.addView(sTitle);

        TextView storageSpaceBadge = new TextView(this);
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long freeBytes = stat.getAvailableBytes();
            storageSpaceBadge.setText("💾 " + DownloadJob.formatBytes(freeBytes) + " Free");
        } catch (Exception e) {
            storageSpaceBadge.setText("💾 Ready");
        }
        storageSpaceBadge.setTextSize(9);
        storageSpaceBadge.setTypeface(Typeface.DEFAULT_BOLD);
        storageSpaceBadge.setTextColor(Color.parseColor("#7C3AED"));
        storageSpaceBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        storageSpaceBadge.setBackground(createPillDrawable(Color.WHITE, dp(4)));
        sHeaderRow.addView(storageSpaceBadge);
        storageCard.addView(sHeaderRow);

        TextView sSub = new TextView(this);
        sSub.setText("Universal media downloader with zero restrictions. YouTube HD, Instagram, X/Twitter, TikTok, Reddit, Facebook, Pinterest & Web HTML5 streams.");
        sSub.setTextSize(10);
        sSub.setTextColor(Color.parseColor("#6B7280"));
        sSub.setPadding(0, dp(4), 0, 0);
        storageCard.addView(sSub);
        content.addView(storageCard);

        // 2. URL Input & Extraction Card
        LinearLayout inputCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        inputCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        icLp.setMargins(0, dp(12), 0, 0);
        inputCard.setLayoutParams(icLp);

        TextView inTitle = new TextView(this);
        inTitle.setText("🔗 UNIVERSAL MEDIA EXTRACTOR");
        inTitle.setTextSize(11);
        inTitle.setTypeface(Typeface.DEFAULT_BOLD);
        inTitle.setTextColor(Color.parseColor("#0F172A"));
        inputCard.addView(inTitle);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        irLp.setMargins(0, dp(8), 0, 0);
        inputRow.setLayoutParams(irLp);

        downloaderUrlInput = new EditText(this);
        downloaderUrlInput.setHint("Paste any link (YouTube, X, TikTok, Reddit, FB, Web)...");
        downloaderUrlInput.setTextSize(11);
        downloaderUrlInput.setSingleLine(true);
        downloaderUrlInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        downloaderUrlInput.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        downloaderUrlInput.setHintTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#94A3B8"));
        downloaderUrlInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(isDarkTheme ? 0xFF2A2D2E : Color.parseColor("#F8FAFC"));
        inBg.setCornerRadius(dp(8));
        inBg.setStroke(dp(1), isDarkTheme ? 0xFF3C3C3C : Color.parseColor("#CBD5E1"));
        downloaderUrlInput.setBackground(inBg);
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        downloaderUrlInput.setLayoutParams(editLp);
        inputRow.addView(downloaderUrlInput);

        PremiumButton pasteBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
        pasteBtn.setText("📋 Paste");
        pasteBtn.setTextSize(11);
        pasteBtn.setCustomAccent(isDarkTheme ? DesignTokens.ACCENT_INDIGO_DARK : DesignTokens.ACCENT_INDIGO_PRIMARY);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        pbLp.setMargins(dp(6), 0, 0, 0);
        pasteBtn.setLayoutParams(pbLp);
        pasteBtn.setOnClickListener(v -> pasteFromClipboard());
        inputRow.addView(pasteBtn);

        PremiumIconButton clearBtn = new PremiumIconButton(this, "✕", isDarkTheme);
        clearBtn.setTextSize(14);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        cbLp.setMargins(dp(4), 0, 0, 0);
        clearBtn.setLayoutParams(cbLp);
        clearBtn.setOnClickListener(v -> {
            if (downloaderUrlInput != null) downloaderUrlInput.setText("");
            if (downloaderSourceBadge != null) downloaderSourceBadge.setVisibility(View.GONE);
            if (downloaderPreviewCard != null) downloaderPreviewCard.setVisibility(View.GONE);
        });
        inputRow.addView(clearBtn);
        inputCard.addView(inputRow);

        // Auto-detect source badge
        downloaderSourceBadge = new TextView(this);
        downloaderSourceBadge.setVisibility(View.GONE);
        downloaderSourceBadge.setTextSize(9);
        downloaderSourceBadge.setTypeface(Typeface.DEFAULT_BOLD);
        downloaderSourceBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.setMargins(0, dp(8), 0, 0);
        downloaderSourceBadge.setLayoutParams(sbLp);
        inputCard.addView(downloaderSourceBadge);

        // Listen for typing to show source badge
        downloaderUrlInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString().trim();
                if (text.length() > 5) {
                    MediaSource src = MediaSource.detectFromUrl(text);
                    downloaderSourceBadge.setVisibility(View.VISIBLE);
                    downloaderSourceBadge.setText(src.icon + " " + src.displayName);
                    downloaderSourceBadge.setTextColor(Color.parseColor(src.colorHex));
                    downloaderSourceBadge.setBackground(createPillDrawable(isDarkTheme ? 0xFF2A2D2E : Color.parseColor("#F8FAFC"), dp(6)));
                } else {
                    downloaderSourceBadge.setVisibility(View.GONE);
                }
            }
        });

        // Analyze Button
        downloaderAnalyzeBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
        downloaderAnalyzeBtn.setText("⚡ ANALYZE & EXTRACT STREAMS");
        downloaderAnalyzeBtn.setTextSize(12);
        downloaderAnalyzeBtn.setCustomAccent(isDarkTheme ? DesignTokens.ACCENT_INDIGO_DARK : DesignTokens.ACCENT_INDIGO_PRIMARY);
        LinearLayout.LayoutParams abLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        abLp.setMargins(0, dp(12), 0, 0);
        downloaderAnalyzeBtn.setLayoutParams(abLp);
        downloaderAnalyzeBtn.setOnClickListener(v -> {
            if (downloaderUrlInput != null) {
                String raw = downloaderUrlInput.getText().toString().trim();
                triggerAnalyzeUrl(raw);
            }
        });
        inputCard.addView(downloaderAnalyzeBtn);

        // Loading indicator & status
        downloaderLoadingBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        downloaderLoadingBar.setIndeterminate(true);
        downloaderLoadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        lbLp.setMargins(0, dp(8), 0, 0);
        downloaderLoadingBar.setLayoutParams(lbLp);
        inputCard.addView(downloaderLoadingBar);

        downloaderStatusView = new TextView(this);
        downloaderStatusView.setTextSize(10);
        downloaderStatusView.setTextColor(Color.parseColor("#64748B"));
        downloaderStatusView.setVisibility(View.GONE);
        downloaderStatusView.setPadding(0, dp(4), 0, 0);
        inputCard.addView(downloaderStatusView);

        content.addView(inputCard);

        // 3. Media Preview & Resolution Picker Card (Initially Hidden)
        downloaderPreviewCard = createCardLayout(Color.WHITE, Color.parseColor("#DDD6FE"));
        downloaderPreviewCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        downloaderPreviewCard.setVisibility(View.GONE);
        LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpLp.setMargins(0, dp(12), 0, 0);
        downloaderPreviewCard.setLayoutParams(dpLp);

        downloaderTitleView = new TextView(this);
        downloaderTitleView.setTextSize(12);
        downloaderTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        downloaderTitleView.setTextColor(Color.parseColor("#0F172A"));
        downloaderPreviewCard.addView(downloaderTitleView);

        downloaderMetaBadgeView = new TextView(this);
        downloaderMetaBadgeView.setTextSize(10);
        downloaderMetaBadgeView.setTextColor(Color.parseColor("#64748B"));
        downloaderMetaBadgeView.setPadding(0, dp(2), 0, dp(8));
        downloaderPreviewCard.addView(downloaderMetaBadgeView);

        downloaderThumbView = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170));
        ivLp.setMargins(0, 0, 0, dp(10));
        downloaderThumbView.setLayoutParams(ivLp);
        downloaderThumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setColor(Color.parseColor("#F1F5F9"));
        thumbBg.setCornerRadius(dp(10));
        downloaderThumbView.setBackground(thumbBg);
        if (Build.VERSION.SDK_INT >= 21) {
            downloaderThumbView.setClipToOutline(true);
        }
        downloaderPreviewCard.addView(downloaderThumbView);

        downloaderFormatsContainer = new LinearLayout(this);
        downloaderFormatsContainer.setOrientation(LinearLayout.VERTICAL);
        downloaderPreviewCard.addView(downloaderFormatsContainer);

        content.addView(downloaderPreviewCard);

        // 4. WhatsApp Status Harvester Card
        LinearLayout waCard = createCardLayout(Color.parseColor("#F0FDF4"), Color.parseColor("#BBF7D0"));
        waCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams waLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        waLp.setMargins(0, dp(12), 0, 0);
        waCard.setLayoutParams(waLp);

        LinearLayout waHeaderRow = new LinearLayout(this);
        waHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        waHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView waTitle = new TextView(this);
        waTitle.setText("💬 WHATSAPP STATUS HARVESTER");
        waTitle.setTextSize(11);
        waTitle.setTypeface(Typeface.DEFAULT_BOLD);
        waTitle.setTextColor(Color.parseColor("#15803D"));
        LinearLayout.LayoutParams wtLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        waTitle.setLayoutParams(wtLp);
        waHeaderRow.addView(waTitle);

        whatsappStatusCountBadge = new TextView(this);
        whatsappStatusCountBadge.setText("0 Cached");
        whatsappStatusCountBadge.setTextSize(9);
        whatsappStatusCountBadge.setTypeface(Typeface.DEFAULT_BOLD);
        whatsappStatusCountBadge.setTextColor(Color.parseColor("#15803D"));
        whatsappStatusCountBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        whatsappStatusCountBadge.setBackground(createPillDrawable(Color.WHITE, dp(4)));
        waHeaderRow.addView(whatsappStatusCountBadge);
        waCard.addView(waHeaderRow);

        TextView waSub = new TextView(this);
        waSub.setText("Instant offline status viewer. Save any viewed friend's video or photo status straight to your public gallery.");
        waSub.setTextSize(10);
        waSub.setTextColor(Color.parseColor("#4B5563"));
        waSub.setPadding(0, dp(4), 0, dp(8));
        waCard.addView(waSub);

        PremiumButton scanWaBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
        scanWaBtn.setText("🔄 Scan Cached Statuses");
        scanWaBtn.setTextSize(11);
        LinearLayout.LayoutParams swLp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        scanWaBtn.setLayoutParams(swLp2);
        scanWaBtn.setOnClickListener(v -> refreshWhatsAppStatusesUI());
        waCard.addView(scanWaBtn);

        whatsappStatusContainer = new LinearLayout(this);
        whatsappStatusContainer.setOrientation(LinearLayout.HORIZONTAL);
        whatsappStatusContainer.setPadding(0, dp(10), 0, 0);

        android.widget.HorizontalScrollView waScroll = new android.widget.HorizontalScrollView(this);
        waScroll.setHorizontalScrollBarEnabled(false);
        waScroll.addView(whatsappStatusContainer);
        waCard.addView(waScroll);

        content.addView(waCard);

        // 5. Active Downloads & Queue Manager Card
        LinearLayout queueCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        queueCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams qcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qcLp.setMargins(0, dp(12), 0, 0);
        queueCard.setLayoutParams(qcLp);

        LinearLayout qHeaderRow = new LinearLayout(this);
        qHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        qHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView qTitle = new TextView(this);
        qTitle.setText("📥 DOWNLOAD MANAGER");
        qTitle.setTextSize(11);
        qTitle.setTypeface(Typeface.DEFAULT_BOLD);
        qTitle.setTextColor(Color.parseColor("#0F172A"));
        LinearLayout.LayoutParams qtLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        qTitle.setLayoutParams(qtLp);
        qHeaderRow.addView(qTitle);

        downloaderQueueBadge = new TextView(this);
        downloaderQueueBadge.setText("0 Active");
        downloaderQueueBadge.setTextSize(9);
        downloaderQueueBadge.setTypeface(Typeface.DEFAULT_BOLD);
        downloaderQueueBadge.setTextColor(Color.parseColor("#7C3AED"));
        downloaderQueueBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        downloaderQueueBadge.setBackground(createPillDrawable(Color.parseColor("#F5F3FF"), dp(4)));
        qHeaderRow.addView(downloaderQueueBadge);
        queueCard.addView(qHeaderRow);

        downloaderQueueContainer = new LinearLayout(this);
        downloaderQueueContainer.setOrientation(LinearLayout.VERTICAL);
        queueCard.addView(downloaderQueueContainer);

        content.addView(queueCard);

        renderDownloadQueue();
        refreshWhatsAppStatusesUI();

        return content;
    }

    private void refreshMediaDownloaderUI() {
        renderDownloadQueue();
        refreshWhatsAppStatusesUI();
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
                if (item != null && item.getText() != null) {
                    String clean = item.getText().toString().trim();
                    if (downloaderUrlInput != null) {
                        downloaderUrlInput.setText(clean);
                        downloaderUrlInput.setSelection(clean.length());
                    }
                    triggerAnalyzeUrl(clean);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not paste from clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerAnalyzeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            Toast.makeText(this, "Please enter or paste a valid link", Toast.LENGTH_SHORT).show();
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && downloaderUrlInput != null) {
            imm.hideSoftInputFromWindow(downloaderUrlInput.getWindowToken(), 0);
        }

        downloaderPreviewCard.setVisibility(View.GONE);
        downloaderLoadingBar.setVisibility(View.VISIBLE);
        downloaderStatusView.setVisibility(View.VISIBLE);
        downloaderStatusView.setText("Probing remote streams & resolving media metadata...");
        downloaderAnalyzeBtn.setEnabled(false);

        new Thread(() -> {
            try {
                ExtractedMedia media = MediaResolverRegistry.resolve(rawUrl);
                runOnUiThread(() -> {
                    downloaderLoadingBar.setVisibility(View.GONE);
                    downloaderStatusView.setVisibility(View.GONE);
                    downloaderAnalyzeBtn.setEnabled(true);
                    renderMediaPreview(media);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    downloaderLoadingBar.setVisibility(View.GONE);
                    downloaderStatusView.setVisibility(View.VISIBLE);
                    downloaderStatusView.setText("❌ Analysis Failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                    downloaderAnalyzeBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private void renderMediaPreview(ExtractedMedia media) {
        if (media == null) return;
        downloaderPreviewCard.setVisibility(View.VISIBLE);
        downloaderTitleView.setText(media.title);

        String meta = media.source.icon + " " + media.source.displayName;
        if (media.author != null && !media.author.isEmpty()) {
            meta += " • " + media.author;
        }
        if (media.durationSeconds > 0) {
            meta += " • " + media.getFormattedDuration();
        }
        downloaderMetaBadgeView.setText(meta);

        if (media.thumbnailUrl != null && !media.thumbnailUrl.isEmpty()) {
            downloaderThumbView.setVisibility(View.VISIBLE);
            AsyncThumbnailLoader.load(media.thumbnailUrl, downloaderThumbView);
        } else {
            downloaderThumbView.setVisibility(View.GONE);
        }

        downloaderFormatsContainer.removeAllViews();

        TextView fHeader = new TextView(this);
        fHeader.setText("SELECT DOWNLOAD FORMAT & QUALITY:");
        fHeader.setTextSize(10);
        fHeader.setTypeface(Typeface.DEFAULT_BOLD);
        fHeader.setTextColor(Color.parseColor("#475569"));
        fHeader.setPadding(0, dp(4), 0, dp(6));
        downloaderFormatsContainer.addView(fHeader);

        for (StreamFormat opt : media.streamOptions) {
            downloaderFormatsContainer.addView(createFormatOptionRow(media, opt));
        }
    }

    private View createFormatOptionRow(ExtractedMedia media, StreamFormat opt) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(4), 0, 0);
        row.setLayoutParams(rlp);

        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(isDarkTheme ? 0xFF232526 : Color.parseColor("#F8FAFC"));
        rBg.setCornerRadius(dp(8));
        rBg.setStroke(dp(1), isDarkTheme ? 0xFF353739 : Color.parseColor("#E2E8F0"));
        row.setBackground(rBg);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(tcLp);

        TextView label = new TextView(this);
        label.setText(opt.resolution + " (" + opt.extension.toUpperCase() + ")");
        label.setTextSize(11);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_PRIMARY : Color.parseColor("#0F172A"));
        textCol.addView(label);

        TextView sub = new TextView(this);
        String subText = opt.requiresMuxing ? "Native Hardware Muxed • Zero Quality Loss" : (opt.isAudioOnly ? "Direct Audio Stream" : "Direct Stream");
        if (opt.approximateBytes > 0) {
            subText += " • " + opt.getFormattedSize();
        }
        sub.setText(subText);
        sub.setTextSize(9);
        sub.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
        textCol.addView(sub);
        row.addView(textCol);

        PremiumButton dlBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
        dlBtn.setText("📥 Download");
        dlBtn.setTextSize(11);
        dlBtn.setCustomAccent(isDarkTheme ? DesignTokens.ACCENT_INDIGO_DARK : DesignTokens.ACCENT_INDIGO_PRIMARY);
        LinearLayout.LayoutParams dlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        dlBtn.setLayoutParams(dlLp);
        dlBtn.setOnClickListener(v -> startDownloadFromOption(media, opt));
        row.addView(dlBtn);

        return row;
    }

    private void startDownloadFromOption(ExtractedMedia media, StreamFormat opt) {
        String subDir = "Movies/RoseMedia";
        if (opt.isAudioOnly) {
            subDir = "Music/RoseMedia";
        } else if ("jpg".equalsIgnoreCase(opt.extension) || "png".equalsIgnoreCase(opt.extension)) {
            subDir = "Pictures/RoseMedia";
        }

        String jobId = "dl_" + System.currentTimeMillis();
        DownloadJob job = new DownloadJob(
                jobId,
                media.title,
                media.source,
                opt,
                subDir
        );

        MediaDownloadManager.getInstance(this).enqueueJob(job);
        Toast.makeText(this, "🚀 Download started: " + media.title, Toast.LENGTH_SHORT).show();
        renderDownloadQueue();
    }

    private void refreshWhatsAppStatusesUI() {
        if (whatsappStatusContainer == null) return;
        whatsappStatusContainer.removeAllViews();

        String persistedUriStr = getSharedPreferences("rose_media_prefs", MODE_PRIVATE)
                .getString("saf_whatsapp_uri", null);
        Uri treeUri = (persistedUriStr != null) ? Uri.parse(persistedUriStr) : null;

        List<WhatsAppStatusHarvester.StatusItem> items = WhatsAppStatusHarvester.scanStatuses(this, treeUri);

        if (whatsappStatusCountBadge != null) {
            whatsappStatusCountBadge.setText(items.size() + " Cached");
        }

        if (items.isEmpty()) {
            LinearLayout emptyBox = new LinearLayout(this);
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setPadding(dp(12), dp(8), dp(12), dp(8));

            TextView msg = new TextView(this);
            msg.setText("No cached WhatsApp statuses found in standard paths. If using Android 11–16, grant SAF access to the .Statuses folder.");
            msg.setTextSize(10);
            msg.setTextColor(Color.parseColor("#64748B"));
            emptyBox.addView(msg);

            PremiumButton grantBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
            grantBtn.setText("📂 Grant WhatsApp SAF Access");
            grantBtn.setTextSize(11);
            LinearLayout.LayoutParams gbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            gbLp.setMargins(0, dp(6), 0, 0);
            grantBtn.setLayoutParams(gbLp);
            grantBtn.setOnClickListener(v -> {
                Intent picker = WhatsAppStatusHarvester.createSafFolderPickerIntent();
                startActivityForResult(picker, 707);
            });
            emptyBox.addView(grantBtn);

            whatsappStatusContainer.addView(emptyBox);
            return;
        }

        for (WhatsAppStatusHarvester.StatusItem item : items) {
            whatsappStatusContainer.addView(createWhatsAppStatusCard(item));
        }
    }

    private View createWhatsAppStatusCard(WhatsAppStatusHarvester.StatusItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, dp(8), 0);
        card.setLayoutParams(clp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isDarkTheme ? 0xFF232526 : Color.WHITE);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), isDarkTheme ? 0xFF2E4035 : Color.parseColor("#DCFCE7"));
        card.setBackground(bg);

        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100));
        iv.setLayoutParams(ivLp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(isDarkTheme ? 0xFF2A2D2E : Color.parseColor("#F1F5F9"));
        if (Build.VERSION.SDK_INT >= 21) iv.setClipToOutline(true);

        if (item.file != null) {
            iv.setImageURI(Uri.fromFile(item.file));
        } else if (item.uri != null) {
            iv.setImageURI(item.uri);
        }
        card.addView(iv);

        TextView tag = new TextView(this);
        tag.setText(item.isVideo ? "🎥 Video" : "📸 Photo");
        tag.setTextSize(9);
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        tag.setTextColor(isDarkTheme ? 0xFF4ADE80 : Color.parseColor("#15803D"));
        tag.setPadding(0, dp(4), 0, 0);
        card.addView(tag);

        TextView size = new TextView(this);
        size.setText(item.getFormattedSize());
        size.setTextSize(8);
        size.setTextColor(isDarkTheme ? DesignTokens.DARK_TEXT_SECONDARY : Color.parseColor("#64748B"));
        card.addView(size);

        PremiumButton saveBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
        saveBtn.setText("💾 Save");
        saveBtn.setTextSize(10);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        sbLp.setMargins(0, dp(4), 0, 0);
        saveBtn.setLayoutParams(sbLp);
        saveBtn.setOnClickListener(v -> {
            try {
                Uri saved = WhatsAppStatusHarvester.saveStatusToGallery(this, item);
                Toast.makeText(this, "🎉 Saved to Gallery: " + item.name, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(saveBtn);

        return card;
    }

    private void renderDownloadQueue() {
        if (downloaderQueueContainer == null) return;
        downloaderQueueContainer.removeAllViews();

        List<DownloadJob> jobs = MediaDownloadManager.getInstance(this).getJobs();

        int activeCount = 0;
        for (DownloadJob j : jobs) {
            if (j.state == DownloadJob.State.DOWNLOADING || j.state == DownloadJob.State.MUXING || j.state == DownloadJob.State.QUEUED) {
                activeCount++;
            }
        }
        if (downloaderQueueBadge != null) {
            downloaderQueueBadge.setText(activeCount + " Active • " + jobs.size() + " Total");
        }

        if (jobs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No active or recent downloads. Paste a link above to begin.");
            empty.setTextSize(10);
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setPadding(0, dp(8), 0, 0);
            downloaderQueueContainer.addView(empty);
            return;
        }

        for (DownloadJob job : jobs) {
            downloaderQueueContainer.addView(createTaskQueueRow(job));
        }
    }

    private View createTaskQueueRow(DownloadJob job) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(6), 0, 0);
        row.setLayoutParams(rlp);

        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(Color.parseColor("#F8FAFC"));
        rBg.setCornerRadius(dp(8));
        rBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        row.setBackground(rBg);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(job.source.icon + " " + job.title);
        title.setTextSize(11);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setSingleLine(true);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        title.setLayoutParams(tLp);
        topRow.addView(title);

        TextView stateBadge = new TextView(this);
        stateBadge.setTextSize(9);
        stateBadge.setTypeface(Typeface.DEFAULT_BOLD);
        stateBadge.setPadding(dp(6), dp(2), dp(6), dp(2));

        if (job.state == DownloadJob.State.COMPLETED) {
            stateBadge.setText("✅ COMPLETED");
            stateBadge.setTextColor(Color.parseColor("#15803D"));
            stateBadge.setBackground(createPillDrawable(Color.parseColor("#DCFCE7"), dp(4)));
        } else if (job.state == DownloadJob.State.MUXING) {
            stateBadge.setText("⚙️ MUXING MP4");
            stateBadge.setTextColor(Color.parseColor("#7C3AED"));
            stateBadge.setBackground(createPillDrawable(Color.parseColor("#F5F3FF"), dp(4)));
        } else if (job.state == DownloadJob.State.DOWNLOADING) {
            stateBadge.setText("⬇️ " + job.getProgressPercent() + "%");
            stateBadge.setTextColor(Color.parseColor("#2563EB"));
            stateBadge.setBackground(createPillDrawable(Color.parseColor("#DBEAFE"), dp(4)));
        } else if (job.state == DownloadJob.State.PAUSED) {
            stateBadge.setText("⏸ PAUSED");
            stateBadge.setTextColor(Color.parseColor("#B45309"));
            stateBadge.setBackground(createPillDrawable(Color.parseColor("#FEF3C7"), dp(4)));
        } else if (job.state == DownloadJob.State.ERROR) {
            stateBadge.setText("❌ ERROR");
            stateBadge.setTextColor(Color.parseColor("#B91C1C"));
            stateBadge.setBackground(createPillDrawable(Color.parseColor("#FEE2E2"), dp(4)));
        } else {
            stateBadge.setText("⏳ " + job.state.name());
            stateBadge.setTextColor(Color.parseColor("#64748B"));
            stateBadge.setBackground(createPillDrawable(Color.parseColor("#F1F5F9"), dp(4)));
        }
        topRow.addView(stateBadge);
        row.addView(topRow);

        if (job.state == DownloadJob.State.DOWNLOADING || job.state == DownloadJob.State.MUXING) {
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(job.getProgressPercent());
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
            pbLp.setMargins(0, dp(6), 0, 0);
            pb.setLayoutParams(pbLp);
            row.addView(pb);

            TextView speed = new TextView(this);
            speed.setText(job.getFormattedProgressBytes() + (job.getFormattedSpeed().isEmpty() ? "" : (" • " + job.getFormattedSpeed())));
            speed.setTextSize(9);
            speed.setTextColor(Color.parseColor("#64748B"));
            speed.setPadding(0, dp(2), 0, 0);
            row.addView(speed);
        }

        if (job.state == DownloadJob.State.ERROR && job.errorMessage != null) {
            TextView err = new TextView(this);
            err.setText(job.errorMessage);
            err.setTextSize(9);
            err.setTextColor(Color.parseColor("#DC2626"));
            err.setPadding(0, dp(2), 0, 0);
            row.addView(err);
        }

        // Action Buttons Row
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.RIGHT);
        actionsRow.setPadding(0, dp(6), 0, 0);

        if (job.state == DownloadJob.State.DOWNLOADING) {
            PremiumButton pauseBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            pauseBtn.setText("⏸ Pause");
            pauseBtn.setTextSize(10);
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            pauseBtn.setLayoutParams(pbLp);
            pauseBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).pauseJob(job.id));
            actionsRow.addView(pauseBtn);

            PremiumButton cancelBtn = new PremiumButton(this, PremiumButton.Style.DESTRUCTIVE, isDarkTheme);
            cancelBtn.setText("✖ Cancel");
            cancelBtn.setTextSize(10);
            LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            cbLp.setMargins(dp(6), 0, 0, 0);
            cancelBtn.setLayoutParams(cbLp);
            cancelBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).cancelJob(job.id));
            actionsRow.addView(cancelBtn);
        } else if (job.state == DownloadJob.State.PAUSED) {
            PremiumButton resumeBtn = new PremiumButton(this, PremiumButton.Style.PRIMARY, isDarkTheme);
            resumeBtn.setText("▶ Resume");
            resumeBtn.setTextSize(10);
            LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            resumeBtn.setLayoutParams(rbLp);
            resumeBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).resumeJob(job.id));
            actionsRow.addView(resumeBtn);

            PremiumButton dismissBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            dismissBtn.setText("✕ Dismiss");
            dismissBtn.setTextSize(10);
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            dbLp.setMargins(dp(6), 0, 0, 0);
            dismissBtn.setLayoutParams(dbLp);
            dismissBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).dismissJob(job.id));
            actionsRow.addView(dismissBtn);
        } else if (job.state == DownloadJob.State.COMPLETED) {
            PremiumButton openBtn = new PremiumButton(this, PremiumButton.Style.SUCCESS, isDarkTheme);
            openBtn.setText("📂 Open Media");
            openBtn.setTextSize(10);
            LinearLayout.LayoutParams obLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            openBtn.setLayoutParams(obLp);
            openBtn.setOnClickListener(v -> openDownloadedMedia(job));
            actionsRow.addView(openBtn);

            PremiumButton dismissBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            dismissBtn.setText("✕ Dismiss");
            dismissBtn.setTextSize(10);
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            dbLp.setMargins(dp(6), 0, 0, 0);
            dismissBtn.setLayoutParams(dbLp);
            dismissBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).dismissJob(job.id));
            actionsRow.addView(dismissBtn);
        } else {
            PremiumButton dismissBtn = new PremiumButton(this, PremiumButton.Style.SECONDARY, isDarkTheme);
            dismissBtn.setText("✕ Dismiss");
            dismissBtn.setTextSize(10);
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            dismissBtn.setLayoutParams(dbLp);
            dismissBtn.setOnClickListener(v -> MediaDownloadManager.getInstance(this).dismissJob(job.id));
            actionsRow.addView(dismissBtn);
        }

        row.addView(actionsRow);
        return row;
    }

    private void openDownloadedMedia(DownloadJob job) {
        if (job.savedMediaUri != null) {
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                String mime = job.format.isAudioOnly ? "audio/*" : "video/*";
                if ("jpg".equalsIgnoreCase(job.format.extension) || "png".equalsIgnoreCase(job.format.extension)) {
                    mime = "image/*";
                }
                viewIntent.setDataAndType(job.savedMediaUri, mime);
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(viewIntent);
                return;
            } catch (Exception ignored) {}
        }
        Toast.makeText(this, "Media saved to System Gallery (" + job.destinationSubdir + ")", Toast.LENGTH_SHORT).show();
    }

    private void handleIncomingShareIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                String extractedUrl = MediaResolverRegistry.extractUrl(sharedText);
                if (extractedUrl != null && !extractedUrl.isEmpty()) {
                    openFeature(FeatureRegistry.ID_MEDIA);
                    if (downloaderUrlInput != null) {
                        downloaderUrlInput.setText(extractedUrl);
                    }
                    triggerAnalyzeUrl(extractedUrl);
                }
            }
        }
    }

