# antGnss: Advanced GNSS Satellite Monitoring for Android

[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-green.svg?style=flat-square)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-CC-NC-yellow.svg?style=flat-square)](https://opensource.org/licenses/MIT)

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="antGnss App Logo" width="150"/>
</p>

**antGnss** is a powerful and intuitive Android application designed for real-time monitoring and analysis of Global Navigation Satellite System (GNSS) constellations. Built entirely with modern Android technologies like Kotlin and Jetpack Compose, this app provides a detailed dashboard for developers, researchers, and enthusiasts to visualize and inspect satellite data directly from their device's receiver.

The application leverages Android's raw GNSS measurement APIs to provide a rich, interactive user experience, including a dynamic sky map of satellite positions, detailed signal quality metrics, and advanced data fetched from external APIs.

---

## 📖 Table of Contents

-   [✨ Features](#-features)
-   [📡 Core Functionality](#-core-functionality)
-   [🛠️ Tech Stack & Architecture](#-tech-stack--architecture)
-   [🚀 Getting Started](#-getting-started)
    -   [Prerequisites](#prerequisites)
    -   [Installation & Setup](#installation--setup)
-   [🤝 Contributing](#-contributing)
-   [📜 License](#-license)

---

## ✨ Features

* **Real-Time Satellite Tracking:** Directly utilizes the device's `GnssStatus` and `GnssMeasurements` callbacks to display up-to-the-second data for all visible satellites.
* **Multi-Constellation Support:** Tracks and identifies satellites from major global constellations, including **GPS**, **GLONASS**, **BeiDou**, and **Galileo**.
* **Interactive Sky Map:** Visualizes the position of satellites in the sky relative to the user.
    * **Full View:** A comprehensive sky map showing all tracked satellites.
    * **Split View:** A unique, quad-view display that separates satellites by constellation for clearer analysis.
    * **Dynamic Highlighting:** Tap a satellite on the map to highlight it and automatically scroll to its detailed information card.
* **Detailed Data Display:** Each satellite is presented with a comprehensive set of metrics:
    * **SVID** (Satellite ID) & Constellation Name
    * **CN0** (Carrier-to-Noise Density Ratio) for signal quality, color-coded for clarity.
    * **Elevation** & **Azimuth** angles.
    * **"Used in fix?"** status.
    * **Doppler Shift** and **Pseudorange Uncertainty**.
* **External API Integration:** Enriches the raw data by fetching additional satellite information (like official name and eclipsed status) from the **N2YO API**.
* **Distance Calculation:** Implements an `ECEFEngine` to calculate the precise 3D distance between the user and each satellite using the Earth-Centered, Earth-Fixed coordinate system.
* **Modern UI/UX:** A sleek, dark-themed interface built entirely with **Jetpack Compose**, featuring smooth animations and a clean, readable layout.
* **Permissions Handling:** A user-friendly, robust permission handler for `ACCESS_FINE_LOCATION` ensures a smooth user experience.

---

## 📡 Core Functionality

The application is centered around three main engines:

1.  **`GNSSEngine`**: This is the heart of the app. It interfaces directly with the Android `LocationManager` to register `GnssStatus.Callback` and `GnssMeasurementsEvent.Callback`. It collects, processes, and manages the state of all tracked satellites in a `MutableStateFlow`, making the data readily available to the UI in a reactive manner.

2.  **`ApiService` (with Ktor)**: To provide more than just raw data, this service connects to the N2YO API. It takes a satellite's SVID and constellation, maps it to the correct **NORAD ID** using internal maps (`gpsSvidToNorad`, etc.), and fetches positional data. This allows for more accurate distance calculations and displays the satellite's common name.

3.  **`ECEFEngine`**: This module performs the mathematical heavy lifting. It converts the user's and the satellite's geodetic coordinates (latitude, longitude, altitude) into the **Earth-Centered, Earth-Fixed (ECEF)** coordinate system. It then calculates the straight-line Euclidean distance, giving a more accurate user-to-satellite range.

---

## 🛠️ Tech Stack & Architecture

This project is built using a modern, single-activity architecture and leverages the latest in Android development.

* **Core Language:** [**Kotlin**](https://kotlinlang.org/)
* **UI Framework:** [**Jetpack Compose**](https://developer.android.com/jetpack/compose) for declarative UI development.
* **State Management:** Kotlin **Flows** and `collectAsState` to reactively update the UI.
* **Networking:** [**Ktor Client**](https://ktor.io/docs/client-overview.html) for efficient and modern asynchronous HTTP requests.
* **Asynchronous Programming:** [**Kotlin Coroutines**](https://kotlinlang.org/docs/coroutines-overview.html) are used extensively for background tasks like API calls and location updates.
* **Location Services:** Android's native `LocationManager` and the `GnssStatus` API.
* **Navigation:** [**Jetpack Navigation for Compose**](https://developer.android.com/jetpack/compose/navigation) to manage screen transitions.
* **Serialization:** `kotlinx.serialization` for parsing JSON responses from the N2YO API.
* **Build System:** [**Gradle**](https://gradle.org/) with Kotlin DSL (`build.gradle.kts`).

---

## 🚀 Getting Started

Follow these steps to get the project running on your local machine.

### Prerequisites

* **Android Studio:** The latest version is recommended.
* **Android Device:** A physical device with Android API level 33+ is required to access the GNSS receiver. The app will not function correctly on an emulator without GPS data.
* **N2YO API Key:** The app requires a free API key from [N2YO.com](https://www.n2yo.com/api/) to fetch detailed satellite data.

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/booukes/antGnss.git](https://github.com/booukes/antGnss.git)
    cd antGnss
    ```

2.  **Create `local.properties` file:**
    In the root directory of the project, create a file named `local.properties`.

3.  **Add your API Key:**
    Inside `local.properties`, add your N2YO API key.
    ```properties
    APIKEY="YOUR_N2YO_API_KEY_HERE"
    ```
    The project is configured to read this key from `local.properties` and make it available in the build configuration, so your key remains private.

4.  **Open in Android Studio:**
    Open the project in Android Studio. It will automatically sync the Gradle files and download the necessary dependencies.

5.  **Run the application:**
    Connect your Android device, ensure USB debugging is enabled, and run the 'app' configuration. The app will ask for location permissions upon first launch, which are required for it to function.

---

## 🤝 Contributing

Contributions are welcome! If you'd like to improve antGnss, please follow these steps:

1.  **Fork the Project**
2.  **Create your Feature Branch** (`git checkout -b feature/NewFeature`)
3.  **Commit your Changes** (`git commit -m 'Add some NewFeature'`)
4.  **Push to the Branch** (`git push origin feature/NewFeature`)
5.  **Open a Pull Request**

Please feel free to open an issue to discuss any bugs or feature ideas.

---

## 📜 License

This project is distributed under the MIT License. See the `LICENSE.txt` file for more information.
