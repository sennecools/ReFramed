# ReFramed 1.21.1 Port - Changelog

## Overview

This document outlines all changes made to port ReFramed from Minecraft 1.20.4 to 1.21.1.

## Build System Updates

### gradle.properties
- Updated Minecraft version: `1.20.4` → `1.21.1`
- Updated Yarn mappings: `1.20.4+build.3` → `1.21.1+build.3`
- Updated Fabric Loader: `0.15.11` → `0.16.9`
- Updated Fabric API: `0.97.0+1.20.4` → `0.116.7+1.21.1`

### build.gradle
- Updated Java version: `17` → `21`
- Updated Gradle wrapper: `8.6` → `8.12`
- Updated Fabric Loom: `1.6-SNAPSHOT` → `1.9.2`
- Temporarily disabled unavailable dependencies:
  - Axiom (commented out - awaiting 1.21.1 version)
  - Chipped (commented out - awaiting 1.21.1 version)
  - Compat mod dependencies (moved to `compat_disabled` - to be re-enabled later)

### buildSrc/build.gradle
- Added Java 21 toolchain to prevent version conflicts:
  ```gradle
  java {
      toolchain {
          languageVersion = JavaLanguageVersion.of(21)
      }
  }
  ```

## Major API Changes

### 1. Model Loading API (Complete Rewrite)

**Old API (1.20.4):**
```java
ModelResourceProvider
ModelVariantProvider
```

**New API (1.21.1):**
```java
ModelLoadingPlugin with callbacks:
- resolveModel() for block models
- modifyModelOnLoad() for item models
```

**Files Modified:**
- `src/main/java/fr/adrien1106/reframed/client/ReFramedClient.java`
- `src/main/java/fr/adrien1106/reframed/client/ReFramedModelProvider.java`

**Key Changes:**
- Registered `ModelLoadingPlugin` instead of deprecated providers
- `ModelIdentifier` changed from class to record type - requires `instanceof` checks
- Added dual callback approach to handle both block and item model resolution

### 2. Component Data System (Replaced NBT)

**Old API (1.20.4):**
```java
stack.getBlockEntityNbt()
stack.getOrCreateNbt()
```

**New API (1.21.1):**
```java
stack.get(DataComponentTypes.BLOCK_ENTITY_DATA)
stack.apply(DataComponentTypes.BLOCK_ENTITY_DATA, ...)
NbtComponent.copyNbt()
```

**Files Modified:**
- `src/main/java/fr/adrien1106/reframed/block/ReFramedBlock.java`
- `src/main/java/fr/adrien1106/reframed/block/ReFramedEntity.java`
- `src/main/java/fr/adrien1106/reframed/mixin/BlockItemMixin.java`
- `src/main/java/fr/adrien1106/reframed/item/ReFramedBlueprintItem.java`
- `src/main/java/fr/adrien1106/reframed/item/ReFramedHammerItem.java`

### 3. BlockEntity Serialization

**Old API (1.20.4):**
```java
public void readNbt(NbtCompound nbt)
public void writeNbt(NbtCompound nbt)
```

**New API (1.21.1):**
```java
protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
```

**Files Modified:**
- `src/main/java/fr/adrien1106/reframed/block/ReFramedEntity.java`
- `src/main/java/fr/adrien1106/reframed/block/ReFramedDoubleEntity.java`

**Key Changes:**
- Added `RegistryWrapper.WrapperLookup` parameter to all NBT methods
- Changed visibility from `public` to `protected`
- Updated `toInitialChunkDataNbt()` signature

### 4. Block Interaction (Critical Change)

**Old API (1.20.4):**
```java
public ActionResult onUse(BlockState state, World world, BlockPos pos,
    PlayerEntity player, Hand hand, BlockHitResult hit)
```

**New API (1.21.1):**
```java
protected ActionResult onUse(BlockState state, World world, BlockPos pos,
    PlayerEntity player, BlockHitResult hit)
```

**Files Modified:**
- `src/main/java/fr/adrien1106/reframed/block/ReFramedBlock.java`
- `src/main/java/fr/adrien1106/reframed/block/ReFramedDoubleBlock.java`
- `src/main/java/fr/adrien1106/reframed/block/ReFramedDoorBlock.java`
- `src/main/java/fr/adrien1106/reframed/block/ReFramedTrapdoorBlock.java`
- `src/main/java/fr/adrien1106/reframed/block/ReFramedFenceBlock.java`
- `src/main/java/fr/adrien1106/reframed/block/ReFramedButtonBlock.java`
- And several other block classes

