# Vanilla goat equipment

Authored and exported in Blockbench 5.1.6, using the Minecraft 26.1.2 goat as the fitting reference.

## Editable projects

- `goat_saddle.bbmodel`: saddle, blanket, girth and stirrups.
- `goat_armor.bbmodel`: breastplate, shoulders, flanks and four leg guards.
- `goat_mount_preview.bbmodel`: both sets fitted to the vanilla goat for visual checks. Vanilla geometry and texture are included only as an authoring reference.
- `preview.png`: Blockbench preview of the iron set.

Open the projects with **File > Open Model** in Blockbench. Equipment uses a 128×128 box-UV atlas with the vanilla model's pixel density. Keep the six bone names and their origins unchanged so equipment follows the native body and legs. Export the textures to `src/main/resources/assets/exploration-reloaded/textures/entity/goat`.

`GoatSaddleExport.java` and `GoatArmorExport.java` are the raw Modded Entity exports. Their `createBodyLayer` methods become `saddle` and `armor` in `GoatEquipmentGeometry`; the runtime wrapper uses Minecraft 26.1.2 render states.

## Texture treatment

The metal palettes follow the Minecraft 26.1.2 horse-body equipment textures: neutral iron, bright yellow gold, turquoise diamond, coral copper and dark warm netherite. Faces have coherent bevels and a lower overlapping plate instead of random speckles. Chainmail has open rings. Leather uses a greyscale dye layer and a separate brown strap layer, with the vanilla horse's default undyed colour.

`goat_equipment.js` records the authoring recipe and can be loaded as a local Blockbench plugin. Its optional export bridge uses a loopback server on port 8129; opening and editing the `.bbmodel` files does not require this helper. `export-report.json` records the cube definitions and UV layout used by the export.

Runtime poses are procedural. Rearing uses AbstractHorse's 20-tick stand, animation easing and AbstractEquineModel's 45-degree body lean and alternating forelegs, fitted around the goat's hip and smaller body. The same stand starts when releasing a charged jump. Legs tuck once the stand ends in mid-air, with a little compression on landing. Mounted ramming uses the native goat head-lowering animation. The equipment and vanilla goat share the same pose calculation. Horns, facial shape, goat textures and baby rendering are retained.
