# Actually Additions: More Additions

NeoForge addon for Actually Additions.

| | |
|---|---|
| Mod ID | `aamoreadditions` |
| Minecraft / NeoForge | 1.21.1 / 21.1.249 |
| Version | 1.2.1 |
| Depends on | Actually Additions `1.3.26+mc1.21.1` (Maven) |
| Optional compile | Jade, JEI APIs |

## Build

Requires JDK 21.

```bash
./gradlew build
```

Jar output: `build/libs/aamoreadditions-1.2.1.jar`

## License

- **Original addon code and original audio** (`alloyfurnace.ogg`, `empowerer_structure.ogg`): [MIT](LICENSE) © 2026 YenipyuSaya.
- **AA-derived textures** (alloy furnace block textures copied/edited from Actually Additions): remain under the Actually Additions MIT license. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and `META-INF/licenses/ActuallyAdditions-MIT.txt`. This project does **not** claim sole ownership of those AA originals.
- **NeoForged MDK template files**: see [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).

No Create artwork is redistributed by this project.


## Upgrade rule modes and slots

`config/aamoreadditions-startup.toml` contains **Dependency Upgrade Plugins** (default `false`)
and one `slots` entry for each machine, including the Alloy Furnace and Casing Cross.
Slot counts range from 0 to 10; defaults retain the original 5 / Energizer 2 / Cross 3 slots.
**Restart the game/server after changes.** These values are captured once; reloading the TOML
or datapacks during play does not change the active rules. Multiplayer clients use the
server's startup snapshot. Existing per-machine effect coefficients remain in the server config.

- `true`: original prerequisite / no-duplicate rules, highest tier per line, original I/O limits.
- `false`: repeated and independent grades, combined effects and boundary penalties; FE I/O limits
  are `Integer.MAX_VALUE`. Three additional previous-tier crafting recipes are available only here.
- Zero slots hides both upgrade buttons; up to ten slots remain in a centered row above the GUI.
- Shrinking slots retains the leftmost slots. Excess items are saved in a pending world-drop queue
  until the machine/structure's chunk ticks; they are not put in a player's backpack or discarded.
- Switching to dependency mode keeps illegal installed items, but stops work and clears remaining
  fuel time / processing progress. FE exchange and the repair GUI remain available; item/fluid
  automation and automated JEI filling are blocked until the plugins are repaired. The GUI shows
  **Wrong plugin logic!** while invalid. Back up worlds before changing a heavily used layout.
- Capacity reductions continue to discard energy above the new capacity, as before.
- The info tooltip shows only the final boundary penalty coefficients, not layer counts or the
  full upgrade multipliers. Unaffected rows and empty penalty sections are hidden.
  Counts combine every grade on the same line (not each tier separately), with
  `L = min(3, max(0, count - 3))`. Processor efficiency operation energy is the product
  of its plugin coefficients times `(1 + 0.15 * L)`; Efficiency II itself remains `0.5`.
  Five Efficiency II plugins therefore use `0.5^5 * 1.3 = 0.040625` operation energy.
  Processor energy's displayed boundary coefficient is `(1 + 0.1 * Ls) * (1 + 0.15 * Le)`.
  Other boundary penalties are unchanged.

The Upgrade Plugins Wrench uses the same rule policy and supports recorded 0–10-slot layouts.
Its recorded list is shown in the item tooltip; record/transfer notifications use the action bar.
Wrong-plugin-logic wrench warnings are red; success notifications are unchanged.

Config labels are readable English display metadata, independent of the game's language;
TOML paths remain compatible. Categories have readable titles but no description tooltips;
leaf descriptions are retained. When Configured is installed, its own
config factory is used instead of being overridden by the native fallback screen. The native
fallback's dependency tooltip can widen to its unchanged title, bounded by the window.
Upgrade changes explicitly sync and preserve the attachment instance referenced by open menus.

Jade uses server-selected upgrade icon/count groups (at most three groups per row), including
Energizers and all valid Casing Cross parts. Independent mode counts each exact item ID/grade;
dependency mode displays only the highest active grade on each upgrade line. Empty layouts
send an explicit empty state. Illegal layouts hide the upgrade section and never advertise active plugins.

