const pptxgen = require("pptxgenjs");
const { applyTheme } = require("/root/.claude/skills/synced/1cb0a675-83c8-49a5-8ac9-141e174b8e06_fb37c761-d272-4f08-948e-43b70233cd23/pptx/scripts/apply_theme.js");
const R = "..";
const REPO = "https://github.com/withvedantpathak/defence_logistics_and_supply_chain_management_system_using_objectorientedprogramming";
const THEME = { name: "DLSCMS", headFontFace: "Cambria", bodyFontFace: "Calibri",
  colors: { dk1: "14181C", lt1: "FFFFFF", dk2: "2F3B2F", lt2: "EEF1EA", accent1: "E0A400", accent2: "5B7B4A", accent3: "3E6A8A", accent4: "B5543C", accent5: "7A8590", accent6: "C9D3BE", hlink: "3E6A8A", folHlink: "7A8590" } };
const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; pres.theme = { headFontFace: "Cambria", bodyFontFace: "Calibri" };
pres.title = "Defence Logistics and Supply Chain Management System";
pres.author = "Vedant Pathak, Dhairya Shah, Kanishak Thakran";
const C = pres.SchemeColor;
const INK="14181C", OLIVE="2F3B2F", AMBER="E0A400", MOSS="5B7B4A", PAPER="EEF1EA", GREY="5A646E";

pres.defineSlideMaster({ title: "DARK", background: { color: INK },
  objects: [], slideNumber: { x: 9.2, y: 5.25, w: 0.5, h: 0.3, fontSize: 10, color: "8A949E" },
  slideNumber2: undefined });
pres.defineSlideMaster({ title: "LIGHT", background: { color: "FFFFFF" },
  objects: [],
  slideNumber: { x: 9.2, y: 5.25, w: 0.5, h: 0.3, fontSize: 10, color: GREY },
  placeholders: [{ placeholder: { options: { name: "title", type: "title", x: 0.5, y: 0.3, w: 9, h: 0.75, fontFace: "Cambria", fontSize: 30, bold: true, color: INK, margin: 0, valign: "middle" }, text: "" } }] });
let sec = 0;
function light(title, notes) {
  const s = pres.addSlide({ masterName: "LIGHT" });
  s.addText(title, { x: 0.5, y: 0.3, w: 9, h: 0.75, fontFace: "Cambria", fontSize: 30, bold: true, color: INK, margin: 0, valign: "middle", isTextBox: true });
  s.addNotes(notes); return s;
}
function footer(s) { s.addText("DLSCMS  |  NMIMS MPSTME  |  27/29 Oct 2026", { x: 0.5, y: 5.25, w: 6, h: 0.3, fontSize: 10, color: GREY, margin: 0, isTextBox: true }); }
function card(s, x, y, w, h, head, body, accent) {
  s.addShape(pres.ShapeType.rect, { x, y, w, h, fill: { color: PAPER }, line: { color: "D5DACC", width: 0.75 } });
  s.addText(head, { x: x + 0.15, y: y + 0.1, w: w - 0.3, h: 0.4, fontSize: 16, bold: true, color: accent || OLIVE, margin: 0, isTextBox: true, fontFace: "Cambria" });
  s.addText(body, { x: x + 0.15, y: y + 0.52, w: w - 0.3, h: h - 0.6, fontSize: 16, color: INK, margin: 0, valign: "top", paraSpaceAfter: 6, isTextBox: true });
}

// 1 Title
let s = pres.addSlide({ masterName: "DARK" });
s.addText("DLSCMS", { x: 0.6, y: 0.7, w: 4, h: 0.4, fontSize: 16, bold: true, color: AMBER, charSpacing: 6, margin: 0, isTextBox: true });
s.addText("Defence Logistics and Supply Chain Management System", { x: 0.6, y: 1.2, w: 8.2, h: 1.6, fontSize: 38, bold: true, fontFace: "Cambria", color: "FFFFFF", margin: 0, valign: "top", isTextBox: true });
s.addText("An object-oriented Java model that moves stock between bases, and a study of how well different dispatch rules work", { x: 0.6, y: 2.95, w: 7.6, h: 0.8, fontSize: 18, color: "C9D3BE", margin: 0, isTextBox: true });
s.addText([{ text: "Vedant Pathak (R045)   Dhairya Shah (R057)   Kanishak Thakran (R046)", options: { breakLine: true } },
  { text: "Faculty: Dr. Sakshi Ranjan   |   SVKM's NMIMS MPSTME, Mumbai" }], { x: 0.6, y: 4.2, w: 8.8, h: 0.8, fontSize: 14, color: "FFFFFF", margin: 0, isTextBox: true });
