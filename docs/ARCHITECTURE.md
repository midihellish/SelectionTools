# Arquitetura do Selection Tools

## Camadas

1. **Comum (`src/main`)**: entrypoint `BuildZones`, canais de rede e o serviço de substituição. Roda no servidor dedicado sem classes de cliente.
2. **Cliente (`src/client`)**: entrada, máquina de estados, armazenamento, renderização, HUD e animações.
3. **Mixins (`src/client/.../mixin`)**: dois, ambos client-only. `MouseScrollMixin` e `KeyboardInputMixin`.

## Fluxo da seleção

```
Mouse / teclado
   -> callbacks Fabric (AttackBlock, UseBlock, UseItem) e mixins (onMouseScroll, onKey)
   -> SelectionInputController   (prioridade de entradas, um só lugar)
   -> SelectionStateMachine      (única autoridade sobre IDLE / WAITING / PREVIEW)
        -> SelectionDraft        (rascunho: A fixo, B move no eixo ativo)
        -> SelectionManager      (regiões confirmadas, ordem = histórico)
             -> SelectionStorage (JSON por perfil de mundo e dimensão)
   -> SelectionWorldRenderer     (lê estado, desenha; não altera dados)
   -> SelectionHud               (painel e toasts)
```

## Smart Replacement

```
Cliente: ALT + botão direito -> UseBlockCallback -> ReplaceBlockRequest -> servidor
Servidor: validação (alcance, raycast, permissões, alvo) -> ReplaceBlockService
          -> troca só com o substituto validado -> drops/consumo -> ReplaceBlockResult
Cliente: HUD mostra o resultado
```

O cliente nunca altera blocos. Sem o mod no servidor, `ClientPlayNetworking.canSend` é falso, o HUD avisa e o clique volta ao vanilla.

## Área (modo área e operações em massa)

```
Cliente: ALT + B / ALT + C / ALT + V / CTRL + ALT + Z / RMB (segurado)
   -> SelectionInputController  (prioridade; delega ao AreaModeController)
   -> AreaModeController        (alvo, fantasmas, contagem, confirmação, repetição)
        -> SurvivalBrushPlanner (puro: quais células são fantasma, raio do olhar)
        -> sync_region / area_op_request / area_place_request
Servidor: BuildZonesNetworking -> server.execute
        -> ServerRegionRegistry  (região ativa por jogador, validada na entrada)
        -> AreaOperationValidator (puro: ordem das recusas)
        -> AreaOperationService  (jobs em fatias por tick, undo, colocação)
             -> ReplacementPolicy (mesmas regras de proteção da substituição)
             -> ReplaceBlockService.finishPlacement (hook, som, evento, consumo)
Resultado: area_result -> SelectionInputController.onAreaResult -> SelectionHud
```

- O cliente nunca altera blocos. Ele lê o mundo para desenhar e contar, e manda pedidos.
- O servidor só aceita o que bate com a região que ele guardou. Divergência responde `REGION_UNKNOWN` e o cliente reenvia a região ativa.
- Um trabalho em massa por jogador. Cada tick processa até 4096 células, com `NOTIFY_LISTENERS` e sem drops.
- Regras puras (`AreaBox`, `AreaOperationValidator`, `AreaUndoBuffer`, `AreaJobRunner`, `SurvivalBrushPlanner`) não precisam de bootstrap e têm testes.
- Limites compartilhados em `AreaLimits` (common). `BuildZonesConfig` só guarda valores de comportamento do cliente.

## Separação de responsabilidades

| Classe | Papel | Não faz |
|---|---|---|
| `SelectionBounds` | Min/max, dimensões, volume em `long`, caixa de render | Input, render |
| `SelectionDraft` | Rascunho, direção de crescimento, acumulador de scroll | Persistência |
| `AxisResolver` | Eixo dominante com histerese | Qualquer outra coisa |
| `SelectionStateMachine` | Transições de estado | Teclado, render |
| `SelectionManager` | Coleção ordenada, undo, clear, gravação | Input |
| `SelectionStorage` | Leitura e gravação atômica | Regras de seleção |
| `SelectionWorldRenderer` | Desenho por região | Alterar dados |
| `AreaModeController` | Modo área, alvo, contagem, confirmação, colocação repetida | Alterar blocos |
| `AreaOperationService` | Região do servidor, jobs, undo, colocação | Decidir a ordem das recusas (isso é do validator) |
| `AreaGhostRenderer` | Fantasmas e contornos de remoção | Alterar dados |

## Persistência

`config/buildzones/worlds/<perfil>/<dimensão>.json`. O perfil é um hash SHA-256 truncado do caminho do save (singleplayer) ou do endereço do servidor (multiplayer). Escrita em `.tmp` seguida de troca atômica.
