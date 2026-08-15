# Paper 1.21.11 Mojang-Mapped NMS API Reference

Paper 1.21.11 (build 130) uses **Mojang-mapped** NMS (NOT obfuscated like 1.16.5). Compile classpath:
`libs/paper-api-1.21.11.jar` (org.bukkit) + `libs/paper-server-1.21.11.jar` (NMS net.minecraft.* + org.bukkit.craftbukkit)

## Key signatures (verified via javap)

### ServerPlayer (net.minecraft.server.level.ServerPlayer)
- Constructor: `ServerPlayer(MinecraftServer, ServerLevel, GameProfile, ClientInformation)`
- `getBukkitEntity()` -> CraftPlayer
- public field: `ServerGamePacketListenerImpl connection`
- public field: `int lastSentExp`
- `hurtServer(ServerLevel, DamageSource, float)` -> boolean (NOTE: not hurt())
- `drop(ItemStack, boolean, boolean, boolean, Consumer<Item>)` -> ItemEntity
- `drop(boolean)` -> boolean (active hand item)
- `getScoreboard()`, `getTeam()`, `setExperiencePoints(int)`, `setExperienceLevels(int)`, `giveExperienceLevels(int)`
- `initMenu(AbstractContainerMenu)`, `initInventoryMenu()`
- `tick()`, `doTick()`
- `resetFallDistance()`, `spawnIn(ServerLevel)`
- `setCamera(Entity)` (check)
- `getInventory()` (from Player)
- `connection` field is `ServerGamePacketListenerImpl` (Mojang name for PlayerConnection)

### ServerGamePacketListenerImpl (net.minecraft.server.network)
- Constructor: `ServerGamePacketListenerImpl(MinecraftServer, Connection, ServerPlayer, CommonListenerCookie)`
- `send(Packet)` (inherited from ServerCommonPacketListenerImpl / ServerPacketListener)
- `disconnect(Component)` -> void (inherited)

### CommonListenerCookie (net.minecraft.server.network)
- `createInitial(GameProfile, boolean)` -> CommonListenerCookie

### ClientInformation (net.minecraft.server.level)
- `createDefault()` -> ClientInformation

### MinecraftServer (net.minecraft.server)
- static `getServer()` -> MinecraftServer
- `getPlayerList()` -> PlayerList
- `getLevel(ResourceKey<Level>)` -> ServerLevel (use `Level.OVERWORLD`)
- `getAllLevels()` -> Iterable<ServerLevel>
- `getServerDirectory()`, `isStopped()`, `getCommands()` -> Commands
- `getScoreboard()`

### PlayerList (net.minecraft.server.players)
- `placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)` -> void (registers player, broadcasts ADD)
- `remove(ServerPlayer)` -> Component
- `remove(ServerPlayer, Component)` -> Component
- `getPlayer(String)` -> ServerPlayer, `getPlayer(UUID)` -> ServerPlayer
- `getPlayerByName(String)` -> ServerPlayer
- `getPlayers()` -> List<ServerPlayer>, public field `List<ServerPlayer> players`
- `broadcastAll(Packet)`, `broadcastAll(Packet, Player)`, `broadcastAll(Packet, Level)`

### ServerLevel (net.minecraft.server.level)
- `addFreshEntity(Entity)` -> boolean
- `addFreshEntity(Entity, CreatureSpawnEvent.SpawnReason)` -> boolean
- `addWithUUID(Entity)` -> boolean
- `getBlockState(BlockPos)` (from Level)
- `setBlock(BlockPos, BlockState, int)` (from Level)
- `destroyBlockProgress(int, BlockPos, int)` -> void
- `playSound(...)` (from Level)
- `getChunkSource()` -> ServerChunkCache
- `getSeed()` -> long
- `getWorld()` -> CraftWorld (from Level)

### Level (net.minecraft.world.level)
- `getEntitiesOfClass(Class, AABB, Predicate)` -> List<T>
- `getBlockState(BlockPos)`, `setBlock(BlockPos, BlockState, int)`
- `playSound(Entity, BlockPos, SoundEvent, SoundSource, float, float)`
- `getWorld()` -> CraftWorld

### Entity (net.minecraft.world.entity)
- `setPos(double, double, double)`
- `moveTo(double, double, double, float, float)` (check exact)
- `teleportTo(double, double, double)`
- `setDeltaMovement(Vec3)` / `setDeltaMovement(double, double, double)`
- `setYRot(float)`, `setXRot(float)`
- `push(double, double, double)`, `push(Vec3)`, `push(Entity)`
- `hurtClient(DamageSource)` -> boolean (client-side only)
- `level()` -> Level, `getUUID()` -> UUID
- `kill(ServerLevel)`
- `isAlive()`, `discard()`, `tick()`
- `hurtServer(ServerLevel, DamageSource, float)` -> boolean

