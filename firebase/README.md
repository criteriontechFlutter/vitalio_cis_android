# Feature flags (Remote Config)

The app's main modules can be switched on/off for every user, live.

```
Admin page (login 9807039752)  ──writes──▶  Firestore  cis_app_config/feature_flags
                                                │  onDocumentWritten
                                                ▼
                                   Cloud Function publishFeatureFlags
                                                │  publishes template
                                                ▼
                         Remote Config  cis_feature_*  ──real-time──▶  every app (FeatureFlags.kt)
```

Remote Config can't be written from an app (the client SDK is read-only), which is why the
admin page goes through Firestore and a Cloud Function.

## Keys

| Remote Config key | Module |
|---|---|
| `cis_feature_vitals` | Vitals tile, vitals card, vital history |
| `cis_feature_fluid` | Fluid intake / output |
| `cis_feature_symptoms` | Symptoms tracker and history |
| `cis_feature_medicine` | Medicine reminder, upcoming medications, manage medicines |
| `cis_feature_diet` | Diet checklist, diet box, plan builder, nutrition analytics, daily diet card |
| `cis_feature_interaction_checker` | Interactions checker |
| `cis_feature_appointments` | Find doctor, booking, appointments (home + drawer) |
| `cis_feature_lab_reports` | Reports, upload report, AI report |
| `cis_feature_prescriptions` | Prescriptions, upload/analyse prescription |
| `cis_feature_voice_assistant` | Mic button on the dashboard |
| `cis_feature_activity` | Activity tab |
| `cis_feature_smart_ring` | Smart Ring tab |
| `cis_feature_allergies` | Allergies |
| `cis_feature_emergency_contacts` | Emergency contacts |
| `cis_feature_family_health_history` | Family health history |
| `cis_feature_share_health_records` | Share health records |
| `cis_feature_faq` | FAQ |
| `cis_feature_feedback` | Feedback |

Every key defaults to `true` in the app, so a missing key or a failed fetch never hides a module.
A key can also be changed by hand in the Firebase console (Remote Config → *CIS feature flags*
group); the next save from the admin page overwrites that key again.

## One-time setup

1. **Blaze plan** — Cloud Functions need it.
2. **Firestore** — create the database if the project doesn't have one yet.
3. **Rules** — `firestore.rules` holds the project's full Firestore rules (the existing `watch_data`
   rule plus the feature-flags block). Deploying replaces every rule in the project, so first check
   the console for rules added since, then: `firebase deploy --only firestore:rules`.
4. **Deploy the function**
   ```sh
   npm install -g firebase-tools
   firebase login
   cd firebase/functions && npm install && cd ..
   firebase deploy --only functions:publishFeatureFlags
   ```
   If the deploy reports a region mismatch, add `region: "<firestore location>"` to the
   `onDocumentWritten` options in `functions/index.js`.
5. The function's service account needs the **Firebase Remote Config Admin** role (the default
   App Engine/compute service account normally has Editor, which includes it).

## Security note

The app signs in through its own backend (OTP), not Firebase Auth, so Firestore can't verify
who is writing. The admin page is shown only to the 9807039752 login and the rules accept only
switch fields, but someone who reverse-engineers the APK could still write the doc. To close that,
have the backend mint a Firebase custom token at login (`admin.auth().createCustomToken(uhid,
{ featureAdmin: true })` for the admin), sign in with it in the app, and change the rule to
`allow write: if request.auth.token.featureAdmin == true;`.