Farmer uses the shared restart-required `slots.farmer` count (default five, range 0–10).
Its toggle is at (134, 4), info at (123, 4), both 10×10; zero slots hides both. It accepts
Speed I/II/III and Efficiency I/II under the existing shared mode/legality rules, uses the
same handler, persistence, synchronization, wrench, Jade and overflow-drop lifecycle.

Farmer's coefficients come from `ProcessorUpgradeDefaults`, the exact same code defaults
used by processor config generation; there is no new effect config namespace or coupling
to a particular processor's overridden config. Speed coefficients are 2/3/5, speed-side
operation energy 1.5/2/2.5; efficiency energy .8/.5 and capacity 1.5/2. In independent mode,
scan speed sums without boundary loss; actual scan-to-scan cooldown is
`max(1, floor(6 / speed))`, including the original reset tick (so one really means one tick).
Each operation costs `max(1, round(original_cost * total_energy_multiplier))`. Original
planting gates still reserve TWO individually rounded unit costs, even on existing farmland;
only operations actually performed are charged. The five built-in AA behaviors retain their
existing failure, planting, seed-routing and full-fit inventory-check logic.

Independent-mode energy uses the shared processor sum/product and .1/.15 penalties. Capacity
sums efficiency coefficients without a penalty. Fortune Bonus uses exact integer tenths
(7/12 for Efficiency I/II) and the confirmed 1/2/3/5 thresholds. Dependency mode activates
only the highest tier on each line, without boundary penalties; active Efficiency I gives
Fortune I, Efficiency II gives Fortune III. Slot count zero has baseline farming rates, operation costs, capacity and Fortune.

The harvest adapters inject a real Fortune-enchanted diamond hoe into the original loot
context, preserving the original call chain (not LUCK or comparison/reroll heuristics).
Fortune IV doubles the already-rolled drops only for original vanilla sugar cane, pumpkin
and cactus. Melon uses its vanilla loot table's nine-slice maximum and is never doubled;
other original vanilla crops use their normal Fortune loot functions (e.g. wheat seeds,
not extra wheat). Final drops are generated/split before AA's original inventory fit checks.
Arbitrary third-party farmer behaviors and mod/datapack changes to loot sensitivity are
not automatically classified or guaranteed; the explicit vanilla list is not a dynamic
Fortune detector. Illegal-plugin machines cannot work through the built-in behavior or
planting helper entry points, while shared FE access and manual repair remain available.

Farmer info has exactly three rows: `work cooldown`, energy multiplier, and
`current Fortune level`. The first two values are aqua, Fortune is green, other text white.
There is no extra Farmer Boundary Loss row (the energy multiplier includes that penalty),
no displayed scan multiplier/capacity/Bonus field. Other machines' boundary info is unchanged.
The startup-policy protocol is 3, with its list bound derived from the shared machine keys;
client and server must both use the updated addon. No mod-version change is implied.

First attachment NBT reads defer the constructor's empty-plugin capacity refresh until
saved plugins have been restored, preventing upgraded stored FE from being truncated to
bare capacity during chunk load. Genuine capacity reductions still clamp excess FE.

Farmer operation-cost rounding normalizes the fixed defaults' decimal precision before
HALF_UP, avoiding IEEE-754 half-value errors (e.g. .115 × 500 = 57.5 must charge 58 FE).
Every allowed 0–10-slot grade-count combination is checked against exact decimal arithmetic.

Farmer smart sleep is a live SERVER config named **Farmer Smart Sleep**, enabled by default.
A complete normal-speed traversal of the actual odd-sized work area must find no
processable mature harvest before switching to one cell every 20 ticks (not a whole-area
burst). A rejected target does not slow the unfinished round: any actual harvest whose
drops fit keeps that whole round at normal speed, even if all later targets are rejected.
Capacity comes from AA's original final output check, after Fortune and seed fallback;
nonempty slots can still accept compatible stacks. No empty-slot heuristic is used.
A sleeping Farmer wakes on a mature harvest whose actual drops fit or a successful planting;
successful planting resets normal-round accounting and prevents an immediate return to sleep. the following normal
round starts fresh after that wake-up cell and must visit the entire area again.
Positive but insufficient operation FE can prevent reaching the original capacity check:
such a mature target is conservatively uncertain, preventing a new empty-round sleep but
not claiming a fit or waking an already sleeping machine. Zero FE and redstone pause the
native scan. No additional random drop generation is performed. Planting/tilling remain
in the original scan flow at both rates. Disabling clears sleep/partial-round state and
restores the normal plugin period, without disabling the cheap harvest prefilter.

