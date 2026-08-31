# Call Screening Feature Implementation

This plan adds a Call Screening feature to ScamGuard, allowing the app to intercept incoming calls and analyze them using the existing voice manipulation scoring system.

## User Review Required

> [!IMPORTANT]
> To function correctly, the app will need to be granted the **Call Screening Role** by the user. This implementation includes the logic to request this role.

> [!WARNING]
> Real-time audio access during a call is highly restricted by Android for privacy reasons. This feature will implement the `CallScreeningService` infrastructure and provide a simulated voice analysis path that integrates with your existing `ScamDetector` and `RiskAnalysis` logic.

## Proposed Changes

### Core Service

#### [NEW] [ScamCallScreeningService.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2/app/src/main/java/com/example/scamguardv1/service/ScamCallScreeningService.kt)
Implement `CallScreeningService` to intercept calls. It will use the `ScamDetector` to "analyze" incoming call metadata and potentially trigger a live analysis UI.

### Data Layer

#### [MODIFY] [Constants.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2/app/src/main/java/com/example/scamguardv1/utils/Constants.kt)
Add constants for Call Screening role request and new scoring weights related to call metadata (e.g., number verification status).

### UI Layer

#### [MODIFY] [MainUiState.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2/app/src/main/java/com/example/scamguardv1/ui/state/MainUiState.kt)
Add state to track if Call Screening is active and the results of recent screened calls.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2/app/src/main/java/com/example/scamguardv1/ui/MainViewModel.kt)
Add logic to:
- Check and request the Call Screening role.
- Handle updates from the `ScamCallScreeningService`.
- Toggle call screening on/off.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2/app/src/main/java/com/example/scamguardv1/ui/screens/MainScreen.kt)
Add a new "Call Screening" section in the UI with a toggle and a list of recently screened calls.

### Configuration

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/mohan/AndroidStudioProjects/ScamGuard%20v2/app/src/main/AndroidManifest.xml)
- Register `ScamCallScreeningService`.
- Add permissions: `ANSWER_PHONE_CALLS`, `READ_PHONE_STATE`.

## Verification Plan

### Automated Tests
- Create `ScamCallScreeningServiceTest` to verify call interception logic with mock `Call.Details`.

### Manual Verification
- Deploy the app.
- Toggle Call Screening in the UI.
- Verify the system prompts to set ScamGuard as the call screening app.
- Simulate an incoming call (e.g., using `adb shell am start -a android.intent.action.INSERT -t vnd.android.cursor.dir/contact -e phone 123456789`) and verify the service triggers.
