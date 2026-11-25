# Notas de renderização

## Camadas (`GhostRenderLayers`)

- Faces e arestas usam camadas próprias: translúcidas, sem escrita no depth buffer, sem culling, teste de profundidade `LEQUAL`.
- Efeito: a caixa é um overlay. Não esconde o que está atrás, não briga com as faces dos blocos (sem z-fighting) e caixas sobrepostas não piscam pela ordem de desenho.
- Terreno e construções na frente continuam ocultando a caixa, porque o teste de profundidade está ativo.
- `MultiPhaseParameters` é protegida no Yarn 1.20.1, então `GhostRenderLayers` estende `RenderLayer` só para acessar o builder. A classe nunca é instanciada.

## Geometria

- Cada face sai uma vez. Sem culling, o sentido do quad não importa.
- Epsilon de 0,005 bloco para as faces ficarem levemente fora do bloco.
- Custo fixo por caixa: 6 faces e 12 arestas, sem percorrer o volume.
- Distância máxima de render: 192 blocos (`BuildZonesConfig.RENDER_DISTANCE`).

## Ordem de desenho

Primeiro todas as faces (um buffer), depois todas as arestas (outro buffer). O `VertexConsumerProvider.Immediate` encerra a camada anterior quando outra é pedida, então cada buffer é obtido uma vez por passada. Isso evita o erro `Not filled all elements of the vertex`.

## Animações

- Relógio monotônico (`Util.getMeasuringTimeMs`) e `dt` limitado a 100 ms.
- A caixa da prévia converge por `1 - e^(-22·dt)`.

## Pontos a validar em jogo

- Fancy, Fabulous e Fast.
- Água e vidro na frente e atrás da seleção.
- Sodium e Iris, se instalados.
