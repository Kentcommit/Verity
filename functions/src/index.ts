import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

/**
 * Triggered when a new voice recording or analysis JSON is uploaded.
 * Restricted to the 'Verity/data/encrypted/' path.
 */
export const processVoiceUpload = functions.storage.object().onFinalize(async (object) => {
  const filePath = object.name; // e.g., Verity/data/encrypted/voice_enrollment_20240912.mp3.enc

  if (!filePath || !filePath.startsWith("Verity/data/encrypted/")) {
    return null;
  }

  const contentType = object.contentType; // application/octet-stream
  const fileName = filePath.split("/").pop();

  console.log(`New voice data detected: ${fileName}`);

  // TODO: Implement your AI training trigger here.
  // Example: Send the file metadata to your training server.
  // const trainingData = {
  //   userId: object.metadata?.userId,
  //   filePath: filePath,
  //   timestamp: object.timeCreated,
  // };

  // Note: Since files are AES-256-GCM encrypted locally,
  // the server can only store them as blobs unless it possesses the decryption key.

  return null;
});

/**
 * Utility to verify if a user has admin privileges (for use in HTTPS functions if needed).
 */
export const checkAdminAccess = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  // Check custom claims
  if (context.auth.token.admin !== true) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only project admins can access this data."
    );
  }

  return { status: "success", message: "User is an authorized admin." };
});
