---
globs: backend/src/main/java/**/*.java
description: Apply this rule when refactoring Java classes with boilerplate
  getters/setters/constructors to use Lombok annotations instead.
alwaysApply: false
---

Use Lombok annotations (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder) to reduce boilerplate code in Java classes. Replace manual getters/setters with Lombok annotations where appropriate.