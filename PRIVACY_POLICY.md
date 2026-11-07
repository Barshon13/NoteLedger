# Privacy Policy for NoteLedger

**Last Updated:** November 7, 2026

Welcome to **NoteLedger** ("the Application", "we", "us", or "our"), developed and published by **Barshon Hossain** (package name: `com.barshon.noteledger`). 

Your privacy is of paramount importance to us. This Privacy Policy explains how NoteLedger handles your information, our strict commitment to an offline-first architecture, and how third-party services integrated into the app operate in accordance with Google Play Developer Policies.

By downloading, installing, or using NoteLedger, you acknowledge and agree to the practices described in this Privacy Policy.

---

### 1. Offline Data Storage & Zero Personal Data Collection

NoteLedger is engineered with a strict **privacy-by-design and offline-first principle**:
- **On-Device Storage:** All user-generated content—including your personal notes, journals, expense entries, transaction categories, budget targets, debt records, and note quotas—is stored **locally on your device** inside a private SQLite database managed by Android Room.
- **No Developer Servers:** We do **not** operate external database servers, central accounts, or analytics tracking pipelines that collect, harvest, monetize, or transmit the contents of your notes or financial entries.
- **No Personal Identifiers:** We do not request, harvest, or store personal identifiers such as your full name, physical address, phone number, contacts list, or photographs.

---

### 2. Advertisements (Google AdMob)

NoteLedger integrates the official **Google Mobile Ads SDK (AdMob)** to serve advertisements (such as banner ads, interstitial ads, and opt-in rewarded video ads) that support ongoing development.

- **Data Processed by Ad Networks:** Google AdMob and its advertising partners may collect and process pseudonymous device telemetry to serve advertisements. This data may include your device's Android Advertising ID (AAID), IP address, general coarse location (country/city level), mobile carrier, device make and model, and interaction metrics with displayed ads.
- **Personalized vs. Non-Personalized Ads:** Depending on your geographical region (such as the EEA/UK under GDPR regulations) and your consent preferences, advertisements may be personalized or non-personalized. Non-personalized ads still use basic device information to limit frequency and detect fraud.
- **Third-Party Policy:** The collection and processing of advertising data by Google are governed exclusively by Google's Privacy Policy. We strongly encourage you to review:
  - **Google Privacy & Terms:** [https://policies.google.com/privacy](https://policies.google.com/privacy)
  - **How Google uses information from sites or apps that use our services:** [https://policies.google.com/technologies/partner-sites](https://policies.google.com/technologies/partner-sites)
- **Opting Out of Personalized Advertising:** You can reset or opt out of personalized ad tracking directly within your Android device settings by navigating to: **Settings > Google > Ads > Delete advertising ID** or **Reset advertising ID**.

---

### 3. In-App Purchases (Google Play Billing)

NoteLedger offers optional digital in-app products (such as Coin packs to unlock additional note creation quotas):
- **Processed Exclusively by Google:** All financial transactions, card processing, billing authorization, and currency conversions are handled securely and directly by the **Google Play Billing Library**.
- **No Financial Data Access:** NoteLedger and its developer **never** see, receive, or store your credit card numbers, debit card details, bank account credentials, or billing addresses.
- **Order Verification:** We only receive an anonymous purchase token and order identifier from Google Play to confirm successful payment and credit the appropriate coin balance to your on-device ledger.

---

### 4. Optional Cloud Backup (Google Drive AppData)

NoteLedger provides an optional **Cloud Backup & Restore** feature to safeguard your records:
- **Private User Storage:** If you choose to connect your Google Account for cloud backups, an encrypted snapshot of your local database is uploaded strictly to your **own private Google Drive AppData folder** (`drive.appdata` scope).
- **Zero Developer Access:** This backup is stored exclusively in your private cloud account. The developer has zero access to your Google Drive files, cannot read your backups, and cannot alter your cloud data.
- **User Control:** Cloud sync only initiates when you explicitly tap "Backup to Google Drive" or "Restore from Google Drive" in the application settings.

---

### 5. Data Retention & User Data Deletion

Because NoteLedger stores your information locally on your device, you have complete and total ownership over your data at all times:
- **In-App Data Erasure:** You can permanently wipe all local notes, expenses, budgets, and balance records at any time by going to **Settings > Clear All Data** within the application.
- **Device-Level Deletion:** You can permanently delete all locally stored records by clearing the app's storage via **Android Settings > Apps > NoteLedger > Storage & cache > Clear storage**, or by simply uninstalling the application from your device.
- **Deleting Cloud Backups:** If you utilized Google Drive backup, you can delete your AppData files anytime by visiting [Google Drive on the web](https://drive.google.com), navigating to **Settings (gear icon) > Manage Apps > NoteLedger > Options > Delete hidden app data**.

---

### 6. Children's Privacy

NoteLedger is not directed toward children under the age of 13 (or under 16 in the European Economic Area). We do not knowingly solicit, collect, or process personal data from children. If you believe that a child has provided us with personal information, please contact us immediately so we can assist in removing it.

---

### 7. Android Device Permissions

NoteLedger requests only the minimal system permissions required to deliver core functionality:
- **`android.permission.INTERNET`:** Required to load AdMob advertisements, verify in-app purchases with Google Play, and execute user-requested Google Drive backups.
- **`android.permission.ACCESS_NETWORK_STATE`:** Used to check whether your device has an active internet connection before initiating ads or cloud sync requests.
- **`com.android.vending.BILLING`:** Required to facilitate in-app purchases securely through Google Play.
- **Zero Dangerous Permissions:** The app **does not** request intrusive permissions such as Location, Camera, Microphone, Contacts, Phone State, SMS, or broad storage access (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`).

---

### 8. Changes to This Privacy Policy

We may periodically revise this Privacy Policy to reflect application updates, regulatory requirements, or changes to third-party SDK standards. Any updates will be published with an updated "Last Updated" date at the top of this document and will be made available through the application settings and our public website.

---

### 9. Contact Us

If you have any questions, feedback, or concerns regarding this Privacy Policy or your data privacy while using NoteLedger, please contact:

- **Developer:** Barshon Hossain
- **Application:** NoteLedger (`com.barshon.noteledger`)
- **Contact Email:** mohammadborshonhossain6@gmail.com
- **Official Website:** [https://mobiledoctor12.blogspot.com](https://mobiledoctor12.blogspot.com)
