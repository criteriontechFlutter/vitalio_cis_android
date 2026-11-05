# Requirements Traceability

**This matrix was reconstructed from the code on 2026-11-05.** The feature list shows what is built, not what was approved. The `F-` numbers were assigned for this document and are not approved requirement IDs.

To complete it, add the task or requirement ID from the approved source for each feature. Do not fill this column from memory.

## How to read the test column

- **Unit:** an automated test class exists in the repository for part of the feature. See [07-test-report.md](07-test-report.md) for results.
- **Instrumented, not run:** a device test exists but has no recorded result.
- **None found:** no feature-specific automated test exists. `RoutesTest` checks navigation routes across the app and is not listed for each feature.

Manual test results are not in the repository for any feature.

## Matrix

| ID | Feature | Requirement or task ID | Main screens (`ui/screens`) | Other code | Feature flag | Automated tests |
|---|---|---|---|---|---|---|
| F-01 | Login with OTP | To be filled | `LoginView`, `OtpView`, `onboarding` | | | Unit: `LoginViewModelTest` |
| F-02 | Vitals and history | To be filled | `VitalDetailsView`, `VitalHistoryView`, `VitalHeroes`, `ConnectionView` | | `cis_feature_vitals` | None found |
| F-03 | Fluid intake and output | To be filled | `FluidDataInputView`, `FluidInputHistoryView`, `FluidOutputHistoryView` | | `cis_feature_fluid` | None found |
| F-04 | Symptom tracking | To be filled | `SymptomTrackerView`, `SymptomsView`, `SymptomHistoryView` | | `cis_feature_symptoms` | None found |
| F-05 | Medicine reminders | To be filled | `MedicineReminderView`, `AddMedicineReminder`, `ManageMedicineView`, `MedicineReminderSettingsView`, `RemindersView` | `reminder` | `cis_feature_medicine` | Unit, models only: `AppointmentAndMedicineModelTest` |
| F-06 | Diet and nutrition | To be filled | `DietChecklistView`, `DietBoxView`, `DietPlanBuilderView`, `DietAddFoodView`, `NutrientSearchView`, `NutritionAnalyticsView` | | `cis_feature_diet` | None found |
| F-07 | Drug interaction checker | To be filled | `InteractionCheckerView`, `InteractionInstructionView` | | `cis_feature_interaction_checker` | None found |
| F-08 | Doctor search and booking | To be filled | `FindDoctorView`, `DoctorsDetailsView`, `SelecteClinicView`, `BookingDetailsView`, `BookingConfirmation`, `AppointmentView` | | `cis_feature_appointments` | Unit, models only: `AppointmentAndMedicineModelTest` |
| F-09 | Video consultation | To be filled | `CallView` | `consultation` | | None found |
| F-10 | Lab reports and AI report | To be filled | `AddLabResultsView`, `UploadReportView`, `AiReportView` | | `cis_feature_lab_reports` | None found |
| F-11 | Prescriptions and analysis | To be filled | `PrescriptionView`, `UploadPrescriptionView`, `PrescriptionAnalysisView` | | `cis_feature_prescriptions` | None found |
| F-12 | Voice assistant | To be filled | | `voice`, `:core` | `cis_feature_voice_assistant` | Unit: `VoiceNlpParserTest`, `TranscriptStitcherTest` |
| F-13 | Activity tracking | To be filled | `AddActivityView`, `ActivityHistoryView` | | `cis_feature_activity` | None found |
| F-14 | Smart ring | To be filled | `SmartRingView` | `ring` | `cis_feature_smart_ring` | None found |
| F-15 | Health Connect sync | To be filled | `HealthConnectSyncView` | `healthconnect` | | Instrumented, not run: `HealthConnectSyncTest` |
| F-16 | Omron devices | To be filled | `OmronDeviceView` | `omron` | | Unit: `OmronDeviceFilterTest`, `OmronParseTest` (4 failing) |
| F-17 | Yonker BP machine and oximeter | To be filled | `YonkerDeviceView` | `yonker` | | Unit: `YonkerFramesTest` |
| F-18 | Wear OS companion | To be filled | `ConnectWatchView` | `watch`, `:wear` | | None found for `:wear` |
| F-19 | Allergies | To be filled | `AlergiesView` | | `cis_feature_allergies` | None found |
| F-20 | Emergency contacts | To be filled | `EmergencyContactsView` | | `cis_feature_emergency_contacts` | None found |
| F-21 | Family health history | To be filled | `FamilyHealthHistory` | | `cis_feature_family_health_history` | None found |
| F-22 | Share health records | To be filled | `HealthShareView` | | `cis_feature_share_health_records` | None found |
| F-23 | Shared accounts and observers | To be filled | `SharedAccountView`, `AddMemberView`, `MyObservers` | | | None found |
| F-24 | Profile and medical profile | To be filled | `PersonalInfoView`, `EditProfileScreen`, `MedicalProfileView` | | | None found |
| F-25 | Articles | To be filled | `FeaturedArticiesView`, `ResearchBasedArticlesView` | | | None found |
| F-26 | FAQ and feedback | To be filled | `FAQView`, `FeedbackView` | | `cis_feature_faq`, `cis_feature_feedback` | None found |
| F-27 | Digital twin | To be filled | `DigitalTwinView` | | | None found |
| F-28 | Health Orbit | To be filled | `HealthOrbitView` | | | None found |
| F-29 | Breathing therapy | To be filled | `BreathingTherapyView` | | | None found |
| F-30 | Permissions screen | To be filled | `PermissionsView` | `permissions` | | None found |
| F-31 | Feature flag admin | To be filled | `FeatureFlagsAdminView` | `config`, `firebase` | | None found |
| F-32 | Dashboard and navigation drawer | To be filled | `HomeView`, `DrawerView` | | | None found |

## Notes

- **F-28:** the entry icon on the Vitals screen was hidden on 2026-11-04. The route and screen remain in the code, and the entry on the Home screen is also commented out. See CR-001 in [06-change-records.md](06-change-records.md).
- **Links to commits:** no commit message contains a task ID, so features cannot be linked to commits reliably. The commit list is in [05-change-history.md](05-change-history.md).
