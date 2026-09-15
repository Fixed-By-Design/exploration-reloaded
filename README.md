# Exploration Reloaded

Fabric mod extracting Fixed Minecraft's exploration-focused changes into a standalone project for Minecraft 26.1.2.

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
- Wither Compass journeys between lodestone platforms, including voluntary group travel and emerald villager transport.

## Wither Compass platforms

Build a **single horizontal 7×7 layer of 49 identical beacon-base blocks**, with a Lodestone **on top of the centre block**. Both endpoints must have the same material. No pyramid or additional layers are required. Copper is not a beacon-base material.

You can cover the base with a decorative floor, slabs or carpet and leave only the Lodestone accessible. Travellers' feet must be inside the 7×7 footprint, between the base's top and 1.25 blocks above it. Higher floors are outside the boarding zone.

1. Bind a normal compass to the destination Lodestone.
2. Combine that compass with **one Nether Star on an anvil**, for **one experience level**, to make a **Wither Compass**. An optional name, such as “Northern Temple”, identifies the destination during departure.
3. Stand on the departure platform and use the Wither Compass on its Lodestone. Keep it in the same hand and remain within the particles until departure.
4. After a successful journey, the compass returns to an ordinary compass bound to the same destination. Every traveller receives **3 seconds of Blindness**.

| Base material | Travellers | Reach | Preparation |
| --- | --- | --- | --- |
| Iron | Players | 4,000 horizontal blocks, same dimension | 6 seconds |
| Gold | Players, animals and monsters | 4,000 horizontal blocks, same dimension | 2 seconds |
| Emerald | Players, animals, monsters and villagers | 4,000 horizontal blocks, same dimension | 8 seconds |
| Diamond | Players, animals and monsters | Unlimited distance, same dimension | 6 seconds |
| Netherite | Players, animals and monsters | Unlimited distance, including other dimensions | 6 seconds |
| Rose gold (Additional Additions) | Players, animals, monsters and ridden living mounts | 4,000 horizontal blocks, same dimension | 6 seconds |

On every material, passengers join by **using the departure Lodestone with an empty main hand**. Nearby players are never enrolled automatically. A moving particle boundary marks the boarding area above the floor; enrolled players receive particles around their feet and a countdown above the hotbar. Leaving the zone or taking damage removes a passenger. If the initiator leaves, takes damage, switches compass or disconnects, the journey is cancelled. One charge pays for the entire group. There is no arbitrary player cap. Everyone keeps their exact horizontal position relative to the Lodestone, including fractional X and Z coordinates, sampled at departure. Arrival height adjusts to the destination covering and preserves any height above the departure floor. Looking direction is preserved, and all creatures and mounts follow the same rule.

Gold, emerald, diamond, netherite and rose gold automatically select animals and monsters inside the boarding zone when preparation begins. Iron carries players only. Creatures that leave or take damage are removed from the departure. NPCs travel individually and dismount at departure; boats and minecarts stay behind. Animals carrying players cannot bypass explicit player enrollment.

Emerald additionally selects the villagers standing in the boarding zone when preparation begins, including babies. Their foot particles and the countdown’s creature count show that they are enrolled. Moving out of the zone or taking damage removes them for this departure. All other materials leave villagers and wandering traders behind. Villagers keep their profession, trades and inventory. A villager riding a boat or minecart dismounts on departure; its vehicle stays behind.

Rose gold preserves each player’s mount, saddle and equipment through Minecraft's passenger teleportation. Each rider must control their mount and be its only passenger. Unmounted animals and monsters also travel. Boats and minecarts stay behind. Travelling alone is also allowed.

The destination is loaded before preparation, with a ten-second loading timeout. The whole structure and every exact arrival position are checked again before departure. Any obstruction, other creature or unsafe floor cancels the whole journey without relocating anyone to a different square. Decorative floor heights may differ: arrival adjusts to slabs, carpet and full blocks within the boarding height. Walls, low ceilings and insufficient space for a mount or rider still prevent travel. A blocked destination, different material, missing Lodestone or interrupted journey leaves the Wither Compass charged. A charged compass retains its coordinates so a broken destination can be rebuilt. Materials define compatibility, **not ownership or access permissions**.

The block list follows `minecraft:beacon_base_blocks`. Additional Additions is optional; its rose-gold block receives the mounted-travel rules when installed. Other mods' beacon materials use iron's regional rules and still require the exact same block at both endpoints.

The charged and discharged compass share the normal compass texture and lodestone needle behaviour. The Wither Compass's name and a short “Charged” tooltip identify its charge; both states retain the vanilla lodestone glint. The destination appears briefly at preparation and enrollment, then the hotbar message shows a short countdown, with player and creature counts only for group journeys.

All effects use Minecraft assets: beacon activation/deactivation, amethyst chimes, local teleport sounds and restrained dust/portal particles. Sound volume follows the Blocks slider.

## Covered flight stations

Place a trapdoor directly above a lit campfire to control its smoke:

- **Closed:** only a few small smoke wisps escape just above the lid, and the updraft is blocked. With Elytra equipped, crouch on the trapdoor to charge, then stand up to launch. Smokestack charges and the launch bonuses from neighbouring campfires and hay are preserved.
- **Open:** the normal smoke column returns and the updraft passes through the trapdoor, allowing gliders to gain altitude overhead.

Both upper and lower trapdoors work, including iron trapdoors controlled by redstone. Each fire needs its own lid. Solid roofs and water still block updrafts; an extinguished fire cannot charge a glider. Smoke emitted before closing a lid fades out normally.

## Validation

Run `./gradlew build` with Java 25; it includes `runGameTest`. Game tests cover charging surfaces, unenchanted launches, Smokestack charges, hearth power and redstone lids, plus platform shape, decorated floors, safe arrivals, anvil consumption, explicit group enrollment, cancellations, mounts and cross-dimensional travel. Test-only code and the optional rose-gold registry fixture are kept in `src/gametest` and are not included in the mod jar. To check anvil compatibility, place Enchantment Overhaul in `build/run/gameTest/mods` before running the tests.

For teleportation visual checks, test an emerald pair with bare blocks and with a full decorative floor plus carpet. Compare the boundary, passenger particles and countdown in first and third person, including Reduced particles. Verify that beacon activation, enrollment, rising chimes, departure, arrival and cancellation are audible at normal Blocks volume. For multiplayer, one participant should step out or take damage during preparation; bystanders should stay behind. Test a distant, unloaded destination and a Netherite pair across dimensions.

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
