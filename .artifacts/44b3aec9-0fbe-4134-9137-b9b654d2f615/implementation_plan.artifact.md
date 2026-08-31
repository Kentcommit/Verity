# Fix Audio Import Feature

The "Import Audio" feature in the Audio Screening screen currently only calls `refreshFiles()`, which re-queries the MediaStore. If a user picks a file from a location not indexed by MediaStore (or if indexing is slow), the file never appears or gets analyzed. This plan implements a proper import flow that resolves the selected URI, creates a temporary `AudioFile` object, and triggers an immediate scan.

## User Review Required

> [!IMPORTANT]
> The imported file will be analyzed immediately after selection. For now, it will be added to the scan history but might not persist in the "Recordings" list if it's not indexed by MediaStore.

## Proposed Changes

### [Component Name] UI and ViewModel Logic

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/mohan/AndroidStudioProjects/Verity V4/app/src/main/java/com/example/scamguardv1/ui/MainViewModel.kt)
- Add `importAudioFile(uri: Uri)` method to resolve metadata and trigger scan.
- Add logic to handle files not found in MediaStore by adding them to a local session list if needed.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/mohan/AndroidStudioProjects/Verity V4/app/src/main/java/com/example/scamguardv1/ui/screens/MainScreen.kt)
- Update `audioPickerLauncher` to call `viewModel.importAudioFile(uri)` instead of just `refreshFiles()`.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device is more appropriate for URI pickers).

### Manual Verification
1. Open the app and go to **Audio Screening**.
2. Tap the **Import** (Add) icon.
3. Select an audio file from the device's Downloads or Internal Storage.
4. Verify that the app immediately shows the "Scanning" UI.
5. Verify that the scan results appear in the history.