**Key Changes:**
- **Removed `Hand` parameter** - This was the critical change
- Changed visibility from `public` to `protected`
- Implemented hand-loop workaround in helper methods:
  ```java
  for (Hand hand : Hand.values()) {
      ActionResult result = BlockHelper.useCamo(state, world, pos, player, hand, hit, theme_index);
      if (result.isAccepted()) return result;
  }
  ```

### 5. Identifier API

**Old API (1.20.4):**
```java
new Identifier(namespace, path)
new Identifier("minecraft:stone")
```

**New API (1.21.1):**
```java
Identifier.of(namespace, path)
Identifier.ofVanilla(path)
```

**Files Modified:**
- All files using Identifier constructor

### 6. Data Generation

**Old API (1.20.4):**
```java
public GLanguage(FabricDataOutput dataOutput)
```

**New API (1.21.1):**
```java
protected GLanguage(FabricDataOutput dataOutput,
    CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup)
```

**Files Modified:**
- `src/main/java/fr/adrien1106/reframed/generator/GLanguage.java`
- `src/main/java/fr/adrien1106/reframed/generator/GBlockLoot.java`
- `src/main/java/fr/adrien1106/reframed/generator/GRecipe.java`
- `src/main/java/fr/adrien1106/reframed/generator/GAdvancement.java`
- `src/main/java/fr/adrien1106/reframed/generator/GBlockstate.java`

### 7. Tooltip API

**Old API (1.20.4):**
```java
appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context)
```

**New API (1.21.1):**
```java
appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type)
```

**Files Modified:**
- Item classes with custom tooltips

### 8. Other Minor API Changes

- `WeightedCollection.getData()` → `.data()`
- `UnbakedModel.bake()` signature changed (removed `Identifier` parameter)

## Mixin Configuration

### src/main/resources/reframed.mixins.json
- Removed all `_compat.*` mixin entries that referenced disabled compatibility modules
- Kept only core mixins:
  - MinecraftAccessor
  - Model mixins
  - Render mixins
  - Sound mixins
  - BlockItemMixin (updated for Component API)

## Compatibility Modules (Deferred)

The following compatibility integrations have been moved to `src/main/java/fr/adrien1106/reframed/mixin/compat_disabled/` and will be re-enabled in a future update:

- **Continuity** (connected textures) - Requires updated version and mixin updates
- **Sodium** - Render optimization compatibility
- **Indium** - Fabric rendering API compatibility
- **Athena** - Connected textures compatibility
- **Axiom** - World editor compatibility

## Resource Generation

Ran `./gradlew runDatagen` to generate:
- Language files (`en_us.json`)
- Block states
- Block models
- Item models
- Loot tables
- Recipes
- Advancements

Total: 225 resource files generated

## Testing Status

### ✅ Tested and Working
- Block placement
- Camo application (right-click with block)
- Light emission toggle (glowstone dust)
- Redstone emission toggle (redstone torch)
- Collision toggle (popped chorus fruit)
- Screwdriver rotation (on directional blocks like logs)
- Blueprint copying
- Hammer functionality
- All basic frame block types
- Double frame blocks

### ⏳ Not Yet Tested
- Connected textures (Continuity integration disabled)
- Axiom integration
- Chipped integration

## Build Instructions

This port requires **Java 21**. Use the following command to build:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew build
```

Or for a clean build:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew clean build
```

The compiled JAR will be located at: `build/libs/ReFramed-1.6.6.jar`

## Known Issues

None currently identified in core functionality.

## Future Work

1. **Continuity Integration**: Re-enable connected textures support
   - Find compatible Continuity version for 1.21.1
   - Update compat mixins for new APIs
   - Move mixins from `compat_disabled` back to `compat`
   - Update mixin configuration

2. **Axiom Integration**: Re-enable world editor support
   - Wait for Axiom 1.21.1 release
   - Update compatibility code

3. **Chipped Integration**: Re-enable block variant support
   - Find compatible Chipped version for 1.21.1

## Credits

- Original mod by adrien1106
- 1.21.1 port assistance provided
- Testing by Senne

## Notes for Code Review

All debug logging added during the porting process has been removed. The code is clean and ready for integration. The main architectural changes are:

1. Model loading completely rewritten for new plugin system
2. Component data system replaces NBT throughout
3. Block interaction signature change (Hand parameter removed) required careful handling across all interactive blocks
4. All deprecated APIs replaced with 1.21.1 equivalents

The mod is fully functional in 1.21.1 with all core features working as expected.
