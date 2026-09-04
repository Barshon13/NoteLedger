# Notes & Expenses

A privacy-focused offline notes and daily expense tracker built for Android. This application uses a local Room database to ensure all your data stays on your device, with support for JSON backup and restore.

## Features
- **Offline First**: All data is stored locally on the device using Room Database.
- **Expense Tracking**: Easily track daily expenses, categorize spending, and view summaries.
- **Note Taking**: Create, edit, and manage personal notes seamlessly.
- **Data Portability**: Export and import your data via JSON backups.
- **Modern UI**: Built entirely with Jetpack Compose and Material Design 3.

## Tech Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Database**: Room (SQLite)
- **Coroutines & Flow**: For asynchronous programming and reactive UI.
- **Build System**: Gradle (Kotlin DSL)

## Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- JDK 17+

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/notes-and-expenses.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on an emulator or physical device.

## CI/CD
This project is configured with GitHub Actions for continuous integration. The workflow automatically runs on every push and pull request to the `main` branch, performing the following checks:
- Code Linting
- Unit Testing (Robolectric)
- Building Debug APK

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
