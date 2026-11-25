# Smart Replacement: estratégia e compatibilidade

## Decisão do spike

O plano pedia escolher entre `BlockItem.place(context)` e uma implementação controlada. A escolha foi a controlada:

1. `Block.getPlacementState(ctx)` calcula o novo estado (orientação, facing, etc.).
2. `newState.canPlaceAt` e `World.canPlace` (só entidades) validam o encaixe.
3. `World.setBlockState` faz a troca. Se retornar falso, nada mudou.
4. Depois da troca: drops do bloco antigo, `onPlaced`, som, evento de jogo e consumo do item.

Motivo para não usar `BlockItem.place` direto: ele deslocaria a posição para o vizinho quando o alvo é sólido. Corrigir isso exigiria Accessor no campo privado `placementPos`. Em vez disso, `PlacementContextFactory` cria uma subclasse de `ItemPlacementContext` que sobrescreve `getBlockPos()`, `canPlace()` e `canReplaceExisting()`. Nenhum Accessor é usado.

**Status:** a decisão foi tomada por leitura do código-fonte de 1.20.1 e compilação. O comportamento em jogo (orientação de escadas, troncos, fornalhas, sons, drops) ainda precisa ser confirmado no teste manual descrito em `TEST_PLAN.md`.

## Ordem de segurança

Validações antes de qualquer mutação: espectador, modo aventura, item de bloco, altura do mundo, chunk carregado, alcance, raycast do servidor igual ao alvo, permissão de spawn/proteção, alvo não protegido, substituto não multiparte.

## Política

| Regra | Valor |
|---|---|
| Survival | Consome exatamente 1 item, drops normais do bloco antigo |
| Creative | Não consome, não dropa |
| Adventure | Recusado |
| Alvo com BlockEntity | Recusado |
| Alvo com líquido | Recusado |
| Alvo inquebrável (hardness < 0) | Recusado |
| Portas, camas, plantas de duas metades (como alvo ou substituto) | Recusado |
| Mesmo bloco | Recusado |

## Blocos que ficam fora da V1

Baús, fornalhas, placas com texto, portas, camas, plantas altas e qualquer bloco com `BlockEntity`. Blocos de outros mods não têm garantia.
