 	

# Liquor Inventory Manager (Web Edition)

### वाइन शॉप दैनिक स्टॉक व बिक्री खाता-बही (Digital Daily Stock Register)

---

## 1. परियोजना परिचय (Project Overview)

यह वेब एप्लिकेशन वाइन शॉप (शराब दुकान) के पारंपरिक हस्तलिखित दैनिक स्टॉक रजिस्टर (Daily Stock Register) को डिजिटल रूप में प्रस्तुत करता है। यह सिस्टम 3 दुकानों, 40 लोकप्रिय ब्रांड्स, और 3 बोतल साइज़ (180 ml, 375 ml, 750 ml) के दैनिक स्टॉक और बिक्री का संपूर्ण हिसाब रखता है।

### मुख्य विशेषताएं (Key Highlights):

1. **रजिस्टर जैसा लेआउट (Exact Register Look & Feel)**:
   - हर ब्रांड एक ही पंक्ति (Row) में दिखता है।
   - तीनों साइज़ (180 ml, 375 ml, 750 ml) एक साथ सामने दिखते हैं।
   - हर साइज़ में 7 कॉलम: **पि.शेष (Pichla Shesh)** | **आमद (Aamad)** | **योग (Yog)** | **बिक्री (Bikri)** | **शेष (Shesh)** | **दर (Dar)** | **कीमत (Keemat)**।
2. **सख्त रंग कोडिंग (Exact Color Coding)**:
   - 🔴 **लाल रंग (Red)**: पिछला शेष (Opening Balance) एवं अंतिम शेष (Closing Stock) — सिस्टम द्वारा स्वचालित।
   - 🔵 **नीला रंग (Blue)**: आमद (Stock In) एवं बिक्री (Sales) — उपयोगकर्ता द्वारा इनपुट।
   - ⚫ **काला/ग्रे रंग (Black/Slate)**: योग, दर, और कीमत (Calculated & Rate values)।
3. **तेज कीबोर्ड नेविगेशन (Fast Keyboard Entry)**:
   - **Tab** या **Enter** दबाने पर कर्सर सीधे अगली इनपुट सेल (आमद $\rightarrow$ बिक्री $\rightarrow$ अगला साइज़ $\rightarrow$ अगला ब्रांड) में जाता है।
   - माउस छूने की कोई आवश्यकता नहीं है।
4. **स्वतः अगला दिन कैरी-फॉरवर्ड (Automatic Carry Forward)**:
   - पिछले दिन का **शेष (Closing Stock)** अगले दिन का **पिछला शेष (Opening Balance)** अपने आप बन जाता है।
5. **सुरक्षित एवं लेन-देन अखंडता (Atomic Transactions)**:
   - यदि किसी भी ब्रांड में बिक्री उपलब्ध स्टॉक से अधिक दर्ज होती है, तो सिस्टम त्रुटि चेतावनी देता है और गलत डेटा सेव नहीं होने देता।
   - दर (Rate) बदलने पर पुराने दिनों के रिकॉर्ड्स की दर व कीमत में कोई बदलाव नहीं होता (Rate Snapshot Integrity)।
6. **बहु-प्रारूप रिपोर्टिंग (Multi-format Reports)**:
   - 📄 **रजिस्टर स्टाइल PDF** (प्रिंटिंग हेतु)
   - 📊 **Excel (.xlsx)** (अकाउंटेंट हेतु)
   - 📁 **CSV**

---

## 2. त्वरित उपयोग मार्गदर्शिका (Client User Guide)

### लॉगिन कैसे करें:

- ब्राउज़र में एप्लिकेशन खोलें।
- **यूज़रनेम (Username)**: `admin`
- **पासवर्ड (Password)**: `admin123` (या जो `.env` में सेट किया गया हो)
- **Sign In** बटन दबाएं।

### दैनिक रजिस्टर भरने की विधि (Daily Entry Workflow):

1. ऊपर ड्रॉपडाउन से अपनी **दुकान** चुनें।
2. **दिनांक (Date)** चुनें (डिफ़ॉल्ट रूप से आज की तारीख चुनी होती है)।
3. टेबल में **पि.शेष** और **दर** अपने आप भरे हुए दिखाई देंगे।
4. केवल नीले रंग के बॉक्स में:
   - **आमद** (आज प्राप्त माल) दर्ज करें।
   - **बिक्री** (आज बिका माल) दर्ज करें।
5. **योग**, **शेष**, और **कीमत** तुरंत स्क्रीन पर अपडेट होते जाएंगे।
6. पूरा रजिस्टर भरने के बाद ऊपर दाईं ओर **हिसाब सेव करें (Save Sheet)** बटन दबाएं।

### मास्टर डेटा प्रबंधन (Master Data):

