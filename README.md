# Exploration Reloaded

Fabric mod extracting Fixed Minecraft's exploration-focused changes into a standalone project for Minecraft 1.21.11.

## Scope

- Structure rarity overrides for villages, ruined portals, shipwrecks, ocean ruins, ocean monuments, and strongholds.
- Exploration maps in selected loot tables and cartographer trades.
- Map Book item and map UI extracted from Fixed Minecraft.
- Wandering trader buying trades from Fixed Minecraft's trader changes.
- Fixed Minecraft fishing loot pools and bait behavior.
- Sniffer dig loot expansion and related husbandry advancements.
- Campfire-powered Elytra flight, Smokestack charges and Cloudskipper gliding, plus underwater Elytra restriction.
- Horse, nautilus, parrot, llama, and caravan travel changes.
- Ghast harness recipes and Nether fortress harness loot.
- Custom map decorations for outposts, ruined portals, and trail ruins.
- Trail ruins exploration compass loot function.

## Covered flight stations

Place a trapdoor directly above a lit campfire to control its smoke:

- **Closed:** only a few small smoke wisps escape just above the lid, and the updraft is blocked. With Elytra equipped, crouch on the trapdoor to charge, then stand up to launch. Smokestack charges and the launch bonuses from neighbouring campfires and hay are preserved.
- **Open:** the normal smoke column returns and the updraft passes through the trapdoor, allowing gliders to gain altitude overhead.

Both upper and lower trapdoors work, including iron trapdoors controlled by redstone. Each fire needs its own lid. Solid roofs and water still block updrafts; an extinguished fire cannot charge a glider. Smoke emitted before closing a lid fades out normally.

## Validation

Run `./gradlew build runGameTest` with Java 25. Game tests cover charging surfaces, unenchanted launches, Smokestack charges, hearth power and redstone lids. Test-only code is kept in `src/gametest` and is not included in the mod jar.

For visual verification, compare open and closed lids on a single fire and on a 3×3 hearth over hay. Check both trapdoor halves, normal and soul campfires, and the reduced particle setting. Closed lids should show only small wisps; reopening should restore the tall column and lift. Check the charging HUD and takeoff in first person.

## Attribution

This project includes exploration-related code and data derived from Fixed Minecraft by green_jab, used with permission under the MIT License. See `NOTICE`.

## License

Copyright (c) 2026 Aqu1tain.

Exploration Reloaded is licensed under the GNU Lesser General Public License v3.0 only. See `LICENSE`
for the LGPL text and `COPYING` for the GPL text it builds on.

It was previously CC BY-NC-SA 4.0. The move to LGPL-3.0 was made so the mod can carry a port of
[Aileron](https://github.com/OrtusMC/Aileron) and its Fabric rewrite
[Eleron](https://codeberg.org/sindercube/eleron), both LGPL-3.0-only, whose terms forbid the
non-commercial restriction the previous license imposed. The MIT-licensed upstream code noted above
stays compatible with this.
