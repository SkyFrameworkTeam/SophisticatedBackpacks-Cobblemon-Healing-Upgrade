# Sophisticated Backpacks: Cobblemon Healing Upgrade (Fabric 1.21.1)

Mejora para **Sophisticated Backpacks** que cura el equipo Pokémon del jugador desde la mochila, como la
máquina de curación de **Cobblemon**.

- Mod id: `healingupgrade`
- Ítem: `healingupgrade:healing_upgrade`. Aparece en la pestaña creativa de Sophisticated Backpacks, justo después de la mejora de herrería.
- Tag: `#sophisticatedbackpacks:upgrade` (`data/sophisticatedbackpacks/tags/item/upgrade.json`, con `replace: false`). La ranura de mejoras de la mochila solo acepta ítems con este tag.
- Textura: mismo marco y paleta que `upgrade_base` de Sophisticated Backpacks, con una cruz en el marrón oscuro de los iconos (`#3D1C10`).
- Jar: `build/libs/sophisticatedbackpacks-cobblemon-healing-upgrade-1.1.0-FABRIC.jar`

## Versiones

Compilado contra los jars exactos del modpack Cobbleverse (copiados en `libs/`):

| Mod | Versión |
|---|---|
| Minecraft / Java | 1.21.1 / 21 |
| Fabric Loader / Fabric API | 0.17.2 / 0.116.7+1.21.1 |
| Cobblemon | 1.7.1+1.21.1 |
| Sophisticated Backpacks | 1.21.1-3.23.4.3.106 |
| Sophisticated Core | 1.21.1-1.2.9.20.164 |

porting_lib (incluido `DeferredHolder`/`lazy_registration`) y Team Reborn Energy **van incluidos dentro de
sophisticatedcore** (jar-in-jar), así que no hay que instalarlos aparte. `libs/nested/` contiene esos jars
extraídos únicamente para que el compilador resuelva los supertipos (`ItemStackHandler`, etc.).

## Instalación

Copia el jar en la carpeta `mods/` **del servidor y de los clientes**: el cliente necesita el ítem, su
textura y la pestaña de la mochila.

## Uso

1. Fabrica la mejora y colócala en una ranura de mejoras de la mochila.
2. Abre la pestaña de la mejora (icono del ítem en el lateral) y pulsa **Curar equipo**.
3. El servidor comprueba las restricciones, cura el equipo y te avisa por el chat.
4. Mientras dura el cooldown, el botón se ve gris y muestra la cuenta atrás (**Espera 9:42**). Al pulsarlo no
   hace nada. El tiempo lo calcula el servidor y se envía al abrir la mochila y después de cada intento de curar.

## Configuración: `config/healingupgrade.json`

Se crea al arrancar. Para recargarla sin reiniciar:
- `/reload` relee **todo**, incluida la receta.
- `/healingupgrade reload` (nivel de permiso 2) relee todo **menos** la receta, que solo cambia con `/reload`.

```json
{
  "cooldownSeconds": 600,
  "cost": {
    "enabled": false,
    "item": "cobblemon:super_potion",
    "count": 1
  },
  "playEffects": true,
  "allowInBattle": false,
  "allowPlacedBackpacks": false,
  "recipe": {
    "pattern": ["OMO", "HBC", " D "],
    "key": {
      "O": "#c:ingots/gold",
      "M": "cobblemon:healing_machine",
      "H": "cobblemon:hyper_potion",
      "B": "sophisticatedbackpacks:upgrade_base",
      "C": "cobblemon:full_heal",
      "D": "#c:gems/diamond"
    },
    "count": 1
  }
}
```

| Campo | Significado |
|---|---|
| `cooldownSeconds` | Segundos entre curaciones, por jugador (por defecto 600 = 10 min). `0` lo desactiva. Se muestra en el botón. Se guarda en memoria, así que se reinicia al reiniciar el servidor. |
| `cost.enabled` | Si es `true`, cada curación consume `count` unidades de `item` **del inventario de la mochila**. |
| `cost.item` / `cost.count` | ID del ítem (por ejemplo `cobblemon:potion` o `minecraft:golden_apple`) y la cantidad. |
| `playEffects` | Reproduce el sonido de la máquina de curación (`CobblemonSounds.HEALING_MACHINE_ACTIVE`) y lanza partículas de corazones alrededor del jugador. |
| `allowInBattle` | Igual que la máquina: por defecto no se puede curar en combate. |
| `allowPlacedBackpacks` | Las mochilas colocadas como bloque no tienen dueño. Por defecto solo funciona con una mochila que lleves encima. |
| `recipe.pattern` | Filas de la mesa de crafteo, de 1 a 3, todas de la misma longitud. Un espacio es una casilla vacía. |
| `recipe.key` | Letra → ingrediente: un ID de ítem (`"cobblemon:revive"`) o un tag con `#` (`"#c:ingots/iron"`). Cada letra del patrón debe estar aquí y cada letra de aquí debe aparecer en el patrón. |
| `recipe.count` | Cuántas mejoras da la receta. |

