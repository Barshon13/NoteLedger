# Coin Store Implementation Plan

## Overview
Implement a "Coin Store" feature for the NoteLedger app allowing users to purchase coins using Google Play Billing.

## Architecture
- **Navigation**: New bottom navigation destination.
- **Billing**: `BillingManager` utilizing `BillingClient`.
- **Database**: Room `CoinBalanceEntity` and `CoinDao`.
- **ViewModel**: `CoinStoreViewModel` to manage coin balance and billing interactions.
- **UI**: `CoinStoreScreen` with a dark emerald theme and 2-column grid.

## Tasks
1. **Gradle**: Add/uncomment `billing-ktx` dependency.
2. **Manifest**: Add `BILLING` permission.
3. **Database**: Implement `CoinBalanceEntity`, `CoinDao`, and update `AppDatabase`.
4. **Billing**: Implement `BillingManager`.
5. **ViewModel**: Implement `CoinStoreViewModel`.
6. **UI**: Implement `CoinStoreScreen` and supporting UI components.
7. **Navigation**: Update bottom navigation and navigation graph.