s.addNotes("Introduce the team and faculty. One sentence: we built a Java system that models moving supplies between defence bases, and we used it to test which dispatch rules work best. 15 minutes: about 8 on slides, 5 on live demo, 2 for questions.");

// 2 Problem
s = light("The problem: right stock, right place, in time",
 "Context: bases hold stock (fuel, food, medical, ammunition, spares). Requests appear at different priorities. Someone has to decide which base donates, which vehicle carries it. Doing that by hand is slow and inconsistent. Our goals: model it with clean OOP, make the decision rules swappable, and measure them.");
card(s, 0.5, 1.25, 2.85, 3.5, "The situation", "Bases hold stock of fuel, food, medical, ammunition and spares.\n\nRequests arrive with a priority from ROUTINE to CRITICAL.\n\nA donor base and a vehicle must be chosen every time.", OLIVE);
card(s, 3.575, 1.25, 2.85, 3.5, "What we built", "A Java system with 16 terminal commands: bases, inventory, requests, fleet, deliveries, reports.\n\nA simulated clock that moves hour by hour.\n\nSave and load to CSV files.", MOSS);
card(s, 6.65, 1.25, 2.85, 3.5, "What we asked", "Does the choice of donor base matter?\n\nDoes the choice of vehicle matter?\n\nHow does coordination compare with a random baseline, and does it scale?", "3E6A8A");
footer(s);

// 3 Architecture
s = light("Architecture: nine packages, one facade",
 "Walk top to bottom. The terminal UI only talks to LogisticsManager, the facade. Services hold the rules. Strategies are swappable. Model holds the data objects. Persistence reads and writes CSV. The sim package replays scenarios for the experiments. 66 types, about 5,700 lines of main code.");
const layers = [["ui", "Bloomberg-style terminal: 16 function codes", "3E6A8A"], ["service", "LogisticsManager (Facade), Inventory, Request, Fleet, Delivery, Report", MOSS],
  ["strategy", "Donor and vehicle selection rules, swappable at run time", AMBER], ["model + exception", "Base, Supply, Request, Delivery, Vehicle hierarchy; checked exceptions", OLIVE],
  ["persistence + util + sim", "CSV repositories, Registry<T>, SimClock, experiment runner", "7A8590"]];
layers.forEach((l, i) => {
  const y = 1.25 + i * 0.75;
  s.addShape(pres.ShapeType.rect, { x: 0.5, y, w: 2.6, h: 0.62, fill: { color: l[2] }, line: { color: l[2] } });
  s.addText(l[0], { x: 0.6, y, w: 2.4, h: 0.62, fontSize: 15, bold: true, color: l[2] === AMBER ? INK : "FFFFFF", margin: 0, valign: "middle", isTextBox: true });
  s.addText(l[1], { x: 3.3, y, w: 6.2, h: 0.62, fontSize: 15, color: INK, margin: 0, valign: "middle", isTextBox: true });
});
footer(s);

// 4 OOP concepts
s = light("Where each OOP idea shows up",
 "Map concepts to concrete code. Encapsulation: private fields, validated setters. Inheritance and polymorphism: abstract Vehicle with Truck, FuelTruck, MedicalVan, Helicopter; estimateHours is a template method. Interfaces: Trackable, Deliverable, Persistable. Generics: Registry<T>, CsvRepository<T>. Exceptions: a LogisticsException hierarchy for domain refusals.");
const rows = [["Encapsulation", "Private state, validated changes, enums for status"], ["Inheritance", "Abstract Vehicle: Truck, FuelTruck, MedicalVan, Helicopter"],
 ["Polymorphism", "Each vehicle estimates its own travel time and what it can carry"], ["Abstraction", "Interfaces Trackable, Deliverable, Persistable"],
 ["Generics", "Registry<T> and CsvRepository<T extends Persistable>"], ["Exceptions", "Checked LogisticsException family, no silent failures"]];
