# Promo Finder

Watches online stores for a product and tells you (email, Telegram or SMS) when it hits your price.
The searching is done by **Claude Code CLI** (`claude -p`), so it runs on your Claude subscription
instead of API tokens.

```
 Scheduler (every tick) ──► WatchChecker ──► claude -p  (WebSearch + WebFetch only)
                                 │                 │
                                 │          JSON offers
                                 ▼                 │
                         validate in Java ◄────────┘   price ≤ target, in stock, BRL, URL on your store domains
                                 │
                       new offer? ──► Email / Telegram / SMS (Twilio)
```

## Requirements

- Java 25 (Maven wrapper included, no Maven install needed)
- Claude Code installed and logged in: run `claude` once in a terminal and sign in

## Run

```bash
./mvnw spring-boot:run
```

Open http://localhost:8080. A "Get started" checklist walks you through it:

1. **Stores**: add the shops you trust, or click one of the popular-store suggestions.
   Offers on other domains are discarded. With no stores, Claude searches any reputable Brazilian shop.
2. **Watches**: click **New watch** (or press `N`): product, requirements, max price, how often to check,
   and the channels to notify. Leave "Search now after saving" on to get a first result right away.
3. **Settings**: see which notification channels are ready, send a test, and check that the Claude CLI works.

Each watch card shows the best price now vs your target, how far off it is, a 7-day price sparkline and
when the next check runs. While Claude is searching the card says so live (pushed by the server), and a
deal turns the card green with a "Go to store" button. Click a watch's name for the full price history,
prices per store and every check with its cost. Deleting can be undone for 5 minutes.

The UI is in English and Brazilian Portuguese (picked from your browser, switch with EN/PT in the top bar).

You are notified once per offer URL; you are notified again for the same URL only if the price drops further.

## Notifications

Copy `application-local.properties.example` to `application-local.properties` (gitignored) and fill
in the channels you want. Environment variables work too, e.g. `PROMO_NOTIFICATION_TELEGRAM_BOT_TOKEN`.

| Channel  | What you need |
|----------|---------------|
| Telegram | Create a bot with @BotFather, send it any message, read your `chat.id` from `https://api.telegram.org/bot<TOKEN>/getUpdates` |
| Email    | Any SMTP server. For Gmail, use an [app password](https://myaccount.google.com/apppasswords) |
| SMS      | A Twilio account (SID, auth token, a Twilio "from" number) |

The page shows which channels are configured and has a **Send test** button for each.

## Configuration

`src/main/resources/application.properties`:

| Property | Default | |
|---|---|---|
| `promo.claude.command` | `claude` | Path to the CLI if it is not on `PATH` |
| `promo.claude.model` | `haiku` | Cheapest on usage; `sonnet` or `opus` read store pages more carefully |
| `promo.claude.timeout` | `10m` | Max time for one search |
| `promo.scheduler.tick` | `1m` | How often due watches are looked for; each watch has its own interval |

## Notes

- Each check costs roughly what a short Claude Code session does (the log prints the equivalent USD cost
  per check). Many watches checked every hour can eat into your subscription limits; use longer intervals
  or `haiku` if that happens.
- Claude only gets the `WebSearch` and `WebFetch` tools and runs in an empty folder (`data/claude-work`).
  It cannot run commands or touch files. Its answer is re-validated in Java before any alert goes out.
- Data lives in an H2 file database in `data/`. The H2 console is at http://localhost:8080/h2-console
  (JDBC URL `jdbc:h2:file:./data/promofinder`, user `sa`, no password).
- The app finds and alerts; it does not buy anything for you.

## API

| Method | Path | |
|---|---|---|
| GET/POST | `/api/stores` | list / add `{name, domain, enabled}` |
| PUT/DELETE | `/api/stores/{id}` | |
| GET/POST | `/api/watches` | list / add `{product, details, maxPrice, intervalMinutes, active, channels}` |
| PUT/DELETE | `/api/watches/{id}` | |
| POST | `/api/watches/{id}/check` | run a check now (async) |
| GET | `/api/watches/{id}/offers` | offers found within target |
| GET | `/api/notifications/channels` | which channels are configured |
| POST | `/api/notifications/test/{EMAIL\|TELEGRAM\|SMS}` | send a test message |

The web UI itself uses server-rendered Thymeleaf fragments under `/ui/**` (htmx) and live updates from `/events` (Server-Sent Events).