Orden de las comprobaciones: dueño de la mochila → combate → ya curándose en una máquina → equipo vacío →
equipo ya curado → cooldown → coste. El coste y el cooldown solo se aplican si la curación se hace de verdad.

## Receta

Se configura en la sección `recipe` del JSON de arriba. Por defecto:

```
Lingote de oro   Máquina de Curación   Lingote de oro
Hiperpoción      Base de mejora        Cura Total
                 Diamante
```

La Hiperpoción cura PS, la Cura Total (`cobblemon:full_heal`) cura estados y la máquina aporta curar al equipo
entero. Para el oro y el diamante se usan los tags `#c:ingots/gold` y `#c:gems/diamond`, así que valen también
los de otros mods.

Cómo funciona: `data/healingupgrade/recipe/healing_upgrade.json` solo contiene
`{"type": "healingupgrade:config_shaped"}`. Ese tipo de receta lee la config y crea una receta con forma
**vanilla**, así que el libro de recetas, REI y JEI la muestran con normalidad. Si la config tiene un error (un
ítem inexistente, una letra sin definir…), el log muestra
`Receta de config/healingupgrade.json: ...` y solo se omite esa receta. El servidor sigue funcionando.

Un datapack con un archivo en esa misma ruta sigue pudiendo sustituirla por una receta JSON normal.

## Seguridad: solo en el servidor

- El botón del cliente solo envía una petición mediante el canal que ya tiene Sophisticated Core
  (`SyncContainerClientDataPayload`). Ese canal entrega el paquete únicamente al menú que tiene abierto el
  jugador que lo envía.
- `PartyHealService` (solo servidor) vuelve a comprobar que el menú abierto es un `BackpackContainer`, que la
  mejora pertenece a ese menú y que el contexto es una mochila del propio jugador (`ITEM_BACKPACK` /
  `ITEM_SUB_BACKPACK`). Rechaza `ANOTHER_PLAYER_*` (abrir la mochila de otro jugador) y, salvo que se
  configure lo contrario, `BLOCK_*`.
- La mejora solo se puede poner en mochilas (una por mochila), no en almacenes de Sophisticated Storage.

## API utilizada (verificada decompilando los jars del modpack)

- **Sophisticated Core:** `UpgradeItemBase`, `UpgradeWrapperBase`, `UpgradeContainerBase`, `UpgradeContainerType`,
  `UpgradeContainerRegistry.register(...)`, `UpgradeGuiManager.registerTab(...)` (cliente), `UpgradeSettingsTab`,
  `ButtonBase`, `InventoryHelper.extractFromInventory(...)`.
- **Sophisticated Backpacks:** `BackpackContainer#getBackpackContext().getType()`.
- **Cobblemon:** `Cobblemon.INSTANCE.getStorage().getParty(ServerPlayer)` → `PlayerPartyStore.heal()`,
  `Pokemon.canBeHealed()`, `PlayerExtensionsKt.isInBattle(...)`,
  `HealingMachineBlockEntity.Companion.isUsingHealer(...)`.

No hay mixins ni access wideners.

## Compilar

```
./gradlew build
```

Los jars de terceros **no están en el repositorio** (no se redistribuyen). Antes de compilar, copia en `libs/`:

- `Cobblemon-fabric-1.7.1+1.21.1.jar`
- `sophisticatedbackpacks-1.21.1-3.23.4.3.106.jar`
- `sophisticatedcore-1.21.1-1.2.9.20.164.jar`
- `ForgeConfigAPIPort-v21.1.6-1.21.1-Fabric.jar`

y en `libs/nested/` el contenido de `META-INF/jars/` del jar de sophisticatedcore (porting_lib y Team Reborn
Energy). Si usas otras versiones, cambia los nombres en `build.gradle`.

## Licencia

[GPL-3.0](LICENSE). Este mod extiende Sophisticated Backpacks y Sophisticated Core (GPL-3.0) y su textura sigue
el estilo de `upgrade_base` de Sophisticated Backpacks. Cobblemon es MPL-2.0.

## Pruebas

`./gradlew runServer` no sirve con este conjunto de mods: Cobblemon no arranca en desarrollo con los mappings de
Mojang. Para probar, monta un servidor Fabric 1.21.1 limpio con estos jars en `mods/`: fabric-api,
fabric-language-kotlin, ForgeConfigAPIPort, Cobblemon, sophisticatedcore, sophisticatedbackpacks y este mod.

Probado así:
- El servidor arranca con el mod.
- El ítem queda registrado.
- La receta carga sin errores.
- Se genera `config/healingupgrade.json`.
- `/healingupgrade reload` funciona.

Falta probar en una partida real con un cliente: la pestaña, el botón y la curación.
