# Orbit Mobile (Android Native) — التحليل الكامل وخطة العمل

> **الهدف:** بناء تطبيق Android جديد كليًا (Kotlin + Jetpack Compose، Clean Architecture + MVVM) في `G:\Orbit_Mobile_Native`
> بتطابق 100% مع منصة Orbit الويب (Backend FastAPI + Client React) — بدون إسقاط أي ميزة.
>
> **حالة المستند:** نتيجة تحليل الكود المصدري يوم 2026-06-12. هذا هو المرجع الأساسي (Master Reference) للبناء.

---

# الجزء 1 — قائمة الميزات الكاملة (Feature Parity Checklist)

## A. دورة حياة التطبيق (System Boot)

| # | الميزة | المصدر في الويب |
|---|--------|-----------------|
| A1 | فحص `GET /system/init-status` عند الإقلاع (timeout = 8 ثواني) | `App.tsx` |
| A2 | شاشة **Setup Wizard** لإنشاء حساب الـ Founder أول مرة → `POST /auth/setup` | `SetupPage.tsx` |
| A3 | شاشة **Backend Down**: عداد تنازلي 10 ثواني + إعادة تحميل تلقائية + زر Retry now | `App.tsx` |
| A4 | شاشة تحميل أولية "Starting up…" (spinner) | `App.tsx` |

## B. المصادقة والجلسات (Auth & Sessions)

- **Login** بالإيميل وكلمة السر → الرد `{_id, email, name, role, token}`
- التوجيه بعد الدخول حسب الدور (`Login.tsx:56`):
  - `founder` → Command Center (/founder)
  - `sub_admin` → SubAdmin Portal (/subadmin)
  - `it_staff` → IT Console (/it-portal)
  - `quality_control` / `quality_manager` → QC Dashboard (/quality-control)
  - غير ذلك → Dashboard
- **Signup العام معطّل** (route بيحوّل على /login) — إنشاء الحسابات فقط عبر IT / Founder / Configuration
- **Logout** مع Token Blacklist → `POST /auth/logout`
- **Change Password** → `POST /auth/change-password` (من Settings للمستخدم نفسه، ومن ITPortal/Configuration كـ admin reset)
- تسجيل **Session** عند كل login (User-Agent parsing لاسم المتصفح/النظام + IP) + **Audit Log** لكل register/login
- **Maintenance Mode** يمنع الدخول (auth dependency) + **Force Logout** لمستخدم محدد أو للجميع
- هيدرز كل النداءات: `Authorization: Bearer <jwt>` + `X-User-Id: <id>`
- مفاتيح التخزين المحلي في الويب (هتتحول DataStore): token, userId, role, fullName, email, userAvatar, theme

## C. نظام الأدوار والصلاحيات (RBAC) — مستويان

### C1. أدوار المنصة (user.role) — 8 أدوار
`founder` · `it_staff` · `admin` · `manager` · `sub_admin` · `staff` · `quality_control` (+ `quality_manager` legacy)

### C2. منيو التنقل لكل دور (من `Sidebar.tsx:144` — مصدر الحقيقة)

| الدور | عناصر المنيو |
|------|---------------|
| admin | Dashboard, Workspace(/projects), Calendar, Team, Reports, Quality Control, Settings, User Management(/configuration) |
| quality_control / quality_manager | Quality Insights, Reports(/quality-control), Settings |
| sub_admin | Dashboard(/subadmin), Workspace, Calendar, Team, Reports, Settings |
| staff | Dashboard, My Projects, Public Channel(/taskflow?projectId=public-group), Calendar, Settings |
| founder | Command Center(/founder), IT Accounts, Settings |
| it_staff | IT Console, Credentials, User Management, Settings |
| manager | Dashboard, Projects, My Team, Reports, Settings |
| default (member) | Dashboard, Projects, Tasks, Calendar, Team, Settings |

