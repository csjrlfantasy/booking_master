# Banner Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build configurable homepage banner carousels for the admin console, customer web page, and Android customer app.

**Architecture:** Add a `banner` table and expose public `/api/banners` plus protected `/api/admin/banners` endpoints. The admin frontend manages banner metadata and uploaded images, while both customer clients consume the same public API with a local fallback.

**Tech Stack:** Spring Boot, MyBatis-Plus, MySQL/H2-compatible schema initializer, React/Vite, React Native, Axios, Maven/Jest/TypeScript verification.

---

### Task 1: Backend Banner API

**Files:**
- Create: `booking-api/src/main/java/com/ticket/app/entity/Banner.java`
- Create: `booking-api/src/main/java/com/ticket/app/repository/banner/BannerRepository.java`
- Create: `booking-api/src/main/resources/mapper/BannerMapper.xml`
- Create: `booking-api/src/main/java/com/ticket/app/config/BannerTableInitializer.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/banner/BannerController.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/banner/dto/BannerResponse.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/AdminBannerController.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminBannerRequest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/config/interceptor/JwtAuthInterceptor.java`
- Modify: `booking-api/src/main/java/com/ticket/app/config/WebMvcConfig.java`
- Test: `booking-api/src/test/java/com/ticket/app/controller/banner/BannerControllerTest.java`
- Test: `booking-api/src/test/java/com/ticket/app/controller/admin/AdminBannerControllerTest.java`

- [ ] **Step 1: Write failing tests**

Add tests that expect public banner listing to return only repository-visible banners and admin creation to persist title, image URL, platform, link data, sort order, and status.

- [ ] **Step 2: Run tests to verify RED**

Run: `D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd -pl booking-api -Dtest=BannerControllerTest,AdminBannerControllerTest test`

Expected: compilation failure because banner controller classes do not exist yet.

- [ ] **Step 3: Implement minimal backend**

Create the entity, mapper, schema initializer, public controller, and admin CRUD controller. Default public listing uses `platform=web`, `status=1`, and active date windows.

- [ ] **Step 4: Run tests to verify GREEN**

Run the same Maven command and ensure both banner tests pass.

### Task 2: Admin Banner Management

**Files:**
- Modify: `frontend/src/api/admin.ts`
- Create: `frontend/src/pages/banner/BannerListPage.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Add admin API wrappers**

Add `AdminBanner`, `AdminBannerRequest`, `getAdminBanners`, `createAdminBanner`, `updateAdminBanner`, `updateAdminBannerStatus`, and `deleteAdminBanner`.

- [ ] **Step 2: Build management page**

Add a page with banner list, image preview, upload button, title, platform, link type, link target, sort order, status, and create/update/delete actions.

- [ ] **Step 3: Wire route and sidebar**

Route `/banners` to the page and add a sidebar item named `Banner管理`.

### Task 3: Customer Web Carousel

**Files:**
- Modify: `frontend/src/api/customer.ts`
- Modify: `frontend/src/pages/shop/ShopPage.tsx`

- [ ] **Step 1: Add public banner API**

Expose `getShopBanners(platform = 'web')`.

- [ ] **Step 2: Replace hero with configurable carousel**

Load banners with tickets/categories, rotate every 4 seconds, show image/title/dots, and handle link types `ticket`, `category`, `url`, and `none`.

### Task 4: Android Homepage Carousel

**Files:**
- Modify: `booking-app/src/services/api/ticketApi.ts`
- Modify: `booking-app/src/components/common/Banner.tsx`
- Modify: `booking-app/src/screens/home/HomeScreen.tsx`

- [ ] **Step 1: Add mobile banner API**

Expose `ticketApi.getBanners('app')`.

- [ ] **Step 2: Render real images**

Update `Banner` to use React Native `Image` when `imageUrl` exists and keep a styled fallback when it does not.

- [ ] **Step 3: Load banners on homepage**

Fetch banners in `loadData`, map link fields to `action`, and fall back to mock banners when the API is unavailable.

### Task 5: Verification

**Files:**
- No new files.

- [ ] **Step 1: Backend tests**

Run: `D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd -pl booking-api test`

- [ ] **Step 2: Frontend build**

Run: `npm.cmd run build` from `frontend`.

- [ ] **Step 3: Mobile type/build smoke**

Run a lightweight React Native type or test command available in `booking-app`; if the project lacks a stable command, record the limitation and verify changed TypeScript syntax by the closest available script.
