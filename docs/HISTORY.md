# Local history preparation

The supplied project had no Git repository. Modernization and UI improvements were already complete when local commits were requested. These commits introduce the actual completed files in dependency order; they do not reconstruct historical development or import the insecure original backup. All commits use real current timestamps and the existing configured Git author identity. No remote or push is part of this work.

## Commit boundaries

### 1. chore: configure Maven project and repository exclusions

- `.gitignore`
- `pom.xml`
- `nbactions.xml`

### 2. feat: add SQLite persistence and domain repositories

- `src/main/java/camp_res_system/config/Database.java`
- `src/main/java/camp_res_system/model/Campsite.java`
- `src/main/java/camp_res_system/model/Feedback.java`
- `src/main/java/camp_res_system/model/Reservation.java`
- `src/main/java/camp_res_system/model/User.java`
- `src/main/java/camp_res_system/repository/CampsiteRepository.java`
- `src/main/java/camp_res_system/repository/FeedbackRepository.java`
- `src/main/java/camp_res_system/repository/ReservationRepository.java`
- `src/main/java/camp_res_system/repository/UserRepository.java`
- `src/main/resources/db/schema.sql`
- `src/main/resources/db/demo-campsites.tsv`

### 3. feat: secure authentication and role-based sessions

- `src/main/java/camp_res_system/service/AuthService.java`
- `src/main/java/camp_res_system/service/Passwords.java`
- `src/main/java/camp_res_system/service/Session.java`
- `src/main/java/camp_res_system/service/AppException.java`

### 4. feat: enforce reservation rules and application services

- `src/main/java/camp_res_system/service/ReservationService.java`
- `src/main/java/camp_res_system/service/CampsiteService.java`
- `src/main/java/camp_res_system/service/FeedbackService.java`
- `src/main/java/camp_res_system/config/AppContext.java`
- `src/main/java/camp_res_system/config/Logging.java`

### 5. style: add localized Swing presentation foundations

- `src/main/java/camp_res_system/i18n/Messages.java`
- `src/main/resources/i18n/messages.properties`
- `src/main/resources/i18n/messages_es.properties`
- `src/main/java/camp_res_system/ui/Theme.java`
- `src/main/java/camp_res_system/ui/PhotoPanel.java`
- `src/main/java/camp_res_system/ui/WrapLayout.java`
- `src/main/java/camp_res_system/ui/ScrollContent.java`
- `src/main/java/camp_res_system/ui/Documents.java`
- `src/main/resources/images/campsite.jpg`

### 6. feat: add redesigned customer and admin desktop workflows

- `src/main/java/camp_res_system/ui/AppFrame.java`
- `src/main/java/camp_res_system/ui/Screen.java`
- `src/main/java/camp_res_system/ui/CampsiteBrowser.java`
- `src/main/java/camp_res_system/ui/CampsitesView.java`
- `src/main/java/camp_res_system/ui/DashboardView.java`
- `src/main/java/camp_res_system/ui/FeedbackView.java`
- `src/main/java/camp_res_system/ui/ReservationsView.java`
- `src/main/java/camp_res_system/Project.java`
- `src/main/java/camp_res_system/lang.java`
- `src/main/java/camp_res_system/main.java`
- `src/main/java/camp_res_system/user_login.java`
- `src/main/java/camp_res_system/user_reg.java`
- `src/main/java/camp_res_system/welcome.java`
- `src/main/java/camp_res_system/customers.java`
- `src/main/java/camp_res_system/terms.java`
- `src/main/java/camp_res_system/bill.java`
- `src/main/java/camp_res_system/feedback.java`
- `src/main/java/camp_res_system/admin_login.java`
- `src/main/java/camp_res_system/manage.java`
- `src/main/java/camp_res_system/admin.java`
- `src/main/java/camp_res_system/ad_report.java`

### 7. test: cover booking integrity and desktop workflows

- `src/test/java/camp_res_system/service/ApplicationTest.java`
- `src/test/java/camp_res_system/ui/UiSmokeTest.java`

### 8. docs: document setup architecture and verification

- `README.md`
- `docs/AUDIT.md`
- `docs/DATABASE.md`
- `docs/VERIFICATION.md`
- `docs/HISTORY.md`
- `docs/screenshots/setup.png`
- `docs/screenshots/spanish-dashboard.png`
- `docs/screenshots/admin-report.png`
- `docs/screenshots/campsite-cards.png`
- `docs/screenshots/booking.png`

## Rationale and exclusions

The first commit is build configuration, not a runnable application. Persistence, authentication, services and presentation primitives are independently compilable layers. The shared shell and customer/admin views are introduced together because splitting those files further would leave missing Java dependencies or require fabricated stubs. The complete application becomes runnable with the workflow commit. Tests and documentation follow.

The original top-level `images/` collection is unused by the modern application and remains ignored locally; the one packaged asset is tracked under `src/main/resources/images/`. `.local-backup/`, `data/`, `target/`, `.vscode/`, `feedback.text`, logs and exports stay outside Git. Test password strings belong only to temporary test accounts and are not deployment credentials. Documentation screenshots contain fictional test data.

Staged snapshots are exported into the ignored local backup directory for intermediate Maven checks so untracked later layers cannot hide missing dependencies. The final repository is verified again after all commits. See `VERIFICATION.md` for application test coverage and build limitations.
