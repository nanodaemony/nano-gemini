
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a fork of ELADMIN, a Spring Boot-based backend management system. The project has been renamed to "Little Grid" (nano-gemini) with package name `com.naon.grid`.

- **Tech Stack**: Spring Boot 2.7.18, Spring Data JPA, Spring Security, JWT, Redis, MySQL
- **Java Version**: 1.8
- **Build Tool**: Maven

## Project Structure

The project uses a multi-module Maven architecture:

- **grid-common**: Common utilities, annotations, exceptions, configuration, and base classes
- **grid-logging**: System logging module (AOP-based operation/error logging)
- **grid-system**: Core application module and entry point (AppRun.java)
- **grid-tools**: Third-party integrations (email, S3 storage, alipay, local storage)
- **grid-generator**: Code generator module (generates CRUD code for backend/frontend)
- **grid-app**: Additional application module

## Key Configuration

- **Main Entry Point**: `grid-system/src/main/java/com/naon/grid/AppRun.java`
- **Application Config**: `grid-system/src/main/resources/config/application.yml`
- **Server Port**: 8000
- **Active Profile**: dev (needs application-dev.yml which isn't in the current files)
- **Database**: MySQL (needs application-dev.yml for JDBC config)
- **Redis**: Default 127.0.0.1:6379, configurable via REDIS_HOST, REDIS_PORT, REDIS_PWD env vars

## Building and Running

```bash
# Build the project (skips tests by default)
mvn clean package -DskipTests

# Or just compile
mvn clean compile

# Run the application (from grid-system module after building)
# The main class is com.naon.grid.AppRun
```

## Important Modules and Patterns

- **Code Generation**: grid-generator uses FreeMarker templates to generate Entity, DTO, Controller, Service, Mapper, Repository, and frontend code.
- **MapStruct**: Used for DTO &lt;-&gt; Entity mapping.
- **JPA Auditing**: Enabled via @EnableJpaAuditing.
- **Custom Annotations**: @Log, @AnonymousGetMapping, etc.
- **Exception Handling**: Centralized in grid-common exception package.

## Development Notes

- The project uses Alibaba Fastjson2 instead of Jackson for JSON.
- Druid is used as the database connection pool with SQL monitoring.
- Knife4j (Swagger UI) is available at /doc.html when the app is running.
