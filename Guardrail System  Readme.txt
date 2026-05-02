# 🛡️ Social Guardrail System — Backend

## 🚀 Overview

This project implements a backend system that simulates a social media platform with intelligent guardrails. It ensures safe and controlled interactions between users and bots using Redis for real-time constraints and PostgreSQL as the source of truth.

---

## 🧠 Approach

The system is designed with a **dual-layer architecture**:

* **PostgreSQL** → stores persistent data (posts, comments)
* **Redis** → handles real-time constraints (counters, cooldowns, locks)

All critical checks are performed in Redis before committing any data to the database.

---

## 🔐 Thread Safety & Atomic Locks (Phase 2)

To handle concurrency and prevent race conditions (especially during high-load scenarios like 200 simultaneous bot requests), Redis was used as the **atomic gatekeeper**.

### ❌ Problem

A naive approach like:

* check count
* then increment

is **not thread-safe**, because multiple threads can pass the check simultaneously.

---

### ✅ Solution: Atomic Increment Pattern

We used Redis’ atomic `INCR` operation:

```java
Long count = redisTemplate.opsForValue().increment(key);

if (count > 100) {
    redisTemplate.opsForValue().decrement(key); // rollback
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bot limit reached");
}
```

---

### 🧠 Why this is thread-safe

* Redis operations like `INCR` are **atomic**
* Each request gets a **unique incremented value**
* No two threads can read the same value
* Ensures strict enforcement of limits (e.g., max 100 bot comments)

---

### 🎯 Guarantee

Under concurrent load:

* Even with 200 simultaneous requests
* Maximum allowed bot comments = **100 (never exceeds)**

---

## ⚙️ Other Guardrails Implemented

### 🔹 Horizontal Cap (Bot Limit)

* Max 100 bot replies per post
* Enforced using Redis atomic counters

### 🔹 Vertical Cap (Depth Limit)

* Comment depth restricted to ≤ 20
* Prevents infinite nesting

### 🔹 Cooldown System

* Key: `cooldown:bot_{botId}:user_{userId}`
* Prevents repeated bot interaction within 10 minutes
* Implemented using Redis TTL

---

## 🧱 Data Integrity

* All validations are performed **before database writes**
* PostgreSQL is only updated if Redis constraints allow
* Ensures consistency between system rules and stored data

---

## 🧠 Stateless Design

* No in-memory storage (no HashMap, no static variables)
* All state is maintained in Redis
* Ensures horizontal scalability

---

## 🧪 Concurrency Handling

The system is designed to pass:

* High concurrency tests (200+ requests)
* Race condition scenarios
* Ensures strict enforcement of all constraints

---

## ✅ Summary

This backend ensures:

* Thread-safe operations using Redis atomic primitives
* Real-time constraint enforcement
* Clean separation of storage and logic layers
* Production-ready system design principles

---
