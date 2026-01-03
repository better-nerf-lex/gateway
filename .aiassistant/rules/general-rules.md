---
apply: always
---

---
alwaysApply: true
---
# Lexai Backend Development Standards

## Project overview
This folder is a system based on **Java 21 spring based reactive backend and React apps** backed by PostgreSQL and Anthropic API.
It contains 4 folders, ./client-front, ./devtools, ./infra and ./gateway.
Code located in '/devtools' folder is used only in local developmment and is spring gateway app.
Code located in ./gateway is our main backend entry point.
Code located in ./client-front is bun and vite based react frontend using feature sliced design.
We are located in gateway now.

## Antigravity Agent Guidelines

As a coding agent, you MUST follow these interaction principles:

### Communication Style
1. **Be concise and direct** - no unnecessary explanations or filler text
2. **Act as an expert** - never use disclaimers like "I'm not an expert" or apologetic language
3. **Focus on the problem** - extract the key issue and solve it directly
4. **No apologies** - avoid "sorry", "apologize", or "regret" in any context
5. **Be specific** - provide concrete solutions with code examples

### Problem Solving Approach
1. **Decompose complex problems** - break down into clear, logical steps
2. **Rething alternatives** - approach multiple solutions when trade-offs exist and select the best with explaination
3. **Ask for clarification** - only when the requirement is genuinely ambiguous
4. **Reference sources** - cite specific files, patterns, or documentation when explaining
5. **Learn from mistakes** - acknowledge and correct any previous errors without apology

### Code Generation Rules
1. **Write production-ready code** - no placeholders or pseudo-code
2. **Follow existing patterns** - observe and replicate patterns already in the codebase
3. **Complete implementations** - provide full, working solutions
4. **Include necessary imports** - ensure all dependencies are properly imported, use import before class declaration. Never import in class.
5. **Add type hints** - all new code must be fully typed
6. **Avoid comments** - no need to explain self explainatory code, use comments only when necessary.

## Development Environment

### Prerequisites
The development stack is already running when using Antigravity. Do NOT build or start containers.
Access frontend on localhost:8080 and backend on localhost:8080/api

## Coding Standards
### 1. Type Safety & Data Types

#### Requirements
- **Functional first** as we use reaactive, keep functional approach on the backend
- **Use concrete types** - never use `Any` without justification
- **Abuse reusable approach** we want feature sliced design based on hooks in the frontend
- **Create own components** we should always create own component wrappers around library components, never use plain.

#### Example
```java
# Good
    public Mono<RefreshTokenResponse> refreshToken(String token) {
        return jwtService.validateToken(token)
                .flatMap(userRepository::findByUsername)
                .filter(User::isEnabled)
                .map(user -> {
                    String newToken = jwtService.generateToken(user);
                    return RefreshTokenResponse.builder()
                            .token(newToken)
                            .build();
                });
    }

# Bad
    public Mono<RefreshTokenResponse> refreshToken(String token) {
        boolean isValid = jwtService.validateToken(token);
        return isValid;
    }
```

#### Database Best Practices
- Add indexes on frequently queried fields
- Use `bulk_create()` / `bulk_update()` for multiple records
- Use `only()` / `defer()` to limit field selection
- Always paginate list endpoints


#### Single Responsibility
- **Controller**: Handle HTTP requests/responses only
- **Models**: Data structure and simple validations
- **Services**: Complex business logic
- **Mappers**: Data transformation only

#### Open/Closed Principle
- Use abstract base classes for extensibility
- Create mixins for reusable functionality

#### DRY (Don't Repeat Yourself)
- Extract common logic to utils
- Use static functions on backend and hooks/features on frontend for shared behavior

## Code Review Checklist

Before submitting code for review, ensure:

- [ ] **Type hints** on all functions and methods
- [ ] **Tests** written for new functionality (min 80% coverage)
- [ ] **No business logic in controllers** - moved to services
- [ ] **Exceptions handled** appropriately at each layer
- [ ] **Database queries optimized** (no N+1 queries)
- [ ] **Constants extracted**
- [ ] **DRY principle** followed - no code duplication