- **दुकानें (Shops)**: नई दुकान जोड़ें या नाम बदलें।
- **ब्रांड्स (Brands)**: नए ब्रांड जोड़ें और $\uparrow$ / $\downarrow$ तीरों से रजिस्टर में उनका क्रम सेट करें।
- **दरें (Rates)**: किसी भी साइज़ की बोतल की दर अपडेट करें।
- **आरंभिक स्टॉक (Opening Stock)**: दुकान शुरू करते समय पहली तारीख का प्रारंभिक स्टॉक सेट करें।

---

## 3. तकनीकी संरचना (Technology Architecture)

```
P1_Wine_Inventory_Management/
├── server/                      # Node.js + Express + TypeScript + SQLite
│   ├── src/
│   │   ├── db/                 # schema.sql, connection.ts, migrate.ts, seed.ts
│   │   ├── models/             # Typed interfaces
│   │   ├── services/           # All business logic (inventory, carryForward, rate, report, auth)
│   │   ├── routes/             # REST API routes
│   │   └── middleware/         # requireAuth, errorHandler
│   ├── tests/                  # Vitest unit & E2E integration test suite
│   └── data/                   # liquor_inventory.db (SQLite database)
└── client/                     # React 18 + Vite + TypeScript + Tailwind CSS
    ├── src/
    │   ├── api/                # REST API client
    │   ├── context/            # AuthContext (JWT session state)
    │   ├── pages/              # DailyRegister, Dashboard, MasterData, Reports, Login
    │   ├── components/         # RegisterTable, ShopDateSelector, ColorLegend, Navbar, Layout
    │   └── styles/             # devanagari.css (Noto Sans Devanagari font rules)
    └── index.html
```

---

## 4. लोकल पर चलाने के निर्देश (Local Development & Run)

### 1. सभी डिपेंडेंसी इंस्टॉल करें:

```bash
npm run install:all
```

### 2. डेटाबेस इनिशियलाइज़ व सीड करें:

```bash
npm run db:seed --prefix server
```

### 3. ऑटोमेटेड टेस्ट रन करें:

```bash
npm test
```

*(सभी 16 यूनिट और E2E इंटीग्रेशन टेस्ट पास होंगे)*

### 4. एप्लिकेशन स्टार्ट करें:

- **डेवलपमेंट मोड (Dev Mode)**:

  ```bash
  npm run dev:server   # Terminal 1: Starts API on http://localhost:5000
  npm run dev:client   # Terminal 2: Starts Vite on http://localhost:5173
  ```
- **प्रोडक्शन मोड (Production Monolith - Single Port 5000)**:

  ```bash
  npm run build
  npm start
  ```

  ब्राउज़र में `http://localhost:5000` खोलें।

---

## 5. क्लाउड पर फ्री होस्टिंग गाइड (Free Cloud Deployment Guide)

> **ग्राहक के लिए विशेष सूचना**: गूगल ड्राइव (Google Drive) केवल फाइल स्टोरेज है, वह गतिशील वेब सर्वर या डेटाबेस नहीं चला सकता। इसके लिए Render.com या Railway.app सबसे उपयुक्त और निःशुल्क (Free Tier) विकल्प हैं।

### Render.com पर डिप्लॉय करने के सरल चरण:

1. इस प्रोजेक्ट रिपॉजिटरी को अपने **GitHub** खाते में पुश (Push) करें।
2. [Render.com](https://render.com) पर फ्री अकाउंट बनाएं और **New + $\rightarrow$ Web Service** चुनें।
3. अपनी GitHub रिपॉजिटरी को कनेक्ट करें।
4. निम्नलिखित सेटिंग्स भरें:
   - **Environment**: `Node`
   - **Build Command**: `npm run install:all && npm run build`
   - **Start Command**: `node server/dist/index.js`
   - **Plan**: `Free`
5. **Environment Variables** जोड़ें:
   - `NODE_ENV` = `production`
   - `JWT_SECRET` = *(कोई भी सुरक्षित सीक्रेट की)*
   - `SEED_ADMIN_PASSWORD` = *(मालिक का पासवर्ड)*
6. **Persistent Disk** (वैकल्पिक परंतु अनुशंसित):
   - डिस्क माउंट पाथ: `server/data` (ताकि SQLite डेटाबेस सर्वर रीस्टार्ट होने पर भी सुरक्षित रहे)।
7. **Deploy Web Service** पर क्लिक करें। आपका लाइव यूआरएल (उदा. `https://wine-inventory.onrender.com`) तैयार हो जाएगा।

---

## 6. डेटा सुरक्षा व बैकअप (Data Integrity & Backups)

- SQLite डेटाबेस फाइल `server/data/liquor_inventory.db` में सुरक्षित रहती है।
- इस फाइल की एक प्रति (Copy) कभी भी पेनड्राइव या गूगल ड्राइव पर बैकअप के रूप में रखी जा सकती है।
- वित्तीय अखंडता नियम के तहत कोई भी ऐतिहासिक रिकॉर्ड मिटाया नहीं जाता।
