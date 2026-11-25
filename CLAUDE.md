# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos

Toolchain: o JDK padrão do sistema é 25, que não foi testado com Gradle 8.8. Use o JDK 21 do cache do Gradle:

```bash
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
export PATH=$JAVA_HOME/bin:$PATH
```

- Build completo (compila os dois source sets, roda testes, gera `build/libs/selection-tools-0.4.0.jar` e o `-sources.jar`): `./gradlew build`
- Compilar sem testes: `./gradlew compileJava compileClientJava`
- Todos os testes: `./gradlew test`
- Uma classe: `./gradlew test --tests 'dev.buildzones.client.radial.RadialLayoutTest'`
- Um método: `./gradlew test --tests 'dev.buildzones.client.selection.SelectionDraftTest.boundsFollowTheAimedCorner'`
- Cliente de desenvolvimento: `./gradlew runClient`. Abre uma janela de jogo no `DISPLAY`. Gera `run/` e `logs/` na raiz, que são artefatos de teste.
- Não há task de lint nem checkstyle configurados.

Relatório dos testes: `build/test-results/test/*.xml`.

## Arquitetura

Mod Fabric 1.20.1 com Yarn 1.20.1+build.10. Versões em `gradle.properties`.

**Source sets divididos (Loom `splitEnvironmentSourceSets`):**
- `src/main`: código comum. Entrypoint `BuildZones`, canais de rede e a substituição de blocos. Roda no servidor dedicado, então **nunca pode referenciar `net.minecraft.client.*`**.
- `src/client`: tudo sob `dev.buildzones.client`. Pode depender de `src/main`, nunca o contrário.
- `build.gradle` põe as classes de `main` e `client` no classpath dos testes. Por isso os testes de geometria funcionam sem bootstrap do Minecraft.

**Fluxo de entrada (cliente):**
Callback Fabric (`MouseInputHandler`: UseBlock), mixins `MouseButtonMixin` (cliques da seleção) e `KeyboardInputMixin` (tecla de modificador em `Keyboard.onKey`) → `SelectionInputController` → `SelectionStateMachine` → `SelectionDraft` / `SelectionManager` → `SelectionStorage`. A tecla de modificador é um `KeyBinding` (`ModKeys`), configurável em Controles.
- `SelectionInputController` é o único lugar com a ordem de prioridade das entradas. Os callbacks são registrados no cliente, mas também disparam no servidor integrado do singleplayer, por isso checam `world.isClient`.
- `SelectionStateMachine` é a única autoridade sobre IDLE / ARMED / WAITING_SECOND_CORNER. Não conhece teclado nem render.
- Toque em ALT (apertar e soltar, sem outra tecla no meio): abre o `RadialMenuScreen` ou para a ação em curso. `ModifierKeyState` consulta o GLFW direto. ESC só é consumido com escolha ou rascunho. Fora disso o vanilla segue. A roda do mouse nunca é consumida.

**Contexto (perfil + dimensão):** o `tick` do controller detecta troca de `ClientWorld` e chama `manager.setContext(...)`. O perfil é um hash SHA-256 truncado do caminho do save (singleplayer) ou do endereço do servidor (`WorldProfileKey`). Persistência em `config/buildzones/worlds/<perfil>/<dimensão>.json`, com escrita em `.tmp` e troca atômica.

**Smart Replacement (cliente → servidor):**
- Cliente: `ALT + RMB` gera `ReplaceBlockRequest` (pos, side, hand). Sem o canal no servidor (`ClientPlayNetworking.canSend` falso), devolve PASS e avisa.
- Servidor: `BuildZonesNetworking` lê o pacote na thread de rede e agenda para a thread do servidor. `ReplaceBlockValidator` refaz o raycast, checa alcance, permissão e alvo. `ReplaceBlockService` calcula o novo estado e só então chama `setBlockState`, que é o ponto de não retorno. Drops e consumo do item acontecem depois.
- Resultado: `ReplaceBlockResult` volta ao cliente. `ReplacementResult.key()` é a chave de lang que o HUD mostra.
- Contexto de colocação: `PlacementContextFactory` subclassa `ItemPlacementContext` e sobrescreve `getBlockPos()`. Não há Accessor no `placementPos` privado. Decisão e limites em `docs/REPLACEMENT_COMPATIBILITY.md`.

**Renderização:**
- `GhostRenderLayers` define duas camadas translúcidas, sem escrita de profundidade e sem culling. `MultiPhaseParameters` é `protected` no Yarn 1.20.1, por isso a classe estende `RenderLayer` só para acessar o builder. Não é instanciada.
- `SelectionWorldRenderer` desenha em duas passadas: todas as faces, depois todas as arestas. O `VertexConsumerProvider.Immediate` encerra a camada anterior quando outra é pedida, então cada buffer é obtido uma vez por passada. Pegar buffers antecipadamente gera `Not filled all elements of the vertex`.
- Cada face sai uma vez. Emitir de novo com ordem invertida triangula em outra diagonal e gera listras. `SelectionGeometryTest` exige 24 vértices por caixa e falha se houver duplicação.
- Vértices são em coordenada de mundo menos a posição da câmera. A caixa de uma região vai de `min` até `max + 1`.
- Animações usam `Util.getMeasuringTimeMs()`. A caixa da prévia (`AnimatedBounds`) converge por `dt`, não por frame. Pulso e fantasmas ficam em `AnimationController`.

**Estado e dados:**
- `SelectionDraft`: A fixo, B segue a mira com Y limitado ao mundo. Não há mais scroll nem eixo.
- `SelectionManager`: lista ordenada das regiões. A ordem é o histórico, então undo remove o último. Cada mutação grava no disco. Não há estado global compartilhado entre mundos.
- Valores de comportamento em `BuildZonesConfig`, cores e alphas em `RenderStyle`.
- Textos em `src/client/resources/assets/buildzones/lang/` (`buildzones.hud.*`, `buildzones.radial.*`, `buildzones.area.*` e `buildzones.replace.*`).

## Armadilhas

- Os alvos dos mixins usam nomes Yarn. Para conferir assinaturas, use `javap -p` no jar `minecraft-clientOnly-*1.20.1*.jar` dentro de `.gradle/loom-cache`.
- `AttackBlockCallback` só dispara com o cursor sobre um bloco; por isso os cliques da seleção vêm de `MouseButtonMixin`, que também funciona no ar.
- `KeyBinding` do 1.20.1 guarda uma só tecla, sem combinação. O modificador é uma tecla; os atalhos checam se ela está segurada.
- O único teste de render é o de geometria (contagem de vértices). Nada testa visual. Menu radial e painel também: validação é manual, em jogo (ver `docs/TEST_PLAN.md`).
- O jar é para instância Fabric 1.20.1 com Fabric API 0.92.12+1.20.1. Não serve para instâncias NeoForge.

## Documentação existente

`README.md` (uso e compilação), `docs/CONTROLS.md`, `docs/ARCHITECTURE.md`, `docs/RENDERING_NOTES.md`, `docs/REPLACEMENT_COMPATIBILITY.md`, `docs/TEST_PLAN.md`. Se mudar controles ou arquitetura, atualize esses arquivos junto.
