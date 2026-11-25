# Plano de testes

## Automáticos (`./gradlew test`)

89 testes, todos passando na última execução.

| Classe | Cobre |
|---|---|
| `SelectionBoundsTest` | Min/max invertidos, inclusividade, exemplo 21×17×16 = 5.712, `long` sem overflow, caixa de render |
| `SelectionDraftTest` | Canto B começa no A, A nunca se move, caixa segue o B, Y limitado ao mundo |
| `SelectionStateMachineTest` | Escolha arma, primeiro clique marca A, segundo salva, mira move só o B, altura do mundo, confirmação no ar, ESC cancela só o rascunho ou a escolha, undo, clear, isolamento entre mundos, persistência entre sessões, sem contexto |
| `RadialLayoutTest` | Centro morto, fatias no sentido horário a partir do topo, diagonais, todo ângulo cai numa fatia |
| `PlacementScoreTest` | Proximidade manda sobre a mira; a mira desempata células quase à mesma distância; sem desconto atrás do olhar |
| `AreaShapeTest` | Contagem exata de volume (27), casca (26), arestas (20) num cubo 3×3×3; placa plana; célula fora da caixa |
| `SelectionCodecTest` | Round-trip JSON, arquivo corrompido, entradas inválidas, versão de schema desconhecida, cores hex |
| `AreaBoxTest` | Normalização de cantos, volume, cada célula visitada uma vez, limite de dimensão |
| `AreaOperationValidatorTest` | Ordem das recusas em operação em massa, desfazer e colocação; limite de volume exato; quebra do pincel; ritmo de 30 ms |
| `AreaUndoBufferTest` | Ordem LIFO, expulsão do lote mais antigo no teto, lote maior que o teto recusado |
| `AreaJobRunnerTest` | Fatia nunca passa do orçamento, cobertura completa em várias fatias |

## Build e carregamento

- `./gradlew build` gera `build/libs/selection-tools-<versão>.jar` e o `-sources.jar`.
- Código comum sem referências a `net.minecraft.client` (checado com `javap` nas classes de `main`).
- Alvos dos mixins conferidos contra o Minecraft 1.20.1: `Keyboard.onKey(JIIII)` e `Mouse.onMouseButton(JIII)`.
- `KeyBinding` do 1.20.1 guarda uma única tecla (`boundKey`), sem combinação. Por isso o modificador é uma tecla só e os atalhos o seguram.

## Manuais (em jogo, 1.20.1 com Fabric API)

Marque o resultado de cada item. Os automáticos não cobrem render, teclado nem rede.

Menu e toque:
- [ ] Tocar ALT (apertar e soltar) abre o menu radial com quatro fatias, sem pausar o jogo.
- [ ] Tocar ALT de novo com o menu aberto fecha.
- [ ] `1` a `4`, setas + Enter e clique escolhem a mesma fatia.
- [ ] ESC fecha o menu sem escolher.
- [ ] ALT + Z, ALT + F não abrem o menu.
- [ ] Trocar a tecla do modificador em Controles muda o toque, o painel e o texto do menu.
- [ ] Todas as quatro fatias aceitam escolha, com ou sem bloco na mão.
- [ ] Cada fatia tem o ícone pixel art; o menu e os textos são menores que antes.

Dois cliques:
- [ ] Escolher Oco, Arestas ou Preencher, clicar em um canto, depois no outro, e salvar: a área nova é criada com aquele tipo, sem preencher nada.
- [ ] Escolher outro tipo de área com uma já salva: a anterior continua intacta e a nova é independente.
- [ ] Escolher um tipo de área com o pincel ligado ou rascunho aberto: cancela os dois e entra na seleção.
- [ ] Sem bloco na mão, o painel pede um bloco; ao segurar um, o alvo aparece sem reabrir nada.
- [ ] O canto A aparece no clique, mesmo no ar. O canto B segue a mira.
- [ ] Mirando um bloco, o canto fica na célula em frente à face e a caixa não entra no bloco (chão, parede e teto).
- [ ] Na prévia, o scroll muda a dimensão do eixo da face que o olhar atinge; perto da quina o eixo não pisca.
- [ ] Os dois cliques não quebram blocos nem usam o item da mão.
- [ ] ESC com escolha armada ou canto A marcado cancela e o painel some.
- [ ] Tocar ALT com escolha armada ou canto A marcado cancela.

Modo pincel (Construir):
- [ ] Construir liga o pincel direto, sem pedir cliques e sem criar área.
- [ ] Dentro de uma área, o direito coloca na célula livre mais próxima, sem erro "não pode ser colocado ali" (o corpo do jogador não é alvo).
- [ ] Duas células quase à mesma distância: a que está na mira vai primeiro. Uma célula bem mais perto sempre vence a mira.
- [ ] Segurando o direito, os blocos entram a 10 por segundo, sem pausa entre eles.
- [ ] Segurar o direito ou o esquerdo repete rápido, sem camadas e sem exigir remover nada antes.
- [ ] Com duas áreas perto, o pincel age nas duas.
- [ ] Pintar numa área não altera blocos da outra.
- [ ] Esquerdo segurado quebra blocos dentro da área na hora (drops e desgaste em sobrevivência); fora da área é vanilla.
- [ ] Bedrock e baú dentro da área não são quebrados e o aviso não repete a cada tick.
- [ ] Com uma área Oco e outra Arestas, o pincel só coloca e quebra na casca de uma e só nas arestas da outra.
- [ ] Trocar o bloco da mão troca o alvo sem reabrir o modo.
- [ ] Tocar ALT com modo área ligado desliga e avisa "Modo área desligado".
- [ ] Escolher outra ação com o modo ligado desliga o anterior.
- [ ] O fantasma azul marca a próxima célula a receber bloco; o contorno vermelho, o que o esquerdo quebraria.

Operação em massa:
- [ ] MOD + V (criativo) só preenche área de volume inteiro; em Oco ou Arestas avisa.
- [ ] MOD + C limpa a área com confirmação em duas pressões.
- [ ] Mensagem "N alterado(s), M pulado(s)" ao terminar.

Atalhos:
- [ ] MOD + Z remove só a última região; MOD + F limpa o mundo atual.
- [ ] CTRL + MOD + Z desfaz a última operação em massa.
- [ ] Sem o mod no servidor, as opções avisam "Este servidor não tem o Selection Tools" e não mudam nada.

Roda e hotbar:
- [ ] A roda do mouse troca a hotbar em qualquer estado.
