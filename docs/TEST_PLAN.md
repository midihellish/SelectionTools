# Plano de testes

## Automáticos (`./gradlew test`)

68 testes, todos passando na última execução.

| Classe | Cobre |
|---|---|
| `SelectionBoundsTest` | Min/max invertidos, inclusividade, exemplo 21×17×16 = 5.712, `long` sem overflow, caixa de render |
| `SelectionDraftTest` | Scroll +/-, âncora fixa, eixo único, mínimo 1, direção mantida no mínimo, limite de Y, acumulador fracionário |
| `AxisResolverTest` | Eixo dominante, diagonal com histerese, troca clara, oscilação |
| `SelectionStateMachineTest` | Fluxo de estados, ESC só no rascunho, undo, clear, isolamento entre mundos, persistência entre sessões, sem contexto |
| `SelectionCodecTest` | Round-trip JSON, arquivo corrompido, entradas inválidas, versão de schema desconhecida, cores hex |
| `AreaBoxTest` | Normalização de cantos, volume, cada célula visitada uma vez, limite de dimensão |
| `AreaOperationValidatorTest` | Ordem das recusas em operação em massa, desfazer e colocação; limite de volume exato; ritmo de 80 ms |
| `AreaUndoBufferTest` | Ordem LIFO, expulsão do lote mais antigo no teto, lote maior que o teto recusado |
| `AreaJobRunnerTest` | Fatia nunca passa do orçamento, cobertura completa em várias fatias |
| `SurvivalBrushPlannerTest` | Só células diferentes do alvo, raio e camada, teto de fantasmas, raio do olhar para em bloco sólido e fora do alcance |

## Build e carregamento

- `./gradlew build` gera `build/libs/selection-tools-0.2.0.jar` e o `-sources.jar`.
- Código comum sem referências a `net.minecraft.client` (checado com `javap` nas classes de `main`).
- Alvos dos mixins conferidos contra o Minecraft 1.20.1: `Keyboard.onKey(JIIII)`, `Mouse.onMouseScroll(JDD)`.

## Manuais (em jogo, 1.20.1 com Fabric API)

Marque o resultado de cada item.

- [ ] `ALT + clique esquerdo` define A e não minera.
- [ ] Segundo `ALT + clique esquerdo` mostra a caixa.
- [ ] Scroll ajusta X, Y e Z conforme a câmera; a âncora não se move.
- [ ] Scroll para baixo nunca passa de 1 bloco.
- [ ] Botão direito confirma sem abrir baú nem colocar bloco.
- [ ] Três regiões sobrepostas continuam visíveis e independentes.
- [ ] `ALT + Z` remove só a última; `ALT + F` limpa o mundo atual.
- [ ] `ESC` cancela só o rascunho.
- [ ] Com inventário aberto, nada é capturado.
- [ ] Fora da prévia, scroll troca a hotbar.
- [ ] Fechar e reabrir o mundo mantém as regiões.
- [ ] Outro mundo ou dimensão não mostra nem apaga essas regiões.
- [ ] Fancy, Fabulous e Fast sem flicker nem alpha acumulado.
- [ ] Singleplayer Survival: substituição consome 1 item e dropa o bloco antigo.
- [ ] Singleplayer Creative: substituição não consome nem dropa.
- [ ] Servidor sem o mod: HUD avisa e o clique volta ao vanilla.
- [ ] Alvo com BlockEntity (baú) é recusado e nada muda.
- [ ] Escadas, troncos e blocos com facing mantêm a orientação.

### Modo área (em jogo, servidor integrado e servidor dedicado)

Criativo:
- [ ] `ALT + B` com região confirmada e bloco na mão liga o modo e mostra o alvo; sem região ou sem bloco, avisa.
- [ ] `ALT + C` na primeira pressão mostra a contagem; a segunda, dentro de 5 s, limpa 16×16×16 e avisa "N alterado(s), M pulado(s)".
- [ ] `ALT + V` preenche só ar e blocos substituíveis; não apaga sólidos.
- [ ] Operação de 64×64×64 (262.144) passa; 65×64×64 é recusada no cliente com aviso.
- [ ] `CTRL + ALT + Z` desfaz a última operação; uma segunda chamada sem operação avisa "nada para desfazer".
- [ ] Durante uma operação longa, segunda operação recebe "já existe uma operação em andamento".
- [ ] Baús, fornalhas e líquidos dentro da área não mudam; aparecem como pulados.
- [ ] Trocar de mundo desliga o modo área.

Sobrevivência:
- [ ] Fantasmas aparecem só nas células que diferem do alvo, dentro de 8 blocos e 2 camadas.
- [ ] Botão direito coloca no fantasma mirado, sem precisar de bloco vizinho; segurado, repete (~120 ms).
- [ ] Cada colocação gasta 1 item e toca o som do bloco; quebrar continua vanilla (drops e ferramenta).
- [ ] Fora da região, o servidor recusa e nada muda.
- [ ] Trocar o bloco na mão durante o modo não coloca nem gera aviso a cada repetição.
- [ ] Atrás de um bloco sólido, o fantasma não é escolhido.
- [ ] ALT + Z ou ALT + F na última região: o modo área acompanha a região nova ou desliga se não houver mais nenhuma.

Sem o mod no servidor:
- [ ] `ALT + B`, `ALT + C`, `ALT + V` e `CTRL + ALT + Z` avisam "Este servidor não tem o Selection Tools" e não mudam nada.
- [ ] Clique direito volta ao vanilla.

Limites e dessincronia:
- [ ] Região de 513 de largura não é aceita pelo servidor.
- [ ] Região trocada no servidor (reinício) responde "região não sincronizada" e reenvia a região ativa.
