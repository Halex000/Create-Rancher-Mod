# Create Rancher

A ranching addon for [Create](https://github.com/Creators-of-Create/Create) on NeoForge 1.21.1. It adds machines for
moving, feeding, shearing and milking animals, plus alfalfa, manure, stews, grills and more. Needs Create 6.0.6 or
newer.

Most blocks have a Ponder scene (hold W over them) and all the recipes are in JEI.

## Machines

### Mechanical Herder

A brass arm with a suction cup that picks up mobs and drops them somewhere else.

1. Sneak + right-click the drop-off block with the herder.
2. Place the herder within 7 blocks of it. It searches the side facing away from you.
3. Power it through its cog.
4. Right-click it with an empty hand to set the mob, the age (babies, adults or both) and the search area (3x3x3,
   5x3x5 or 7x3x7).

It follows mobs that wander off and lifts them 4 blocks to carry them over. Redstone pauses it.

### Mechanical Feeder

A wooden arm that feeds animals.

- Right-click a depot, belt, basin or anything else an arm can take from to set its pickup point, then place it
  within 5 blocks.
- It feeds animals in a 7x3x7 area. Breeding food only goes to adults with a partner ready, Alfalfa Pellets go to any
  animal that eats them, and brown mooshrooms get one flower each.
- Right-click it with an empty hand to take back what it's holding.

### Mechanical Shearer

The feeder's arm with shears for a hand.

- No setup needed. It shears anything shearable in a 7x3x7 area, other mods' animals included.
- Drops land on the ground unless a hopper, funnel, chute or arm pulls from it.
- Costs 2 stress per RPM (configurable). Redstone stops it.

### Mechanical Fisher

Fishes on its own with a Fishing Net.

- Sneak + right-click water with it to pick a spot, then place it within 16 blocks.
- Powered by a cogwheel next to it, like the Mechanical Crafter. Uses 256 SU at any speed.
- Right-click it with a net to load it. It casts, waits for a bite and hauls in 1 to 4 catches.
- Catches go into its 9 slots. Take them out by hand or with hoppers and funnels. Sneak + right-click to take the net
  back. Redstone pauses it.

### Livestock Drain

Animals standing on it fill it with whatever they'd give a bowl or bucket.

- Cows and goats give milk, mooshrooms give mushroom stew, and a brown mooshroom that's eaten a flower gives one
  serving of suspicious stew. Other mods' animals work too.
- 40mb a second per animal. Animals on a carpet over it still count.
- Holds 1500mb, of several fluids at once. Pipe out of the sides or bottom, or use a bucket.
- Drains next to each other share their fluid like tanks.

### Liquid Blaze Burner

A blaze burner that burns liquid fuel from a 2000mb tank.

- Fill it with pipes, spouts or buckets. Nothing can drain it.
- A bucket of lava lasts 16:40. A bucket of Biofuel lasts 20 minutes and superheats it.
- Fuels from other mods work through the common fluid tags (diesel, gasoline, ethanol, crude oil, plant oil and so
  on), along with Thermal's fuels and Tinkers' Construct's blazing blood.

More fuels can be added with data packs in `data/<namespace>/liquid_blaze_burner_fuels/`:

```json
{ "tag": "minecraft:lava", "burn_time": 20000, "superheated": false }
```

### Smart Depot

A brass funnel and a depot in one block.

- Put it on an inventory and it pulls items up onto its top, one lot at a time.
- The filter slot sets which item and how many, like a brass funnel.
- Place it while sneaking (or wrench it) to reverse it, so it puts items from its top into the inventory below. In
  this mode it waits for any press, deployer, spout or fan to finish first.
- Redstone stops it.

### Auger

Moves items up, down or sideways.

- Place augers end to end. They work upright or lying flat, and a shaft at either end turns the whole line. No
  extra stress.
- Place one against the side of an upright auger to make a diagonal, like Create's chute.
- Counter-clockwise carries items up (or east/south when flat), clockwise carries them down.
- Funnels, hoppers and arms can put items in or take them out anywhere along it. Items wait at the end.
- An end facing a chest pulls from it or pushes into it, depending on which way it turns.

## Item Grate

**On belts:** right-click a flat belt segment to lay a grate on it. Players and mobs can stand on it without being
carried, but items still fall through. Right-click it with a copper, iron or brass nugget to plate it. Sneak +
right-click with an empty hand to take it off.

**As a grill:** right-click a campfire, soul campfire, or a water or lava cauldron. It holds 9 items and each takes 10
seconds.

| Grill | What it does |
|---|---|
| Campfire | Smokes food |
| Soul campfire | Haunts items |
| Lava cauldron | Blasts items |
| Water cauldron | Washes items, needs heat underneath |

Finished items pop off unless a hopper, funnel or arm takes them. Belts carry items across grills, and grills pass
items along to each other. Sneak + wrench a grill to take the grate off.

## Farming

### Alfalfa

- Wild Alfalfa grows near water in plains and along rivers. Break it for seeds.
- Novice farmers buy it like wheat.
- Alfalfa Bales work like hay bales, but horses and llamas won't eat them.
- Animals fed Alfalfa Pellets drop Dung a few minutes later.

### Dung and Manure

- Dung stacks in layers like snow, but doesn't melt and water flows around it.
- Manure works like bone meal.
- Plants grow on a Block of Manure like on dirt, and sugar cane doesn't need water.
- Manure Farmland never dries out and can't be trampled.

## Food and Drink

### Stews and Cans

- Mushroom Stew, Rabbit Stew, Beetroot Soup and Suspicious Stew are fluids too. Spouts fill bowls or cans with them.
- New Beef, Chicken, Pork and Mutton Stews, and Rabbit Stew no longer needs a mushroom. All of them give 14 hunger and
  18 saturation.
- Canned stew stacks to 64 and you eat the can with it.
- Item Drains empty any bowl or can of stew or soup, 250mb each, vanilla ones included.
- Suspicious stew works with other mods' flowers.
- With Delightful Creators installed, its Mushroom Stew, Rabbit Stew and Beetroot Soup fluids are swapped for this
  mod's.
- Use an Empty Can on a cow or goat for milk, or on a mooshroom for stew.
- Stews (except suspicious stew) can be bucketed and placed. They flow slowly, like lava.

### Milk and Chocolate Milk

- Chocolate milk comes in buckets and cans. It only removes bad effects.
- Spouts fill Empty Cans with a bucket's worth of milk or chocolate milk, and Item Drains empty them.
- Right-click Create's spout with a bucket to fill or empty it. Sneak to skip this.

## Potions

Haste, Mining Fatigue, Nausea, Blindness, Glowing, Darkness and Hunger. The harmful ones last 1:30, or 4:00 extended.

## Fluids

- Tannin is used for leather, Builder's Tea, black dye and Tannic Powder.
- Biofuel fuels the Liquid Blaze Burner, furnaces and Create's blaze burner, which it superheats for 20 minutes.
- The mod's fluids react with lava like water does, but make different blocks: dripstone from stews, deepslate and
  cobbled deepslate from tannin, and blackstone from biofuel.

## Building

### Stained Wood

Right-click logs or wood with Tannic Powder (or use a deployer) to stain them barn red. It's a full wood set.

### Wooden Joists

Create's Metal Girder in wood, for every vanilla wood, stained wood and any other mod's wood it finds. They work like
girders but can't hold a shaft.

## Fishing Net

- Cast and reel it in like a fishing rod. It brings up 1 to 4 catches at once.
- It only catches treasure with Treasure Hunter, a new enchantment for nets.
- Nets turn up as fishing treasure, and some journeyman fishermen sell them.

## Other Stuff

- **Crate:** holds 15 items and keeps them when broken, like a shulker box.
- **Satchel:** a 9-slot bag you open from your hand.
- Pellet digestion time, grill times and some stress costs are in the common config.
- Everything works on Create Aeronautics ships. A machine's target has to be on the same ship, and it's forgotten if
  the ship splits apart.

## Development

- Java 21
- `./gradlew runClient` starts a dev client with Create, Ponder, Flywheel and JEI
- `./gradlew runData` runs Registrate datagen into `src/generated/resources`
- `./gradlew build` builds the jar into `build/libs`

Blocks and items are registered in `com.halex.createrancher.index` through `CreateRancher.REGISTRATE`. The creative
tab and JEI order is set by `CreateRancherTabs.ORDER`.

Ponder schematics are in `assets/createrancher/ponder`, with their text in the lang file under `createrancher.ponder.*`.
The keys are numbered in the order `.text()` is called, so keep the lang file in step when editing a scene.

## Credits

- Many textures are recoloured or cut down from Create's: the powders, stew fluids and buckets, grill legs, Liquid
  Blaze Burner, Auger, Smart Depot and Livestock Drain. Create is by simibubi and the Create team, under the
  [MIT licence](https://github.com/Creators-of-Create/Create).
- The Wooden Joists use Create's Metal Girder model and code.
- The new stew bowls, the Chocolate Milk Bucket and the Alfalfa Bale are recoloured from Minecraft's textures.
- The Liquid Blaze Burner's fuels, JEI page and Ponder scene are based on
  [Create Crafts & Additions](https://github.com/mrh0/createaddition) by MRH0, under the MIT licence.