The prefilter exactly recognizes the five built-in behavior classes and native maturity
checks, with only the current block and at most two upper reed/cactus segments examined.
Bare reed/cactus bases retain their original STOP_PROCESSING call semantics. Unknown or
subclassed behaviors run unchanged and conservatively prevent claiming an empty round.
No random drops or area grids are cached; no full-area prescan runs. Only actual cell
checks count, including a fresh or loaded center-offset cursor; redstone/no-FE pauses do
not count. Range, facing, relocation, upgrade changes and load reset progress and wake it.
Sleep is ephemeral across loads and synced through AA's existing block-entity SYNC NBT;
Info retains three rows and reports the server's actual 20-tick sleeping cooldown, with
the existing aqua/aqua/green value colors. No additional network protocol is introduced.

### Farmer growth-ring ripening

**Farmer accelerates ripening** is a live SERVER config, **true by default**. It shows a
separate single growth-ring slot and the approved solid-color arrow. The slot accepts the
actual AA `ring_of_growth` by item identity, including charged/component-bearing stacks.
It is never part of the Farmer's original 12-slot item capability: hoppers and pipes cannot
access it. Manual/shift transfers work independently of the upgrade-panel toggle. Disabling
hides/locks the slot, stops charging and ripening, but retains its saved contents; re-enable
to retrieve the ring. Breaking the Farmer drops that ring once, with its components, even
while disabled. SAVE_TILE and normal AA SYNC NBT preserve the same dedicated handler instance;
SAVE_BLOCK does not embed a second inventory into the dropped machine item.

The independent ring cycle uses the exact current Farmer work plane and facing, not the
player ring's 7x7x7 range. Period = max(10, ceil(30 / shared unpenalized Farmer speed)).
Maximum draws = max(25, HALF_UP(45 multiplied by each active speed plugin's utilization
0.9 / 0.8 / 0.75)). No boundary penalty applies to either number; efficiency has no effect.
Dependency mode uses only the highest active speed tier. With five speed-I plugins in
independent mode: speed 10, period 10 ticks, 45 * 0.9^5 = 26.57205 -> 27 draws.
Each valid randomTick invocation costs exactly **300 ring FE**, even when it causes no growth;
less than 300 stops the cycle. No candidates means no cost. Each cycle gathers candidates
once, then draws randomly **with replacement** up to that budget. Every draw rechecks current
state; a newly mature/attached/otherwise invalid target is skipped without charging and
without a replacement reroll. Thus actual calls may be below the maximum draw count.

Candidates are compatible crop/AGE7 farming states, Nether Wart, sugar cane/cactus below
natural height limits, and unattached melon/pumpkin stems (including AGE7 awaiting fruit).
Ordinary mature crops, attached stems, grass, podzol and mycelium are excluded; this is not
an expansion to all bone-mealable flowers/saplings. Only loaded positions and their adjacent
3x3 chunk neighborhood are used, so vanilla growth checks at borders do not force-load
neighbor chunks. Arbitrary third-party randomTick overrides are not fully compatibility-tested.

Native harvesting/planting runs first, followed by the independent ripening cycle and then
unlimited charging from remaining machine FE. A Farmer-only component-backed adapter bypasses
the ring's 2000 FE/t receive cap, bounded only by machine FE and ring capacity. It deducts
only the amount actually stored, without
upgrade bonuses or fees. Zero machine FE pauses the ring cycle even with a charged ring;
redstone pauses the cycle but, like the Energizer, not charging. Illegal plugin configurations
stop both charging and ripening while retaining permitted external FE interaction. The ring
clock never reads or modifies harvest smart-sleep accounting. Load, ring replacement, mode/
upgrade/area/facing/position changes reset its independent progress; redstone/zero FE pause it.

