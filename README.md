# PageTurner – Online Bookstore (Android)

PageTurner is a native Android online-bookstore app built with Kotlin and Jetpack Compose. It runs fully offline: the catalogue, accounts, baskets and orders live in a local Room database, and payments are simulated.

## Features

- **Authentication** – register and sign in with email and password. Passwords are salted and hashed, and the session is persisted.
- **Home** – personalised recommendations, a "Buy Again" shelf and recently viewed books.
- **Catalogue** – browse by category, search, and open product detail pages with related books.
- **Basket** – add, remove and change quantities of books.
- **Checkout** – pick or add/edit a delivery address, then pay by Card, UPI or Net Banking.
- **Orders** – confirmation screen, order history and order details. Orders can be cancelled within 48 hours of placement.
- **Delivery estimates** – tentative delivery dates count business days only (Mon–Fri).
- **Recommendations** – scored from purchases (strongest signal), browsing history and basket contents. With no history, the most popular books are shown.

## Tech stack

| Area | Library |
|------|---------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00), Material 3 |
| Navigation | Navigation Compose 2.8.3 |
| Architecture | MVVM, repositories, manual DI (`AppContainer`) |
| Persistence | Room 2.6.1 (KSP) |
| Async | Kotlin Coroutines / Flow |
| Tests | JUnit 4, kotlinx-coroutines-test |

- Android Gradle Plugin 8.13.2
- `minSdk` 26, `targetSdk`/`compileSdk` 35
- Java 17

## Project structure

```
app/src/main/java/com/bookstore/app/
├── MainActivity.kt, BookstoreApp.kt, AppContainer.kt
├── core/util/        Validators, PasswordHasher, DeliveryEstimator, Formatters, AppResult
├── data/
│   ├── auth/         AuthRepository, SessionManager
│   ├── local/        Room entities, DAOs, BookstoreDatabase
│   ├── payment/      PaymentGateway (simulated)
│   ├── repo/         Catalog, Cart, Address, Order, Recommendation repositories
│   └── seed/         SeedData, DatabaseSeeder
├── domain/           CancellationPolicy, RecommendationEngine, RelatedBooks
└── ui/
    ├── auth/  home/  catalogue/  basket/  checkout/  orders/
    ├── navigation/   Routes, AppNavigation, MainViewModel
    ├── components/  common/  theme/
```

Unit tests are in `app/src/test/java/com/bookstore/app/` and cover view models, repositories, the payment gateway, domain logic and utilities.

## Getting started

### Prerequisites

- Android Studio (a recent version that supports AGP 8.13)
- JDK 17
- Android SDK 35

### Run

1. Open the project in Android Studio and let Gradle sync.
2. Select an emulator or a device running Android 8.0 (API 26) or higher.
3. Click **Run**, or from the command line:

```bash
./gradlew installDebug
```

### Test

```bash
./gradlew testDebugUnitTest
```

### Build a release APK

```bash
./gradlew assembleRelease
```

The release build has minification and resource shrinking enabled (see `app/proguard-rules.pro`).

## Demo account

On first launch the database is seeded with the catalogue and a demo customer who already has two orders. One is inside the 48-hour cancellation window and one is outside it.

| Field | Value |
|-------|-------|
| Email | `demo@pageturner.com` |
| Password | `Books1234` |

## Simulated payments

No real payment provider is used and no money moves. Outcomes are deterministic so you can test both success and failure paths:

| Method | Behaviour |
|--------|-----------|
| Card | `4000 0000 0000 0002` is declined. Any other card that passes the Luhn check, with a future expiry and a 3–4 digit CVV, is approved. |
| UPI | IDs starting with `fail` (e.g. `fail@upi`) are declined. Other `name@bank` IDs are approved. |
| Net Banking | "Demo Bank (Offline)" is unavailable. Other banks are approved. |

Only a non-sensitive summary (e.g. "Card ending 4242") is stored on the order.

## Validation rules

- **Password:** at least 8 characters, with both letters and numbers.
- **Phone:** 10–12 digits.
- **PIN code:** 6 digits.
- **Card expiry:** `MM/YY`, not in the past.

## Data storage

Data is stored locally in a Room database named `bookstore.db`. Room schemas are exported to `app/schemas`. Clear the app's data or reinstall it to reset the database.
