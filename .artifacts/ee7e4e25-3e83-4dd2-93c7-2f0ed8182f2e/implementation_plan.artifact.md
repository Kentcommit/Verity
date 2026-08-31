# UI Update: New Dashboard Design

Redesign the main screen to match the provided UI layout, featuring a prominent risk score card, a tools grid, and a unified recent activity list.

## Proposed Changes

### [Component] UI State & ViewModel

#### [MODIFY] [MainUiState.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2.5/app/src/main/java/com/example/scamguardv1/ui/state/MainUiState.kt)
- Add `latestRiskScore: Int` to `MainUiState.Success`.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2.5/app/src/main/java/com/example/scamguardv1/ui/MainViewModel.kt)
- Update `latestRiskScore` whenever a new call is screened or an audio file is scanned.
- Logic: `latestRiskScore = latestResult.score.toInt()`.

---

### [Component] Dashboard Redesign

#### [MODIFY] [MainScreen.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2.5/app/src/main/java/com/example/scamguardv1/ui/screens/MainScreen.kt)
- Replace current `Scaffold` content with the new layout.
- Implement **Top Header**: App name and settings icon.
- Implement **Risk Score Card**: Display `latestRiskScore` with a linear progress bar and "Low risk" to "High risk" labels.
- Implement **Protection Tools Grid**: A 2x2 grid of buttons (Audio Scam, Payment ID, Call Screening, QR Check).
- Implement **Recent Protection List**: Unified list showing:
    - Recent screened calls.
    - Recent scanned recordings.
    - A toggle for "Protection is active" at the bottom of the list.

#### [NEW] [DashboardComposables.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2.5/app/src/main/java/com/example/scamguardv1/ui/composables/DashboardComposables.kt)
- `RiskScoreSection`: Composable for the top risk card.
- `ToolGridItem`: Individual item for the 2x2 grid.
- `RecentProtectionItem`: Generic item for the activity list.

---

## Verification Plan

### Automated Tests
- Run `gradle_build` to ensure no UI breakage.

### Manual Verification
- Deploy to device/emulator.
- Verify the risk score updates when a scan completes.
- Verify the tool grid icons and labels match the screenshot.
- Verify the "Recent protection" list displays items correctly.
- Verify the toggle at the bottom controls call screening.
