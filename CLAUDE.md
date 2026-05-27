# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**closetnangam-BE** is a Spring Boot 3.5 / Java 21 backend for a wardrobe management service. Users manage owned/wishlisted clothes, build outfits, and share them on a community feed. Key integrations include Kakao/Google/Naver OAuth2, Gemini AI for clothing analysis, Naver shopping API, and KMA weather API.

## Commands

```bash
# Start local infrastructure (MySQL on 3307, Redis on 6379)
docker-compose up -d

# Build (skip tests)
./gradlew build -x test

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.closetnangam.be.SomeTest"

# Run the application (local profile)
./gradlew bootRun
```

Tests require a `.env` file at project root with variables: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `GEMINI_API_KEY`, `WEATHER_API_KEY`. See `.env.example` for the full list.

## Architecture

### Domain Structure

The codebase follows a domain-driven package layout under `com.closetnangam.be`:

```
domain/
  user/        – User, UserAccount, UserProfile, UserStylePreference, UserTermsAgreement
  wardrobe/    – Wardrobe (1:1 with User; owns Clothes)
  clothes/     – Clothes, ClothesImage, ClothesStyleTag, ExternalProduct
  outfit/      – Outfit, OutfitItem (Outfit.java is currently an empty shell — @Entity not yet added)
  feed/        – FeedPost, FeedComment, FeedPostLike, FeedPostItem
  catalog/     – Style entity + enums for category/color/item-type/style codes
  ai/          – AI photo analysis (delegates to GeminiService)
  purchase/    – Purchase import flow
global/
  auth/        – JWT filter + provider, OAuth2 success handler + user service
  common/      – BaseEntity, ApiResponse<T>, GlobalExceptionHandler
  config/      – SecurityConfig, RedisConfig, SwaggerConfig
  external/    – GeminiService, NaverApiService, WeatherService
```

### Key Architectural Patterns

**Entity base class**: All entities that need audit timestamps extend `BaseEntity` (provides `createdAt`, `updatedAt` via JPA Auditing). `deletedAt` for soft-delete is added per-entity, not in `BaseEntity`.

**API response wrapper**: All endpoints return `ApiResponse<T>` — a record with `success`, `data`, `message`. Use `ApiResponse.ok(data)` for success and `ApiResponse.fail(message)` for errors.

**Exception handling**: Business errors use `IllegalArgumentException` (→ 400) and `IllegalStateException` (→ 409). `GlobalExceptionHandler` in `global.common.exception` handles these centrally. A `CustomException` class exists but is currently empty — prefer the standard Java exceptions until it is implemented.

**Security**: `SecurityConfig` wires Spring Security with OAuth2 login. JWT is issued by `OAuth2SuccessHandler` after successful OAuth2 login. `JwtAuthenticationFilter` validates tokens on subsequent requests. Public endpoints include `/oauth2/**`, `/login/**`, `/api/categories/**`, and Swagger UI paths. **User identity is not yet injected via `SecurityContextHolder`** in most controllers — userId is currently taken from path variables with a TODO to add ownership verification once JWT integration is complete.

**Entity creation**: Use Lombok `@Builder` with `private` constructor. Direct `new` is not used. `@NoArgsConstructor(access = AccessLevel.PROTECTED)` is required on all entities.

**User domain split**: `User` holds core identity (nickname, email, gender, birthDate). `UserAccount` stores OAuth provider info. `UserProfile` and `UserStylePreference` are separate entities for extensibility.

**Clothes ownership**: `Clothes.sourceType` (enum `SourceType`: `OWNED`, `WISHLIST`) distinguishes owned vs. wishlisted items. Both live in the same table. `Wardrobe` is the aggregate root — always look up wardrobe via `userId` before accessing clothes.

**Outfit**: `Outfit.java` is intentionally an empty shell (no `@Entity` yet). Any domain that would reference `Outfit` must store `outfitId` as a plain `Long` with a TODO comment until the entity is fully implemented.

## Conventions

### Naming
- Classes: `PascalCase`
- Variables/methods: `camelCase`
- URIs: lowercase `kebab-case`, prefixed with `/api/v1` (note: some existing endpoints use `/api` without `/v1` — follow `/api/v1` for new work)
- DB columns: `snake_case`
- DTOs: `XxxCreateRequest`, `XxxUpdateRequest`, `XxxResponse`

### Code Style
- Always use braces `{}` for `if`/`for`/`while` even for single-line bodies
- Comments only when necessary, indented to match surrounding code
- Builder pattern for object creation; avoid `new EntityName(...)` directly in service code

### API Documentation
- Swagger (`springdoc-openapi`) is used — annotate controllers with `@Tag` and methods with `@Operation`
- Swagger UI is available at `http://localhost:8080/swagger-ui.html` when running locally

### Testing
- Test profile (`application-test.yml`) connects to the CI MySQL service; use `SPRING_PROFILES_ACTIVE=test`
- AWS S3 auto-configuration is excluded in `local` and `test` profiles
