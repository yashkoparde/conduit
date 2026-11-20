# Conduit: Next-Gen MSME Credit & Capital Engine

Conduit bridges credit assessment, opportunity matching, demand verification, and loan design for MSMEs with AI-powered mobile and web tooling.

## Architecture

- **`conduit-mobile/`**: Native Android Jetpack Compose mobile application with Bhashini Vernacular AI (Groq & Gemini integration), Voice-first onboarding, digital twin analytics, and instant cash flow repayment simulation.
- **`backend/`**: PHP & SQLite engine for credit scoring, opportunity verification, unmet graph analytics, and gap evaluation.
- **`apps/`**: Micro-frontends including Demand Detector, Capital Simulator, Loan Designer, Demand Verification, Unmet Graph, and MSME web console.
- **`sequence/`**: Frame sequences, interactive slides, and media showcases.

## Mobile Setup (`conduit-mobile`)

1. Copy `.env.example` to `.env` and fill in the required keys:
   ```bash
   cp conduit-mobile/.env.example conduit-mobile/.env
   ```
2. Open `conduit-mobile` in Android Studio or build with gradle:
   ```bash
   cd conduit-mobile
   ./gradlew build
   ```
