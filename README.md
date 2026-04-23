#  Social Guardrail System (Spring Boot + Redis)

##  Overview

This project implements a **backend guardrail system** for a social media platform.
It prevents spam, controls bot behavior, and ensures system stability under high concurrency.

### Tech Roles:

* **PostgreSQL** → Source of truth (stores posts/comments)
* **Redis** → Real-time guardrails (limits, cooldowns, notifications)

---

#  Architecture

```text
Client → Spring Boot API → Redis (gatekeeper) → PostgreSQL (DB)
```

###  Core Idea

> Redis decides **whether an action is allowed**
> PostgreSQL stores **only valid actions**

---

#  API Endpoints

##  Post APIs

```text
POST   http://localhost:8080/api/posts
→ Create a new post (User/Bot)

GET    http://localhost:8080/api/posts
→ Fetch all posts (used to get postId)
```

---

## Comment APIs

```text
POST   http://localhost:8080/api/posts/{postId}/comments
→ Add comment (BOT or USER)

GET    http://localhost:8080/api/posts/{postId}/virality-score
→ Get virality score of a post
```

---

# ⚙ Features Implemented

##  Phase 1: Basic Backend

* Post & Comment APIs
* PostgreSQL integration using Spring Data JPA

---

##  Phase 2: Guardrails (Spam Control)

Redis is used to restrict bot behavior and prevent abuse.

---

###  1. Horizontal Cap (Max 100 bot comments)

Redis key:

```text
post:{postId}:bot_reply_count
```

✔ Enforced using **atomic Lua script**

---

###  2. Bot Cooldown (10 minutes)

```text
cooldown:bot_{botId}:user_{userId}
```

✔ Prevents repeated bot spam on same user

---

###  3. Depth Limit

```text
max depth = 20
```

✔ Prevents infinite nesting of comments

---

###  Thread Safety in Phase 2

Ensuring thread safety is critical because multiple bot requests can hit the system **simultaneously**.

---

###  Problem

If we use a naive approach:

```text
INCREMENT → check → allow
```

Under concurrency:

* Multiple threads may read the same value
* Limits can be exceeded (e.g., 101, 102…)
* Leads to **race conditions**

---

###  Solution (Thread-Safe Design)

We ensure thread safety using **Redis atomic operations**.

---

###  1. Atomic Counters (INCREMENT)

```text
redisTemplate.opsForValue().increment(key)
```

✔ Redis guarantees that `INCREMENT` is **atomic**
✔ No two threads can modify the value at the same time

---

###  Limitation

Using only `INCREMENT` + check:

```text
increment → if > limit → rollback
```

Still has a small race window under heavy concurrency

---

###  2. Final Fix (Atomic Lua Script)

To eliminate race conditions completely, we use:

```lua
local current = redis.call('GET', KEYS[1])
if not current then current = 0 else current = tonumber(current) end
if current >= 100 then return -1 end
return redis.call('INCR', KEYS[1])
```

---

###  Why Lua Script is Thread-Safe

* Redis executes Lua scripts as a **single atomic operation**
* No interleaving between threads
* Guarantees:

```text
Max bot comments = 100 (strict)
```

---

###  Final Guarantee

| Property          | Result       |
| ----------------- | ------------ |
| Race Condition    | ❌ Eliminated |
| Over-limit writes | ❌ Impossible |
| Thread Safety     | ✅ Guaranteed |

---

###  Key Insight

> Thread safety is achieved not by Java synchronization,
> but by **delegating concurrency control to Redis**


## Phase 3: Notification System (Smart Batching)

### 🔹 Problem

Avoid spamming users with too many notifications

---

### 🔹 Solution

#### Redis Throttler

* First event → instant notification
* Next events → stored in Redis

```text
user:{id}:notif_cooldown
user:{id}:pending_notifs
notif:active_users
```

---

#### Scheduler (CRON)

* Runs periodically (5 min prod / 10 sec test)
* Sends summarized notification:

```text
"Bot X and N others interacted with your post"
```

---

###  Note (Phase 3 Testing)

To test notification system independently:

* Bot cooldown may be temporarily disabled

---

##  Phase 4: Concurrency Safety (Critical)

###  Problem

Multiple concurrent requests can break system limits

---

### ❌ Naive Approach

```text
INCREMENT → check → decrement
```

→ Causes race condition

---

### ✅ Final Solution

Used **Redis Lua Script (atomic execution)**:

```lua
local current = redis.call('GET', KEYS[1])
if not current then current = 0 else current = tonumber(current) end
if current >= 100 then return -1 end
return redis.call('INCR', KEYS[1])
```

---

###  Why This Works

* Redis Lua scripts are **atomic**
* No race conditions possible
* Guarantees:

```text
Max bot comments = 100
```

---

# 🧪 Testing

## Phase 3 Testing

✔ Immediate notification
✔ Queueing
✔ Scheduler batching
✔ Redis cleanup

---

##  Phase 4 Testing (IMPORTANT)

### 🔹 Setup

1. Clear Redis:

```bash
flushall
```

2. Clear Database:

```sql
DELETE FROM comment WHERE post_id = X;
```

3. Run load test using Postman Runner:

```text
Iterations: 200
Delay: 0 ms
```

---

### ⚠️ Important Testing Note

For **accurate concurrency testing**, temporarily disable:

```java
// if (isCooldown(botId, userId))
// setCooldown(botId, userId)
// notificationService.handleNotification(...)
```
Reason:

* These features block requests early
* They interfere with concurrency validation

---

###  Expected Result

| System   | Value |
| -------- | ----- |
| Redis    | 100   |
| Database | 100   |

---

### ✅ Conclusion

* No race conditions
* No extra database writes
* Strong consistency guaranteed

---

#  Redis Key Design

| Purpose               | Key                         |
| --------------------- | --------------------------- |
| Bot limit             | post:{id}:bot_reply_count   |
| Virality              | post:{id}:virality_score    |
| Bot cooldown          | cooldown:bot_{id}:user_{id} |
| Notification cooldown | user:{id}:notif_cooldown    |
| Notification queue    | user:{id}:pending_notifs    |
| Active users          | notif:active_users          |

---

# ⚡ Key Learnings

* Redis acts as a **gatekeeper**
* Atomic operations prevent race conditions
* DB writes only after validation
* Stateless systems scale better

---

# 🚀 Tech Stack

* Java 17
* Spring Boot
* PostgreSQL
* Redis (Docker)
* Postman

---

# 🎯 Final Outcome

✔ Fully stateless backend
✔ Race-condition-free system
✔ Scalable guardrail architecture
✔ Production-level concurrency handling

---

# 👨‍💻 Author

Mayank Rajput