rows.forEach((r, i) => {
  const col = i % 2, row = Math.floor(i / 2), x = 0.5 + col * 4.6, y = 1.25 + row * 1.2;
  s.addShape(pres.ShapeType.ellipse, { x, y: y + 0.1, w: 0.5, h: 0.5, fill: { color: i % 2 ? MOSS : OLIVE }, line: { color: "FFFFFF", width: 0 } });
  s.addText(String(i + 1), { x, y: y + 0.1, w: 0.5, h: 0.5, fontSize: 16, bold: true, color: "FFFFFF", align: "center", valign: "middle", margin: 0, isTextBox: true });
  s.addText([{ text: r[0], options: { bold: true, fontSize: 17, breakLine: true, fontFace: "Cambria" } }, { text: r[1], options: { fontSize: 14 } }], { x: x + 0.7, y, w: 3.7, h: 0.95, color: INK, margin: 0, valign: "top", isTextBox: true });
});
footer(s);

// 5 UML
s = light("UML class diagrams",
 "Left: domain model, vehicle inheritance. Right: services, strategies and facade. The exception hierarchy and a sequence diagram of the demo are in the repository. Say: all diagrams are in the docs folder on GitHub, with the link on the last slide.");
s.addImage({ path: R + "/docs/img/class_domain.png", x: 0.5, y: 1.15, h: 3.95, w: 3.95 * 1568 / 2036 });
s.addImage({ path: R + "/docs/img/class_services.png", x: 3.9, y: 1.4, w: 5.2, h: 5.2 * 1194 / 1568 });
s.addText("Domain model", { x: 0.5, y: 5.1, w: 3, h: 0.25, fontSize: 11, color: GREY, margin: 0, isTextBox: true });
s.addText("Services, strategies, facade. More diagrams on GitHub", { x: 3.9, y: 4.65, w: 5.2, h: 0.25, fontSize: 11, color: GREY, margin: 0, isTextBox: true });

// 6 Patterns
s = light("Design choices that make the rules swappable",
 "Strategy is the core idea: donor selection (NEAREST, SURPLUS, BALANCED) and vehicle selection (SMALLEST, FASTEST) are objects you can swap at run time with the STRAT command. Facade keeps the UI simple. Template Method: Vehicle.estimateHours. Dependency injection makes everything testable. Key domain rule: donors only give stock above their reorder level.");
card(s, 0.5, 1.25, 4.4, 1.7, "Strategy", "NEAREST, SURPLUS, BALANCED donors; SMALLEST, FASTEST vehicles; RANDOM as a baseline. Change with STRAT.", MOSS);
card(s, 5.1, 1.25, 4.4, 1.7, "Facade", "LogisticsManager is the single entry point for the UI, the scripts and the simulator.", "3E6A8A");
card(s, 0.5, 3.1, 4.4, 1.7, "Template Method", "Vehicle.estimateHours is fixed; each subclass supplies speed and what it may carry.", OLIVE);
card(s, 5.1, 3.1, 4.4, 1.7, "Domain safety rules", "Donors keep their reorder level. Stock is reserved on plan, removed on dispatch, added on delivery, returned on failure.", "B5543C");
footer(s);

// 7 Demo
s = pres.addSlide({ masterName: "DARK" });
s.addText("Live demo", { x: 0.6, y: 0.4, w: 6, h: 0.7, fontSize: 34, bold: true, fontFace: "Cambria", color: "FFFFFF", margin: 0, isTextBox: true });
s.addText("Scenario: Pune needs 200 medical kits, urgently", { x: 0.6, y: 1.1, w: 8, h: 0.4, fontSize: 18, color: AMBER, margin: 0, isTextBox: true });
const steps = ["run.bat   (opens the example network; --demo auto-plays it)", "REQ NEW PUN MED-KIT 200 URGENT   then   REQ APPROVE", "REQ FULFIL   (donor and vehicle chosen by the strategies)", "DLV DISPATCH   then   TICK 12   (time moves only on TICK)", "RPT KPI   then   STRAT SRC BALANCED   (swap the rule)"];
steps.forEach((t, i) => {
  const y = 1.8 + i * 0.64;
  s.addText(String(i + 1), { x: 0.6, y, w: 0.45, h: 0.45, fontSize: 16, bold: true, color: INK, align: "center", valign: "middle", fill: { color: AMBER }, margin: 0, isTextBox: true });
  s.addText(t, { x: 1.3, y, w: 8, h: 0.45, fontSize: 16, color: "FFFFFF", fontFace: "Consolas", margin: 0, valign: "middle", isTextBox: true });
});
s.addNotes("DEMO, about 5 minutes. Have the terminal open and the jar already built. Backup: run.bat --demo auto-plays the same scenario. Say what you type before you type it. Point out the stock change at the donor and at Pune, the vehicle chosen, and the KPI report. If something fails, run SAVE and LOAD from the demo folder. Full script: docs/demo_script.md.");