### C3. حُرّاس التوجيه (App.tsx → Navigation Guards في الموبايل)
- `restrictForStaff` — staff ممنوع من projects/tasks/teams/portfolio
- `restrictForSubAdminOnly` — /taskmaster لـ sub_admin فقط
- `restrictToQualityControl` — QC pages لـ quality_control/quality_manager/admin/manager/sub_admin
- `redirectSubAdminDashboard` / `redirectQualityDashboard` / `redirectItStaff` — تحويل الداشبورد الافتراضي
- `restrictToFounder` — صفحات /founder/*

### C4. أدوار الفريق (membership.role)
`admin` / `subadmin` / `manager` / `member` + حقل `managed_by` (الـ SubAdmin يدير أعضاء محددين)

**فلترة رؤية المهام** (`rbac.py:216`):
- Admin / Manager → كل مهام الفريق
- SubAdmin → مهام الأعضاء الذين يديرهم فقط
- Member → مهامه فقط

## D. الشاشات (كل routes الويب = 33)

| Route | الشاشة | الميزات |
|-------|--------|---------|
| /login | Login | فورم مطابق للويب: لوحة براند داكنة (gradient) + فورم 400px، إظهار/إخفاء كلمة السر، error box |
| /setup | SetupPage | معالج إنشاء الـ Founder (مرة واحدة) |
| /dashboard | Dashboard | متكيّف بالدور: نسخة Staff (مشاريعي + إحصائيات شخصية `analytics/user/{id}/dashboard` + قائمة مهامي) / نسخة Admin (KPIs + Users + Audit feed) + seed-demo تلقائي لو البيانات فاضية |
| /projects | Projects | كروت/جدول مشاريع + tabs + إنشاء/تعديل/حذف مشروع + إدارة أعضاء الفريق inline + فلاتر |
| /projects/active | ActiveProjects | قائمة `?status=ACTIVE` |
| /projects/progress | ProgressProjects | قائمة المشاريع الجارية |
| /projects/public | PublicProjects | قائمة المشاريع العامة |
| /tasks | TasksPage | قائمة المهام |
| /create-task | CreateTask | اختيار فريق → أعضاء → إنشاء مهمة: multi-assignee + assign_to_all + visibility (team/private) + priority + report_type |
| /calendar | CalendarPage | شبكة شهرية + CRUD أحداث كامل + RSVP (التفاصيل في E5) |
| /team | TeamPage | دليل الأعضاء + Leaderboard أداء (`analytics/user/{id}`) |
| /teams، /teams/:id | TeamsPage/Details | قائمة الفرق + تفاصيل فريق وأعضائه |
| /portfolio/:userId | PortfolioPage | بروفايل + portfolio + analytics للمستخدم |
| /reports | ReportsPage | KPIs المنصة (`analytics/platform-overview`) + رسوم + Workspace Health |
| /settings | SettingsPage | 4 تبويبات: Profile (avatar رفع/حذف + bio/job/department/country/timezone) · Security (تغيير كلمة سر + sessions + activity) · Notifications · Appearance (Light/Dark/System) |
| /taskmaster، /task-master | TaskMasterDashboard | عرض تحليلي للمشاريع (sub_admin فقط) |
| /subadmin | SubAdminPortal | KPIs + health score + مشاريع مع ai_score + توزيع الفريق (`analytics/subadmin/dashboard`) |
| /taskflow?projectId= | TaskFlowDetail ⭐ | قنوات النظام (التفاصيل في E2) |
| /workspace?projectId= | ProjectWorkspace ⭐ | الـ Kanban الكامل (التفاصيل في E1) |
| /configuration | ConfigurationPage | إدارة مستخدمين: إنشاء (auth/register) + تعديل + admin password reset + force-logout + حذف + مصفوفة الأدوار |
| /session-logs | SessionLogsPage | جدول جلسات كل المستخدمين (`users/sessions`) |
| /it-portal | ITPortal | 10 أقسام: dashboard, users, roles, security, sessions, audit, credentials, health, settings (maintenance + max upload), profile |
| /my-projects | StaffProjectsPage | مشاريع الـ staff |
| /staff-task | StaffTaskDetail | تفاصيل مهمة staff: تنزيل Template حسب نوع التقرير (`quality/templates/{key}?lang=ar|en`) + تسليم AI + عرض verdict |
| /quality-control، /quality-manager، /qc/dashboard | QCDashboard | 5 تبويبات: Intelligence / Overview / Standards (CRUD) / Evaluations / Analytics + heatmap نشاط 16 أسبوع + export CSV/PDF |
| /quality-insights | QualityInsights | QC overview آخر 60 يوم (`qc/reports/overview?days=60`) + تنزيل تقارير |
| /founder | FounderDashboard | Metrics (entities/users/MRR + tier_distribution/avg QA/pending evals/uptime) + AI trends 7 أيام + System Alerts + Frameworks CRUD + Entities + quick actions |
| /founder/accounts | FounderAccountsPage | حسابات IT: إنشاء/تعديل/reset password/toggle status/حذف + بحث |
| /founder/credentials | CredentialsPage | سجل credentials نصي (founder + it_staff) + تنزيل ملف |
| /help | HelpPage | مركز مساعدة ثابت |
| (عائم) | ChatbotWidget 🤖 | في كل الصفحات عند تسجيل الدخول (عدا /workspace) |

## E. الأنظمة الجوهرية — تفاصيل التطابق الحرفي

### E1. Kanban Board (ProjectWorkspace — 2532 سطر)
- 4 أعمدة: `todo` / `in_progress` / `review` / `done` (lowercase في PATCH todos)
- ألوان الأعمدة: todo `#64748B` · in_progress `#1D6EF5` · review `#F59E0B` · done `#10B981`
- أولويات: `urgent #EF4444` / `high #F97316` / `medium #F59E0B` / `low #64748B`
- كروت غنية: assignees متعددون + due مع تلوين (overdue أحمر، ≤3 أيام كهرماني) + report_type + completed_by/names + visibility
- إنشاء مهمة inline في العمود + تحديث optimistic مع rollback عند الفشل
- **Task Detail Panel** — 4 تبويبات: `Info` / `Discussion` (replies + @mentions + attachments) / `Files` / `AI` (تاريخ تقييمات + تسليم)
- **Right Sidebar** — 4 تبويبات: `Team` / `Chat` / `Files` / `AI` مع badges بالأعداد
- إدارة أعضاء: إضافة من `members/available` + تعديل responsibility + إزالة
- Resources: رفع (FormData) / تنزيل / حذف
- Endpoints: `task-boards/{projectId}` + `/todos` + `/todos/{taskId}` + `/members` + `/members/{id}` + `/members/available` + `/comments` + `/comments/{id}` + `/resources` + `/resources/{id}` + تنزيل المرفقات

### E2. قنوات النظام (TaskFlowDetail — 967 سطر)
- قناتان افتراضيتان (مشاريع نظامية تُفلتر من قوائم المشاريع):
  - `public-group` 🌐 — gradient `#6D28D9 → #4F46E5 → #2563EB`
  - `all-sub-admin` ⚡ — gradient `#C2410C → #EA580C → #F59E0B`
- **Posts** بصيغة نصية: `[POST:announcement|task|note] العنوان\nالمحتوى` (3 أنواع بألوان وأيقونات: 📢 indigo / 📋 green / 📝 amber)
- **Replies** بصيغة: `[REPLY:<commentId>]\nالنص`
- **@Mentions**: التقاط `@` أثناء الكتابة → قائمة أعضاء (بحث، أول 6) → `mentioned_user_ids` في الطلب
- مرفقات شات + معاينة صور + تنزيل
- **Reactions**: 5 إيموجي (👍 ❤️ 😂 🔥 ✅) — **client-side فقط، غير مخزنة في الباك** (parity حرفي = نفس السلوك)
- Resources: رفع/حذف/تنزيل + ألوان حسب الامتداد (pdf أحمر، doc أزرق، xls أخضر، صور بنفسجي)
- Members مع online dot + عدّاد online
- Todos مع toggle done (optimistic)
- Activity Feed مشتق من آخر 20 comment + آخر 5 resources
- صلاحية الحذف: صاحب العنصر أو دور admin/sub_admin/manager
- التبويب الأيسر: board / tasks / files / members

### E3. خط أنابيب AI Review ⭐ (الأهم)
1. `GET /quality/report-types` → **20 نوع تقرير** (من `qc_service.REPORT_TYPES`): course_report, program_report, course_specification, program_specification, self_study, execution_plan_followup, survey_analysis, exam_results_analysis, reviewer_reports, financial_reports, field_training_reports, student_activities, hr_performance, training_plans_execution, qa_unit_annual, student_data_reports, research_activity, research_ethics, physical_resources, community_engagement — كل نوع: key + name_ar + name_en + description + required_elements
2. **TaskSubmitDrawer** (في الموبايل: Bottom Sheet): سحب وإفلات ملفات PDF/Word/صور → تحويل base64 بطريقة **chunked 32KB** (مهم للملفات الكبيرة) → `POST /quality/evaluate` بالحقول: `task_title, task_description, task_id, report_type, files[{file_name, content, file_type}], image_base64[], submission_notes`
3. النتيجة: `compliance_score` + `passed_standards[{rule,result}]` + `failed_standards[{rule,reason}]` + `suggestions[]` + `report_type_compliance{is_compliant, missing_elements, compliance_note}` + `evaluation_id`
4. **العتبات (انسخها حرفيًا):**
   - الواجهة: نجاح عند `score >= 70` → auto-submit بعد 3 ثواني (`TaskSubmitDrawer.tsx:77`)
   - الباك: `is_passed = score >= 85` (`quality.py:340`) والـ verdict `ACCEPTED` عند `>= 70` (`qc_service.py:1273`)
   - حدود الرفع: 5 ملفات / 10 صور، ~20MB للملف، ~10MB للصورة
5. ملحقات: `GET /ai/history` (آخر 10 تقييمات) · Templates تنزيل لكل نوع (`?lang=ar|en`) · QC standards CRUD · `qc/analyses` · `qc/reports/overview` + `export` CSV/PDF · `PATCH quality/tasks/{id}/verdict` · `quality/chat` (نقاش حول تقييم) · roadmap · best-practices · datasets + train + model/state · RAG admin (status / index / wipe)

### E4. الشات والريل-تايم — حقيقة مهمة ⚠️
**الويب لا يستخدم WebSocket إطلاقًا** (لا يوجد `new WebSocket` في الـ client). الباك يوفر `ws://…/ws/{project_id}?token=` بأحداث موثقة (task_created/updated/status, comment_added, file_uploaded, member_added, progress_updated) لكن الويب يعمل:
- refetch بعد كل action (إرسال رسالة/رفع ملف يرجّع الـ Board كامل)
- polling للإشعارات كل 30 ثانية (Header)

**قرار الـ parity (محدّث بقرار المستخدم):** الموبايل سيستخدم **WebSocket من البداية** لغرف المشاريع (chat/board live updates عبر `ws/{project_id}?token=` — عند أي event نعمل refetch صامت)، مع الإبقاء على refetch-بعد-الـ-action كـ fallback. الإشعارات (Header) تبقى polling كل 30 ثانية لأن الباك لا يوفر WS لها.

### E5. التقويم والأحداث (Events)
- أنواع (10): meeting, project_deadline, review_session, training_session, quality_audit, team_event, reminder, personal_event, task_deadline, milestone
- visibility (4): private / team / project_members / workspace
- priority (4) + recurrence (5): none/daily/weekly/monthly/yearly
- حقول: عنوان/وصف/تاريخ بداية-نهاية/وقت/مكان/رابط اجتماع/مشروع/مهمة/حضور/تذكير (15/60/720/1440 دقيقة)/لون مخصص
- RSVP: `POST /events/{id}/rsvp` + CRUD كامل

### E6. الإشعارات
- `GET /notifications` (poll 30s) — أنواع: info/success/warning/error/mention — payload: {title, body}
- `PATCH /notifications/{id}/read` + `PATCH /notifications/read-all`
- Badge بعدد غير المقروء على الجرس

### E7. Chatbot (Orbit AI)
- `POST /chat {message}` → reply (Gemini passthrough — بدون تخزين history في الباك)
- زر عائم + لوحة شات: GREETING ثابتة، typing indicator (3 نقاط)، unread badge، تنسيق `**bold**`
- مخفي في صفحة /workspace (الشات الداخلي موجود هناك)

### E8. أنظمة مساندة
- **Analytics**: seed-demo (auto عند البيانات الفاضية) · platform-overview · user/{id} · user/{id}/dashboard · subadmin/dashboard · team/{id}
- **Presence**: `POST /users/me/heartbeat` موجود بالباك (last_seen + مدة الجلسة) — **الويب لا يستدعيه** (سنطابق: لن نستدعيه في v1)
- **Audit Logs** + **Upload Rules** per project (GET/PUT) + **Profile/Portfolio** + **Teams/Memberships CRUD**

## F. جرد الـ Backend الكامل (~120 endpoint تحت `/api/v1`)

| Router | Endpoints |
|--------|-----------|
| auth | POST register · POST login · GET me · POST change-password · POST setup · POST logout |
| users | POST me/heartbeat · GET me · PATCH me/profile · DELETE me/avatar · GET me/sessions · GET me/activity · GET "" (list؟role filter) · GET count · PATCH {id} · DELETE {id} · GET sessions |
| teams | POST · GET (my teams) · GET {id} · PUT {id} · DELETE {id} |
| memberships (`/teams/{id}/members`) | POST · GET · PUT {userId} · DELETE {userId} |
| tasks | POST · GET (by team) · GET {id} · PUT {id} · PATCH {id}/assign · DELETE {id} |
| projects | POST · GET count · GET stats · GET (list؟status) · GET {id} · PATCH {id} · DELETE {id} · POST {id}/staff · PATCH {id}/toggles |
| task-boards | GET {pid} · PATCH {pid} · POST {pid}/todos · PATCH {pid}/todos/{tid} · DELETE {pid}/todos/{tid} · POST {pid}/members · PATCH {pid}/members/{mid} · GET {pid}/members/available · POST {pid}/comments · DELETE {pid}/comments/{cid} · GET {pid}/comments/{cid}/attachments/{aid} · POST {pid}/resources · DELETE {pid}/resources/{rid} · GET {pid}/resources/{rid} |
| events | GET · POST · GET {id} · PUT {id} · DELETE {id} · POST {id}/rsvp |
| quality | POST/GET rules · PUT/DELETE rules/{id} · GET report-types · POST evaluate · GET evaluations · GET evaluations/{id} · PATCH tasks/{id}/verdict · POST chat · POST roadmap · GET best-practices · POST/GET datasets · POST train · GET model/state · GET reports/project/{id} · GET reports/overview · GET templates · GET templates/{key} |
| qc | POST/GET standards · GET/PUT/DELETE standards/{id} · POST standards/{id}/dataset · POST tasks/{id}/analyze · GET tasks/{id}/analysis/latest · GET analyses · GET analyses/{id} · GET reports/overview · GET reports/export |
| analytics | POST seed-demo · GET platform-overview · GET team/{id} · GET user/{id} · GET user/{id}/dashboard · GET subadmin/dashboard |
| founder | GET metrics · GET audit-logs · GET credentials · GET credentials/download · GET accounts · POST accounts · PATCH accounts/{id} · POST accounts/{id}/reset-password · POST accounts/{id}/toggle-status · DELETE accounts/{id} |
| system | GET init-status · GET health · PATCH settings · POST force-logout/{uid} · POST logout-all · GET audit-logs |
| frameworks | GET / · POST / · GET {id} · PATCH {id} |
| entities | GET · POST · POST {id}/assign-it |
| notifications | GET · PATCH {id}/read · PATCH read-all |
| upload-rules | GET/PUT projects/{id}/upload-rules |
| rag | GET status · POST index · DELETE index |
| chat | POST (chatbot) |
| ai | GET history |
| profile | GET me · PATCH me · GET {userId} · GET {userId}/portfolio |
| ws | WS /ws/{project_id}?token= (غير مستخدم من الويب) |

## G. ملاحظات Parity حساسة (لا تُنسى)

1. لا WebSocket في الويب → fetch/poll فقط (انظر E4)
2. الـ Reactions في القنوات client-side فقط
3. عتبات AI: عرض 70 / تخزين 85 / verdict 70
4. `/signup` معطّل
5. `Tasks.tsx` ملف stub ("Coming soon") غير مربوط بالـ routes — الصفحة الحقيقية `TasksPage.tsx`
6. `public-group` و `all-sub-admin` مشاريع نظامية تُستبعد من القوائم
7. Dashboard فيه fallback عينات للـ staff عند فشل/خلو الـ API + seed-demo تلقائي
8. الثيم الافتراضي عند عدم وجود تخزين = **Dark** (`ThemeContext.tsx:17`)
9. heartbeat غير مستدعى من الويب
10. الهيدرز دائمًا: Bearer + X-User-Id

---

# الجزء 2 — Design System → Compose Theme

## 2.1 الألوان

### Brand & Semantic (ثابتة في الوضعين)
```
primary   #1D6EF5    sky      #0EA5E9
success   #10B981    warning  #F59E0B
danger    #EF4444    purple   #8B5CF6
teal      #06B6D4    orange   #F97316
slate     #64748B
```

### Light Mode
```
appBackground   #F5F6FA   (صفحات workspace/channels: #F0F4F8)
surface         #FFFFFF
surface2        #F8FAFC
surface3        #EDF2F7
border          rgba(0,0,0,.07)   (عام: .08)
textPrimary     #0F172A
textSecondary   #334155  (token عام: #475569)
textMuted       #94A3B8
headerBg        rgba(255,255,255,.94)
sidebarBg       #FFFFFF
```

### Dark Mode
```
appBackground   #0A0C14   (صفحات workspace/channels: #0B0D14)
surface         #111420
surface2        #161924   (بديل: #171B2E)
surface3        #1C2030   (بديل: #1D2235)
sidebarBg       #0F1220
headerBg        rgba(10,12,20,.92)
popupBg         #070A1A   (قوائم: #131724)
border          rgba(255,255,255,.07)
textPrimary     #F0F4F9   (body عام: #E5E7EB)
textSecondary   rgba(255,255,255,.62)
textMuted       rgba(255,255,255,.28 – .38)
authPanelBg     #0F172A
```

### Gradients
```
brandAuthPanel   160°  #0C1A38 → #071028 → #04091A  (+ توهجات radial زرقاء/بنفسجية)
avatarFallback   135°  #1D6EF5 → #8B5CF6
channelPublic    135°  #6D28D9 → #4F46E5 → #2563EB
channelOps       135°  #C2410C → #EA580C → #F59E0B
```

### ألوان وظيفية
```
Kanban:    todo #64748B · in_progress #1D6EF5 · review #F59E0B · done #10B981
Priority:  urgent #EF4444 · high #F97316 · medium #F59E0B · low #64748B
Roles:     founder #F59E0B · it_staff #06B6D4 · admin #1D6EF5 · sub_admin #8B5CF6
           manager #0EA5E9 · quality_control #10B981 · staff #64748B
Status:    COMPLETED → emerald · ON HOLD → amber · default → blue
Online dot: #10B981 مع glow
```

## 2.2 الخطوط (تُحزم TTF داخل res/font — بدون Google Fonts provider)

| الاستخدام | الخط | الأوزان |
|-----------|------|---------|
| Body | Inter | 300–800 |
| Display / Logo / عناوين | Space Grotesk | 400–700 (letterSpacing −0.01em) |
| Labels تقنية | Chivo Mono | 500–600 (12sp، tracking +0.04em، uppercase) |

مقاسات شائعة من الويب: 9–22sp، html base = 16.

## 2.3 الأشكال والظلال
```
Cards 12dp · Modals/Sheets 14dp · Buttons 10dp · Inputs 8dp · Chips pill (20dp)
focus ring: rgba(29,110,245,.12) بسماكة 3dp
shadow-card: y1 blur3 .10 + y1 blur2 .06
انتقالات الثيم: 200ms
```

## 2.4 سلوك الثيم (مطابق للويب)
- 3 أوضاع: Light / Dark / **System** — الافتراضي بدون تخزين = **Dark**
- التخزين في DataStore (مكافئ `localStorage['theme']`)
- التبديل لحظي من Settings → Appearance

## 2.5 خريطة ملفات Compose
- `core/theme/Color.kt` — كل الـ tokens + `OrbitExtendedColors` (surface2/3, sidebar, kanban, priority, roles, gradients) عبر CompositionLocal
- `core/theme/Theme.kt` — `OrbitTheme(themeMode)` → dark/lightColorScheme + System
- `core/theme/Type.kt` — العائلات الثلاث + الأحجام
- `core/theme/Shape.kt`

---

# الجزء 3 — خطة العمل (Action Plan)

## التقنية
Kotlin 2.x · Compose BOM + Material3 · Navigation-Compose · Hilt · Retrofit + OkHttp + kotlinx-serialization · DataStore · Coil · SplashScreen API · minSdk 24 / target 35

## الهيكل (Clean Architecture)
```
app/
 └─ src/main/java/com/orbit/mobile/
     ├─ core/      (network, datastore, theme, l10n, ui-components, util)
     ├─ data/      (dto, api interfaces, repository implementations)
     ├─ domain/    (models, repository interfaces, usecases)
     └─ feature/   (auth, shell, dashboard, projects, workspace, channels,
                    aireview, qc, calendar, reports, itportal, founder,
                    settings, chatbot, teams, …)  ← Screen + ViewModel لكل شاشة
```

## قواعد ملزمة أثناء التنفيذ
1. تعليقات إنجليزي فقط — كلمة أو كلمتين كحد أقصى (`// Fetch data`)
2. صفر Hardcoded strings — كل نص في `values/strings.xml` (EN) + `values-ar/strings.xml` (عربي مصري)
3. Dark/Light كاملان لكل شاشة من أول يوم
4. تحويلات Mobile-First: Sidebar → Bottom Navigation (4–5 عناصر حسب الدور) + الزائد في More/Drawer · الجداول → Cards/LazyColumn · الـ Modals → Bottom Sheets · TaskSubmitDrawer → ModalBottomSheet · Kanban → أعمدة بتمرير أفقي مع عدادات
5. كل مرحلة تُقفل Build-green + مطابقة بصرية، وبموافقتك قبل التالية

## المراحل والمهام الصغيرة

### P0 — Scaffold المشروع
- [ ] إنشاء مشروع Empty Compose في `G:\Orbit_Mobile_Native`
- [ ] ضبط Gradle/AGP + إضافة الحزم (Hilt, Retrofit, DataStore, Coil, Navigation, Serialization)
- [ ] هيكل الباكدجات core/data/domain/feature
- [ ] فحص بيئة البناء (JAVA_HOME, Gradle home — انظر "ملاحظات بيئة")

### P1 — Design System + L10n
- [ ] Color.kt (كل tokens الجزء 2) + Theme.kt (3 أوضاع، افتراضي Dark) + Type.kt + Shape.kt
- [ ] حزم خطوط Inter / Space Grotesk / Chivo Mono (TTF)
- [ ] مكونات أساس: OrbitButton, OrbitTextField, OrbitCard, Badge, Avatar (initials + gradient), StatusDot, EmptyState, Skeleton, OrbitLogo (O مع النقطة المتوهجة)
- [ ] بنية strings.xml EN + values-ar (مصري) + أول دفعة نصوص مشتركة

### P2 — Data Core
- [ ] RetrofitClient + OkHttp interceptors (Bearer + X-User-Id) + logging
- [ ] DataStore: token/userId/role/name/email/avatar/theme/lang
- [ ] Result/NetworkError wrapper موحّد + error mapping (401/403/422/timeout)
- [ ] Hilt modules (Network, DataStore, Repositories)
- [ ] BASE_URL قابل للتهيئة (default `http://10.0.2.2:8000/api/v1`)
- [ ] ChatSocket: عميل OkHttp WebSocket لـ `ws/{project_id}?token=` (اتصال/قطع/إعادة محاولة + تدفق الأحداث)

### P3 — Boot & Auth
- [ ] Splash + فحص init-status (timeout 8s)
- [ ] شاشة Backend-Down (عداد 10s + retry)
- [ ] Setup Wizard (founder أول مرة)
- [ ] شاشة Login بتصميم الويب (brand panel أعلى الشاشة موبايليًا + الفورم)
- [ ] حفظ الجلسة + التوجيه بالدور + Logout مع blacklist

### P4 — App Shell
- [ ] Bottom Navigation ديناميكي حسب الدور (8 تشكيلات المنيو من C2)
- [ ] Top bar: عنوان الصفحة + جرس الإشعارات + avatar
- [ ] لوحة الإشعارات (poll 30s + read/read-all + أنواع بألوانها)
- [ ] Navigation graph + Route guards (C3)

### P5 — Dashboards
- [ ] Staff dashboard (إحصائيات + مشاريعي + مهامي بالحالات)
- [ ] Admin dashboard (KPIs + users + audit feed)
- [ ] منطق seed-demo التلقائي عند الخلو

### P6 — Projects & Teams & Tasks
- [ ] قوائم المشاريع + فلاتر (All/Active/Progress/Public)
- [ ] إنشاء/تعديل/حذف مشروع (Bottom Sheet مطابق لـ CreateProjectModal)
- [ ] Teams CRUD + أعضاء + أدوار الفريق + TeamDetails
- [ ] TeamPage مع Leaderboard
- [ ] CreateTask الكامل (multi-assignee + assign_to_all + visibility + priority + report_type)
- [ ] TasksPage

### P7 — Kanban Workspace ⭐ (تُقسم 4 دفعات)
- [ ] 7a: الأعمدة الأربعة + الكروت + نقل الحالة + إنشاء inline
- [ ] 7b: Task Detail Sheet (Info / Discussion / Files / AI)
- [ ] 7c: Members + Resources panels
- [ ] 7d: Project Chat (replies + mentions + attachments) + ربط WebSocket (تحديث حي للـ board/chat)

### P8 — Channels ⭐
- [ ] شاشة القناة (هيدر gradient + معلومات)
- [ ] Posts (3 أنواع) + Replies + Mentions
- [ ] Reactions (client-side) + المرفقات + معاينة الصور
- [ ] Resources / Members / Todos / Activity tabs
- [ ] ربط WebSocket للقناة (رسائل حية)

### P9 — AI Review ⭐
- [ ] Report types (20) — قائمة اختيار ar/en
- [ ] Submit Sheet: ملفات → base64 chunked + صور + ملاحظات
- [ ] شاشة النتيجة (score دائري + passed/failed standards + suggestions + compliance) بعتبات 70/85 الحرفية
- [ ] AI History + Templates download (ar/en) + StaffTaskDetail

### P10 — Chatbot
- [ ] FAB عائم بهوية Orbit AI + Bottom Sheet شات
- [ ] POST /chat + typing indicator + unread badge + تنسيق bold

### P11 — Calendar
- [ ] شبكة شهرية + نقاط الأحداث
- [ ] Event CRUD بكل الحقول (E5) + RSVP + فلاتر

### P12 — QC Suite
- [ ] QCDashboard (5 tabs + heatmap 16 أسبوع)
- [ ] Standards CRUD + Evaluations list
- [ ] Export CSV/PDF + QualityInsights

### P13 — Reports & Portals
- [ ] ReportsPage (KPIs + رسوم Compose)
- [ ] SubAdminPortal + TaskMasterDashboard
- [ ] PortfolioPage

### P14 — IT Suite
- [ ] ITPortal — الأقسام العشرة
- [ ] ConfigurationPage (إدارة مستخدمين كاملة)
- [ ] SessionLogs + Credentials + Maintenance toggle

### P15 — Founder Suite
- [ ] FounderDashboard (metrics + MRR/tiers + AI trends + alerts)
- [ ] Frameworks CRUD + Entities + assign-IT
- [ ] FounderAccountsPage + HelpPage

### P16 — Finalization
- [ ] مراجعة l10n شاملة (صفر hardcoded)
- [ ] فحص Dark/Light شاشة-شاشة
- [ ] Settings (4 tabs كاملة بما فيها Appearance)
- [ ] تنظيف + APK نهائي

---

# القرارات النهائية (تم اعتمادها من المستخدم — 2026-06-12)

| # | القرار | المعتمد |
|---|--------|---------|
| 1 | مكان المشروع | ✅ `E:\Orbit_Mobile_Native` (بدل G: غير الموصول — يمكن نقله لاحقًا) |
| 2 | اسم التطبيق / Package | ✅ الاسم الظاهر **Orbit** — الـ package: `com.orbit.mobile` |
| 3 | ملف العربي | ✅ `values-ar` بمحتوى مصري (يغطي كل الأجهزة العربية) |
| 4 | Realtime | ✅ **WebSocket من البداية** لغرف المشاريع (`ws/{project_id}?token=`) — قرار المستخدم بتجاوز التطابق الحرفي. ملاحظة: الإشعارات تبقى polling 30s (لا يوجد WS للإشعارات في الباك). يُحتفظ بـ refetch كـ fallback عند انقطاع الـ WS |
| 5 | BASE_URL | `http://10.0.2.2:8000/api/v1` افتراضيًا + قابل للتغيير |

# ملاحظات بيئة البناء (من تجربة سابقة على نفس الجهاز)
- JDK 22 فقط مثبت (`C:\Program Files\Java\jdk-22`) — Kotlin daemon قد يفشل بالاتصال → fallback non-daemon (بناء أبطأ، 1–6 دقائق)
- **مساحة C: حرجة (~1.2GB)** — يجب توجيه `GRADLE_USER_HOME` إلى قرص آخر (E: أو F:) قبل أول build وإلا سيفشل التنزيل
