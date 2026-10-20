# Team Roadmap — future updates (team-only)

> Private working notes for Vedant, Dhairya, Kanishak (and Claude). **Not part of the course submission.**
> The course MVP is frozen at the scope in `docs/requirements.md`. Everything below is *after* submission, or an optional extra if time allows.
> How to use: add ideas at the bottom of "Inbox"; when the team agrees, move an item into a tier with an owner and a size (S = under half a day, M = 1–2 days, L = a week or more).

Last updated: 2026-10-18

## 1. Direction
Today DLSCMS is a defence-flavoured, single-user terminal simulator. The same core (bases, stock, requests, vehicles, deliveries, strategies) is also a decent skeleton for **inventory and logistics management for a small or medium company** with several warehouses or shops. Changing the vocabulary is easy (base → warehouse, request → replenishment order); the real work is the features such a company would expect and a friendlier way to use them.

## 2. Tier 1 — make it genuinely useful for an SME (next)
| # | Idea | Why a small/medium company wants it | Size |
|---|---|---|---|
| 1 | **Persistent database** (SQLite first, PostgreSQL later) behind the existing `CsvRepository` idea | CSV files are fine for a demo, not for several people editing; gives transactions and history | M |
| 2 | **Audit log + undo trail** (who did what, when) | Accountability, dispute resolution, debugging stock mismatches | S |
| 3 | **Users and roles** (viewer, storekeeper, approver, admin) | Separates who may raise, approve and dispatch | M |
| 4 | **Suppliers and purchase orders** (buy from outside, not only move between own bases) | Real companies replenish mostly from vendors | M |
| 5 | **Automatic reorder suggestions** (reorder point + safety stock + lead time per supply) | Turns "SHORT" flags into a ready-to-approve shopping list | M |
| 6 | **Batch / lot numbers and expiry (FEFO)** | Food, medical and chemical stock expires; oldest-expiry-first picking | M |
| 7 | **Stocktake / cycle counts and adjustments with reasons** (damage, theft, return, found) | Physical count rarely matches the system | S |
| 8 | **Costing and valuation** (unit cost, stock value per base, margin) | Owners think in money, not units | M |
| 9 | **Import / export** (Excel/CSV templates, barcode/SKU columns) | Onboarding a real catalogue in minutes | S |
| 10 | **Alerts** (email / WhatsApp / Slack on shortage, delay, expiry) | Nobody watches a terminal all day | S–M |

## 3. Tier 2 — analytics and smarter decisions
- **ABC / XYZ classification** of items by value and demand variability (what to count weekly vs yearly).
- **Demand forecasting** from past requests (moving average first, then seasonal models); feeds reorder suggestions.
- **Stock-out and overstock risk scores** per base; "days of cover" metric.
- **Route and load planning**: multi-stop trips, vehicle load consolidation, delivery time windows, fuel/cost per trip. (The simulator's strategies already give us a comparison framework.)
- **What-if simulator** for managers: "what if supplier X is 3 days late?" using the existing `SimClock`/experiment code.
- **Carrier / third-party vehicle** support (hired trucks with cost per km).

## 4. Tier 3 — AI report generator and interface layer (the idea you suggested)
### 4.1 AI report generator ("morning brief")
- Reads the KPIs and tables the system already produces (`ReportService`: stock, shortages, deliveries, KPIs) and writes a short plain-language report: *what changed, what is at risk, what to do today*, with numbers cited from the data.
- Output as Markdown/PDF/e-mail; scheduled daily or weekly; "explain this KPI" on demand.
- **Design rule:** the language model only *writes about* numbers the Java engine computed and passes in; it never invents figures. Each sentence links back to the table row it came from. A template-based fallback (no AI, fully offline) produces the same report with fixed wording, so the product still works without an API key.
- Cost control: send aggregated tables, not raw rows; cache the report per day.

### 4.2 AI interface layer (natural-language front end)
- A chat/command box on top of the terminal: *"Pune is short of medical kits, send 200 urgent from the nearest base"* → the layer turns it into the exact commands (`REQ NEW PUN MED-KIT 200 URGENT`, `REQ APPROVE …`, `REQ FULFIL …`) and **shows them for confirmation** before anything runs.
- **Safety principles:** (a) the AI proposes, the deterministic engine executes; (b) every proposed action needs a human Y/N unless the user turns on a trusted auto-mode for read-only queries; (c) role permissions still apply; (d) every action lands in the audit log; (e) nothing outside the company's own data is sent to the model, and the user can switch the layer off.
- Needs a stable **service API** first (see 4.3), so the AI layer and a future web UI call the same functions.

### 4.3 Interface layer (non-AI)
- **Service API:** a small local REST/JSON API exposing the `LogisticsManager` facade (the facade already hides the services, so this is mostly wiring). Enables a web dashboard, mobile scanner app, spreadsheets and the AI layer.
- **Web dashboard:** the MON screen as a browser page with filters, charts and a request form (keep the terminal as the power-user view).
- **Barcode / QR scanning** from a phone for receiving, picking and stocktake.
- **Integrations:** accounting (Tally / Zoho / QuickBooks export), e-commerce (Shopify, WooCommerce) stock sync.

## 5. Suggested order after submission
1. Audit log (#2) and import/export (#9) — cheap, immediately useful.
2. SQLite persistence (#1) and service API (4.3).
3. Reorder suggestions (#5) and suppliers/POs (#4).
4. AI report generator (4.1) using the API — safe, read-only, visible value.
5. Roles (#3), then the AI command layer (4.2) on top of roles and audit.
6. Web dashboard and barcode scanning.

## 6. Risks and open questions
- Scope creep: none of this goes into the course paper except a short "Future work" section.
- Cost: AI features need an API key and per-use cost; keep the offline fallback and make AI optional.
- Privacy: company stock and price data is commercially sensitive; decide what may leave the machine.
- Open: pick one pilot business (e.g. a restaurant group or small retailer) to validate features before building more. *(owner: team)*

## 7. Inbox — drop new ideas here
- *(add date, name, idea)*

## 8. Done / decided
- 2026-10-18: Roadmap section created.