### LivingEntity (net.minecraft.world.entity)
- `getHealth()` -> float, `setHealth(float)`
- `getMaxHealth()` -> float
- `isDeadOrDying()` -> boolean
- `hurtServer(ServerLevel, DamageSource, float)` -> boolean
- `getMainHandItem()` -> ItemStack, `getOffhandItem()` -> ItemStack
- `getItemInHand(InteractionHand)` -> ItemStack
- `setItemInHand(InteractionHand, ItemStack)`
- `getItemBySlot(EquipmentSlot)` -> ItemStack
- `getAttribute(Holder<Attribute>)` -> AttributeInstance
- `getAttributeValue(Holder<Attribute>)` -> double
- `getArmorValue()` -> int
- `addEffect(MobEffectInstance)` -> boolean
- `getAttributes()` -> AttributeMap
- `getEffects()` (check)

### Player (net.minecraft.world.entity.player)
- `getInventory()` -> Inventory
- `getAbilities()` -> Abilities
- `getFoodData()` -> FoodData
- `getSlot(int)` -> SlotAccess
- `getItemInHand(InteractionHand)` (from LivingEntity)

### ItemStack (net.minecraft.world.item)
- `getItem()` -> Item
- `getDisplayName()` -> Component
- `getHoverName()` -> Component
- `copy()` -> ItemStack
- `isEmpty()` -> boolean
- `getMaxStackSize()` -> int
- `getDamageValue()` -> int, `setDamageValue(int)`
- `isDamageableItem()` -> boolean
- `getComponents()` (DataComponentMap)

### Packets (net.minecraft.network.protocol.game)
- `ClientboundPlayerInfoUpdatePacket(EnumSet<Action>, Collection<ServerPlayer>)`
- `ClientboundPlayerInfoUpdatePacket(Action, ServerPlayer)`
- `ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collection<ServerPlayer>)`
- `ClientboundPlayerInfoUpdatePacket.Action`: ADD_PLAYER, INITIALIZE_CHAT, UPDATE_GAME_MODE, UPDATE_LISTED, UPDATE_LATENCY, UPDATE_DISPLAY_NAME, UPDATE_LIST_ORDER, UPDATE_HAT
- `ClientboundPlayerInfoRemovePacket(List<UUID>)`
- `ClientboundAddPlayerPacket(ServerPlayer)` (entity spawn for players)
- `ClientboundAddEntityPacket` (generic entity spawn - check ctor)
- `ClientboundSetEntityDataPacket(int, List<SyntheticPacketData>)` (check)
- `ClientboundMoveEntityPacket.PosRot` etc.
- `ClientboundRotateHeadPacket(Entity, byte)`
- `ClientboundSetHealthPacket(float, int, float)`
- `ClientboundSetExperiencePacket` (check)
- `ClientboundEntityEventPacket(Entity, byte)`
- `ClientboundRemoveEntitiesPacket(int...)` (int ids)
- `ClientboundPlayerChatPacket` (chat; or use `ClientboundSystemChatPacket(Component, boolean)` for system messages)
- `ClientboundSystemChatPacket(Component, boolean)` -> system chat (actionbar=false)
- `ClientboundContainerSetSlotPacket`, `ClientboundSetEquipmentPacket` etc.

### DamageSource (net.minecraft.world.damagesource)
- `DamageSource(Registry<DamageType>, Holder<DamageType>)` (check)
- `serverLevel()` -> ServerLevel (from source)
- `DamageSources.generic()` etc. via `level.damageSources()`
- ServerLevel has `damageSources()` -> DamageSources (check)
- `DamageSources.playerAttack(Player)` / `mobAttack(LivingEntity)` etc.

### BlockPos (net.minecraft.core)
- `BlockPos(int, int, int)`
- `getX()`, `getY()`, `getZ()`
- `relative(Direction, int)`
- `clampYWithinHeight(int)`

### GameProfile (com.mojang.authlib)
- `GameProfile(UUID, String)` — authlib jar needed on classpath (bundled in paper-server)

### Connection (net.minecraft.network)
- For fake players: use `Connection` subclass with a closed/no-op channel, or use ProtocolInfo-based approach.
  - In 1.21.x there is `Connection.connectToServer(...)`, and server-side you can construct `Connection(ProtocolInfo)` — check
  - Paper adds `io.papermc.paper.network.Connection` helpers — check for `Connection.createForPlayer` / connection setup

### ServerCommonPacketListenerImpl (net.minecraft.server.network)
- `send(Packet)` -> void
- `disconnect(Component)` -> void

## Bukkit equivalents (still usable)
- `Bukkit.getScoreboardManager()`, `Bukkit.getServer().getScoreboard()`
- `player.getLocation()`, `player.teleport(Location)`
- `player.setItemInHand(ItemStack)` deprecated; use `player.getInventory().setItemInMainHand(...)`
- `player.getHealth()`, `player.setHealth(double)`
- `player.performCommand(String)` — Bukkit CommandSender.execute; to run as console use `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)`
- `Bukkit.getCommandMap().getKnownCommands()` -> Map<String, Command> (command enumeration for /help feature)
- `player.getEffectivePermissions()` for permission checks
