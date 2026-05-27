# EduTrack Firebase Setup

1. Create a Firebase project.
2. Add an Android app with package name `com.raju.edutrack`.
3. Download `google-services.json` and place it in `app/google-services.json`.
4. Enable Authentication > Google sign-in in Firebase Console.
5. Copy the Web client ID from Firebase/Google Cloud and replace `replace_with_web_client_id` in `app/src/main/res/values/strings.xml`.
6. Enable Firestore Database.
7. Use security rules like:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Cloud backups are stored at:

```text
users/{uid}/backups/latest
```

The app keeps local JSON storage as the offline source and syncs it to Firestore for manual and automatic backups.