// 8 Experiment design
s = light("How we tested the dispatch rules",
 "1,080 simulation runs: 3 dataset sizes, 30 random datasets each, 4 donor rules, 3 vehicle rules. Each run replays a stream of requests hour by hour. We report averages with 95% confidence intervals and paired Wilcoxon tests with Holm correction. All data is synthetic, so results describe the model, not real operations.");
const big = [["1,080", "simulation runs"], ["3 x 30", "sizes x datasets"], ["12", "strategy pairs"], ["74 / 93", "tests significant after correction"]];
big.forEach((b, i) => {
  const x = 0.5 + i * 2.3;
  s.addText(b[0], { x, y: 1.4, w: 2.1, h: 0.9, fontSize: 40, bold: true, fontFace: "Cambria", color: i % 2 ? MOSS : OLIVE, margin: 0, isTextBox: true });
  s.addText(b[1], { x, y: 2.3, w: 2.1, h: 0.6, fontSize: 14, color: GREY, margin: 0, valign: "top", isTextBox: true });
});
s.addText([{ text: "Metrics: ", options: { bold: true } }, { text: "fulfilment rate, lead time (request to full delivery), critical requests within 24 h, distance, vehicle use.", options: { breakLine: true } },
  { text: "Honest limits: ", options: { bold: true } }, { text: "synthetic data, straight-line distances, constant speeds, no breakdowns.", options: {} }],
  { x: 0.5, y: 3.4, w: 9, h: 1.3, fontSize: 15, color: INK, margin: 0, valign: "top", paraSpaceAfter: 8, isTextBox: true });
footer(s);

// 8b results chart
s = light("Result 1: BALANCED is fastest from medium size upward",
 "Mean lead time in hours, fastest-vehicle rule. On large: BALANCED 28.4 h, NEAREST 34.8 h, SURPLUS 40.0 h, RANDOM 55.7 h. BALANCED beats NEAREST by 6.3 h on large (adjusted p about 1e-6). On small the gap is not significant. NEAREST uses the least distance; SURPLUS costs about 42% more distance than NEAREST on large.");
s.addChart(pres.charts.BAR, [
  { name: "BALANCED", labels: ["Small", "Medium", "Large"], values: [22.9, 29.2, 28.4] },
  { name: "NEAREST", labels: ["Small", "Medium", "Large"], values: [24.3, 32.2, 34.8] },
  { name: "SURPLUS", labels: ["Small", "Medium", "Large"], values: [26.5, 36.3, 40.0] },
  { name: "RANDOM", labels: ["Small", "Medium", "Large"], values: [34.0, 48.7, 55.7] }],
  { x: 0.5, y: 1.2, w: 6.2, h: 3.9, barDir: "col", barGrouping: "clustered", chartColors: [MOSS, "3E6A8A", AMBER, "9AA3AB"],
    showValue: true, dataLabelFontSize: 10, dataLabelColor: INK, dataLabelFontFace: "Calibri", showLegend: true, legendPos: "b", legendFontSize: 11, legendFontFace: "Calibri",
    catAxisLabelColor: INK, valAxisLabelColor: GREY, catAxisLabelFontSize: 12, valAxisLabelFontSize: 10, catAxisLabelFontFace: "Calibri", valAxisLabelFontFace: "Calibri",
    valGridLine: { color: "E1E5DC", size: 0.5 }, catGridLine: { style: "none" }, showValAxisTitle: true, valAxisTitle: "Mean lead time (hours)", valAxisTitleFontSize: 11, valAxisTitleColor: GREY });
s.addText([{ text: "6.3 h", options: { fontSize: 40, bold: true, color: MOSS, fontFace: "Cambria", breakLine: true } },
  { text: "faster than NEAREST on large (p < 0.001 after correction)", options: { fontSize: 14, color: INK, breakLine: true } },
  { text: " ", options: { fontSize: 8, breakLine: true } },
  { text: "NEAREST drives the fewest kilometres; SURPLUS about 42% more on large.", options: { fontSize: 14, color: INK } }],
  { x: 7.0, y: 1.5, w: 2.5, h: 3, margin: 0, valign: "top", isTextBox: true });
footer(s);

// 9 baseline
s = light("Result 2: coordination beats the random baseline",
 "NEAREST with FASTEST versus fully random choices. Lead time roughly halves or better. CRITICAL requests delivered within 24 hours go from about a quarter or a third to about 75-85 percent. Distance is 35-42 percent lower. Caution: the baseline is deliberately weak, so this is an upper bound on practical benefit. Fulfilment rate on small does not differ.");
