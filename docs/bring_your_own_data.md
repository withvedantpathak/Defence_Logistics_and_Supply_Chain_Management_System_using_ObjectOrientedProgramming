# Bring your own data — loading an organisation's inventory

> **Team / user-facing document only.** This guide is **not** part of the research paper or the slides (decision of 2026-10-18). It is shipped with the user manual and the GitHub repo.

**Who this is for:** an organisation (a small or medium company, a depot, a relief group) that already keeps stock in a spreadsheet and wants to try DLSCMS on it. You do not need to write code. You need a spreadsheet program (Excel, LibreOffice, Google Sheets) that can save CSV files.

## 1. What the program needs

One **folder** holding these CSV files (first line of each file = the exact header shown; comma-separated, UTF-8).

| File | Needed? | Columns |
|---|---|---|
| `bases.csv` | **Yes** | `id,name,city,lat,lon,capacity_units` |
| `supplies.csv` | **Yes** | `id,name,category,unit_weight_kg` |
| `inventory.csv` | **Yes** | `base_id,supply_id,qty,reserved,reorder_level` |
| `vehicles.csv` | **Yes** | `id,type,home_base,current_base,capacity_kg,speed_kmph,status` |
| `meta.csv` | Recommended | `key,value` with rows `clock_hour,0` and `next_request,1` and `next_delivery,1` |
| `requests.csv`, `deliveries.csv` | Header only | Leave empty; the program fills them when you work |
| `scenario_requests.csv` | Optional | Only if you want to replay a request history with `--sim` |

A ready-made blank folder is `data/template/` and a small worked example (3 warehouses, 4 products, 3 vehicles) is `data/example_sme/`. **Copy one of them, then replace the contents.**

## 2. Step by step

1. **Copy the template.** Copy `data/template/` to a new folder, e.g. `C:\MyCompany\data`.
2. **List your locations in `bases.csv`.** One row per warehouse, depot or branch.
   - `id`: a short code without spaces (`PUN`, `WH-2`). `name`/`city`: free text.
   - `lat`, `lon`: decimal degrees (right-click the place in a maps app to copy them). They are used for distances.
   - `capacity_units`: the most units the location can hold in total.
3. **List your products in `supplies.csv`.** One row per product.
   - `id`: short code (`RICE-25`). `unit_weight_kg`: weight of one unit (used for loading vehicles).
   - `category` must be one of **MEDICAL, FOOD, FUEL, SPARES, SHELTER**. Map your own groups onto these (e.g. tools and parts → SPARES, tarpaulins and tents → SHELTER).
4. **Enter your stock in `inventory.csv`.** One row per location × product.
   - `qty`: units on hand now. `reserved`: **0** when you start. `reorder_level`: the minimum you want to keep; stock **above** it is what other locations may borrow, stock **below** it is shown as a shortage.
   - The total `qty` at a location must not exceed its `capacity_units`.
5. **List your vehicles in `vehicles.csv`.**
   - `type` is one of **Truck, FuelTruck, MedicalVan, Helicopter**. A Truck or Helicopter carries everything except fuel; a FuelTruck carries only fuel; a MedicalVan carries only medical and food items. Pick the nearest type for your fleet.
   - `home_base` and `current_base` are base ids (use the same one). `capacity_kg`, `speed_kmph`: whole numbers. `status` = **AVAILABLE**.
6. **Create `meta.csv`** (copy it from the template) and leave `requests.csv` / `deliveries.csv` with just their header line.
7. **Save each sheet as CSV (UTF-8).** In Excel: *Save As → CSV UTF-8 (Comma delimited)*. Check that the first line is the header exactly as above (no extra quotes, no blank columns).
8. **Check the folder** (needs Python 3):
   `python scripts/check_my_data.py C:\MyCompany\data`
   It lists every problem in plain English, with file name and line number, and says *Ready to load* when there are none. Warnings are advice only.
9. **Open it in the program.**
   - Windows: `run.bat --data C:\MyCompany\data` — or start normally and type `LOAD C:\MyCompany\data` (asks for confirmation).
   - Mac/Linux: `./run.sh --data /path/to/data`.
10. **Work as usual:** `MON` shows shortages, `REQ NEW …` raises a request, `REQ APPROVE`, `REQ FULFIL`, `DLV DISPATCH ALL`, `TICK 24`. Use `SAVE` to write your state back to the same folder (keep a copy of the original first).

## 3. Common problems

| Message from the program | Meaning and fix |
|---|---|
| `Inconsistent data … Unknown base id XXX` | A row in `inventory.csv` or `vehicles.csv` names a base that is not in `bases.csv`. Fix the spelling or add the base. |
| `Data format error in supplies.csv line 2: unknown supply category 'GRAIN'` | Use one of the five categories (step 3). |
| `Data format error in vehicles.csv line 2: capacity_kg must be a whole number` | Remove text, units or decimals from numeric cells. |
| Header problems | The first line of every file must match the header in section 1 exactly. Re-copy it from the template. |
| A product never gets delivered | No vehicle type in your fleet can carry its category (the checker warns about this). Add a suitable vehicle. |
| Requests stay open | Donors only give stock **above their reorder level** (CRITICAL requests may use half of it). If nobody has spare stock, or all suitable vehicles are on the road, the request waits. Lower reorder levels, add stock or add vehicles. |

## 4. Good to know

- Distances are straight-line (great-circle) between coordinates; there is no road network.
- Time is simulated: it moves only when you type `TICK <hours>`.
- Data is plain CSV, so you can open every file in a spreadsheet, and nothing leaves your computer.
- Keep a backup of your original CSV files before using `SAVE`.
- Importing directly from an Excel file, mapping your own column names, and importing your product groups automatically are on the future-updates list (`team/ROADMAP.md`).