GUI: 16x16 slot at (67,75), the real growth-ring texture at 35% alpha always underneath actual
items, and a 13x7 solid arrow at (52,79), 3px shaft, 7px triangular head. Gray unless a ring
is present and machine FE > 0; otherwise the actual AA EnergyDisplay color-wheel animation,
regardless of ring charge or redstone. Existing three Info rows are unchanged.
`farmer_ring_slot.png` derives from AA's `gui_farmer.png` slot artwork and remains subject
to the Actually Additions asset license; the ring icon is referenced directly from AA.

Oil and Coal Generators pause already-burning fuel when internal FE is full. Remaining
burn time, fuel metadata and the displayed fuel-progress amount are retained; no new fuel
starts while full. Any positive space, even 1 FE, advances one entire burn tick and the
native storage accepts only the available FE, discarding overflow. Base FE sharing and
normal sync still run, so extracting power resumes the held fuel. Both generators suppress
smoke while full without clearing their fuel-progress fields; redstone rules are unchanged.
Both plugin modes retain
the existing output/utilization formulas and integer-rounded fuel duration: there is no
fractional burn accumulator in the current implementation. Illegal plugin configurations
still clear work under the separate established policy, not this full-storage pause.


### Config registration cleanup
Only fields with a working effect in at least one upgrade mode are registered. COMMON
startup configuration remains 12 values; SERVER configuration has 161 (33 base/fuel/feature controls, 125 existing machine/plugin
coefficients and three newly added Cross speed coefficients), for 173 total. Cleanup removed
155 of the original 325 values; the three Cross settings were added separately.
Valid paths, defaults, bounds and readable English display metadata are unchanged.
Old unused keys/categories are removed by NeoForge's normal config correction on load;
there is no custom file deletion, rewrite or migration. Valid custom values stay at their
existing paths. Unsupported effect getters keep non-null neutral runtime suppliers.

Remaining fields per tier (S=speed, E=energy/utilization, C=capacity, I=input rate,
O=output rate, L=prevent lava consumption, D=leaf cooldown):

| Machine category | Each speed tier I/II/III | Each efficiency tier I/II | Total coefficients |
|---|---|---|---:|
| Crusher | S E C | S E C I | 17 |
| Crusher Double | S E C | S E C I | 17 |
| Powered Furnace (also Alloy Furnace effects) | S E C | S E C I | 17 |
| Coal Generator | S E C O | S E C | 18 |
| Oil Generator | S E C O | S E C | 18 |
| Heat Collector | S C O L | S C L | 18 |
| Leaf Generator | E C O D | E C | 16 |
| Energizer | no speed categories | C I | 4 |
| Casing Cross | S | no efficiency categories | 3 |

Speed-line capacity, efficiency-line speed where consumed, Leaf speed-line utilization,
Heat speed-line lava protection, and dependency-only I/O/cooldown settings are deliberately
retained even when their defaults are neutral or independent mode does not use them.


Casing Cross speed settings are `[casing_cross.speed_upgrade_machine_1/2/3]` →
`speed_multiplier`, default 1.5/2/3 respectively, range 0.01–100000, under the ordinary
live SERVER spec. Runtime and menu Info share `CrossEffects`; it does not read processor
settings. Jade continues to show the actual server-selected plugin icons/counts, not a
separate guessed numerical multiplier. Farmer remains on fixed `ProcessorUpgradeDefaults`
through `FarmerEffects`, independent of both Cross and editable processor coefficients.


Special-machine audit: Cross progress and recipe duration now sync as full 32-bit values
instead of clamping at 32767; its GUI ratio and fixed-plan energy split avoid integer/long
multiplication overflow. No native Empowerer/stand work runs alongside an active shared
structure. Known limitation retained in 1.2.1 (not fixed in this package): changing Cross speed
plugins or their configured coefficients during a recipe can alter its total FE charge,
because the absolute-tick cursor is reused with the newly computed duration. Avoid changing
speed until that recipe finishes. Saved data still stores only `processTime`, not historical
duration or charged FE. The requested cycle-speed-lock and legacy-progress-reset work was
cancelled and is not included; existing saved progress is not reset by a new migration.
