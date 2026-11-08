# Actually Additions: More Additions

NeoForge addon for Actually Additions.

| | |
|---|---|
| Mod ID | `aamoreadditions` |
| Minecraft / NeoForge | 1.21.1 / 21.1.249 |
| Version | 1.1.0 |
| Depends on | Actually Additions `1.3.26+mc1.21.1` (Maven) |
| Optional compile | Jade, JEI APIs |

## Build

Requires JDK 21.

```bash
./gradlew build
```

Jar output: `build/libs/aamoreadditions-1.1.0.jar`

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

The Upgrade Plugins Wrench uses the same rule policy and supports recorded 0–10-slot layouts.
Its recorded list is shown in the item tooltip; record/transfer notifications use the action bar.
