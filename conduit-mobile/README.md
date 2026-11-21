# Conduit Mobile: Vernacular Voice-First MSME Banking

> Native Android application crafted with Jetpack Compose, Material 3, and integrated with Bhashini / Groq LLMs to deliver real-time voice credit applications, financial digital twins, and flexible daily repayment schedules for Indian micro-merchants.

---

## Architectural Highlights

- **Voice-First Interaction**: Allows non-English speaking merchants to speak in Kannada, Hindi, or English to apply for working capital.
- **Real-Time Vernacular Translation**: Fast audio inference powered by Groq and Gemini models to extract loan requirements, sales velocity, and inventory needs.
- **Financial Digital Twin**: Visualizes the merchant's live operational health, creditworthiness, and revenue trends on-device.
- **Dynamic Cash Flow Repayments**: Micro-installments tailored to daily UPI receipts rather than rigid monthly EMIs.

---

## Component Architecture

```mermaid
flowchart TD
    subgraph UI_Layer["UI Layer (Jetpack Compose & Material 3)"]
        MA["MainActivity (NavHost)"]
        HS["HomeScreen (Credit Overview & Alerts)"]
        VO["VoiceOnboardingScreen (Bilingual Voice Guide)"]
        VA["VoiceApplyScreen (Waveform Visualizer & Speech Input)"]
        DT["DigitalTwinScreen (Merchant Financial Health)"]
        RS["RepaymentScreen (Dynamic Micro-Split Tracking)"]
        MA --> HS & VO & VA & DT & RS
    end

    subgraph State_Layer["ViewModel & State Layer"]
        HVM["HomeViewModel"]
        VAVM["VoiceApplyViewModel"]
        RVM["RepaymentViewModel"]
        HS --> HVM
        VA --> VAVM
        RS --> RVM
    end

    subgraph Data_Layer["Data & Repository Layer"]
        CR["ConduitRepository"]
        PDS["ConduitPreferencesDataStore (Encrypted Cache)"]
        GTS["GroqTranslationService (Bhashini Engine)"]
        API["ConduitApiService (Ktor Client)"]
        
        HVM & VAVM & RVM --> CR
        CR --> PDS
        CR --> GTS
        CR --> API
    end

    subgraph Remote_Services["Remote Cloud Services"]
        GroqCloud["Groq Cloud Vernacular Inference"]
        ConduitBackend["Conduit Core Engine Gateway"]
        GTS --> GroqCloud
        API --> ConduitBackend
    end
```

---

## Voice Application Execution Flow

```mermaid
sequenceDiagram
    autonumber
    actor Merchant as Merchant
    participant Mic as Android Audio Record
    participant Screen as VoiceApplyScreen
    participant VM as VoiceApplyViewModel
    participant Groq as GroqTranslationService
    participant Backend as Conduit API

    Merchant->>Screen: Tap microphone button & speak in Kannada
    Screen->>Mic: Capture audio buffer & render live waveform
    Merchant->>Screen: Tap finish recording
    Screen->>VM: Pass audio data
    VM->>Groq: Request intent parsing (language: kn-IN)
    Groq-->>VM: Extracted JSON: { loanType: "Inventory", amount: 150000, tenureMonths: 6 }
    VM->>Screen: Display confirmation dialog in Kannada & English
    Merchant->>Screen: Confirms loan request
    Screen->>Backend: Submit application payload
    Backend-->>Screen: Return instant sanction offer & digital twin update
```

---

## Folder Organization

```
conduit-mobile/
├── app/
│   ├── build.gradle.kts           # Compose dependencies, Ktor, and compiler flags
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions (Microphone, Internet, Audio)
│       │   ├── java/com/conduit/msme/
│       │   │   ├── ConduitApp.kt  # Application entrypoint
│       │   │   ├── data/          # ApiClient, GroqTranslationService, Models, DataStore
│       │   │   └── ui/            # Compose screens, ViewModels, Themes, and Components
│       │   └── res/
│       │       ├── values/        # Default English strings, colors, themes
│       │       ├── values-kn/     # Kannada vernacular translation strings
│       │       ├── font/          # Noto Sans Kannada & Inter fonts
│       │       └── drawable/      # Adaptive icons, logos, and merchant profile assets
│       └── test/                  # Unit and Robolectric tests
├── gradle/
│   └── libs.versions.toml         # Gradle Version Catalog
└── build.gradle.kts               # Root Android build configuration
```

---

## Setup & Running Locally

### Prerequisites
- **Android Studio Ladybug (or newer)**
- **JDK 17 or higher**
- Android SDK with API 34+

### Step-by-Step Instructions

1. **Clone and open the directory**:
   Open Android Studio, choose **Open**, and navigate to `conduit-mobile`.

2. **Configure Environment Variables**:
   Copy the example environment template:
   ```bash
   cp .env.example .env
   ```
   Add your keys in `.env`:
   ```properties
   GROQ_API_KEY=your_groq_api_key_here
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Build the Project**:
   ```powershell
   ./gradlew assembleDebug
   ```

4. **Run on Device or Emulator**:
   Select your connected device in Android Studio and press **Run (Shift + F10)**.