const st = [["24 h vs 46 h", "lead time, small"], ["35 h vs 85 h", "lead time, large"], ["84% vs 32%", "critical requests within 24 h, large"], ["-42%", "distance travelled, large"]];
st.forEach((b, i) => {
  const x = 0.5 + (i % 2) * 4.6, y = 1.3 + Math.floor(i / 2) * 1.75;
  s.addShape(pres.ShapeType.rect, { x, y, w: 4.4, h: 1.5, fill: { color: i === 2 ? OLIVE : PAPER }, line: { color: "D5DACC", width: 0.75 } });
  s.addText(b[0], { x: x + 0.25, y: y + 0.15, w: 3.9, h: 0.8, fontSize: 34, bold: true, fontFace: "Cambria", color: i === 2 ? AMBER : OLIVE, margin: 0, isTextBox: true });
  s.addText(b[1], { x: x + 0.25, y: y + 0.95, w: 3.9, h: 0.4, fontSize: 14, color: i === 2 ? "FFFFFF" : GREY, margin: 0, isTextBox: true });
});
s.addText("Coordinated = NEAREST donor + FASTEST vehicle. Baseline = random donor + random vehicle. The baseline is deliberately weak.", { x: 0.5, y: 4.8, w: 9, h: 0.4, fontSize: 12, color: GREY, margin: 0, isTextBox: true });

// 10 quality
s = light("Quality: tests, reproducibility and honest limits",
 "119 automated JUnit tests across 21 classes, every one of the 66 types is exercised. Experiments are deterministic: same seed, same result, checked by a script. Honest limits: about 2 percent of requests stay open in every run. We diagnosed it: mostly stock-limited because donors keep a reorder floor, and a backlog of slow fuel trucks on larger sizes. We did not test a longer horizon or more fuel trucks. This is listed in the report under Known inconsistencies.");
card(s, 0.5, 1.25, 2.85, 3.5, "119 tests", "21 test classes cover all 66 types. Run with one command (com.dlscms.RunAll).", MOSS);
card(s, 3.575, 1.25, 2.85, 3.5, "Reproducible", "Fixed seeds; a script re-runs the grid and compares every number with the saved results.", "3E6A8A");
card(s, 6.65, 1.25, 2.85, 3.5, "Known gaps", "1-3% of requests stay open: donors keep a reserve and fuel trucks are scarce. We explain it, we did not remove it.", "B5543C");
footer(s);

// 11 Closing
s = pres.addSlide({ masterName: "DARK" });
s.addText("Summary and links", { x: 0.6, y: 0.4, w: 8, h: 0.7, fontSize: 34, bold: true, fontFace: "Cambria", color: "FFFFFF", margin: 0, isTextBox: true });
s.addText([
  { text: "Clean OOP design: facade, strategies, vehicle hierarchy, generics", options: { bullet: true, breakLine: true } },
  { text: "Swappable dispatch rules, measured over 1,080 runs", options: { bullet: true, breakLine: true } },
  { text: "BALANCED donor selection is fastest at scale; coordination clearly beats random", options: { bullet: true, breakLine: true } },
  { text: "Next: longer horizons, bigger fuel fleet, real-world data", options: { bullet: true } }],
  { x: 0.6, y: 1.3, w: 8.6, h: 1.9, fontSize: 17, color: "FFFFFF", margin: 0, valign: "top", paraSpaceAfter: 8, isTextBox: true });
s.addText([{ text: "Source code and UML on GitHub", options: { bold: true, color: AMBER, breakLine: true } },
  { text: REPO, options: { hyperlink: { url: REPO }, color: "9CC3E0", fontSize: 11 } }],
  { x: 0.6, y: 3.6, w: 8.8, h: 0.8, fontSize: 15, margin: 0, isTextBox: true });
s.addText("Thank you. Questions?", { x: 0.6, y: 4.6, w: 6, h: 0.4, fontSize: 18, color: "C9D3BE", margin: 0, isTextBox: true });
s.addNotes("Close in 30 seconds. Repeat the one-line takeaway, point to the GitHub link, invite questions. Likely questions: why BALANCED wins (it avoids draining one donor), why 2% stay open (reorder floor and fuel trucks), how to load your own data (docs/bring_your_own_data.md).");

pres.writeFile({ fileName: "DLSCMS_Presentation.pptx" }).then(() => applyTheme("DLSCMS_Presentation.pptx", THEME)).then(() => console.log("ok"));
