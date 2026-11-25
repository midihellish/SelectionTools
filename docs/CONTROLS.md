# Controles do Selection Tools

A tecla de modificador é **ALT esquerdo** por padrão. Ela aparece como **MOD** aqui. Troque em **Opções > Controles > Selection Tools**. Quando o nome muda, o HUD e o menu mostram a tecla atual.

## Menu radial

| Entrada | Quando | Efeito |
|---|---|---|
| Tocar **MOD** (apertar e soltar, sem outra tecla) | Ocioso | Abre o menu radial |
| Tocar **MOD** | Ação em curso: escolha armada, canto A marcado ou modo área | Para a ação. Não abre o menu |
| Tocar **MOD** | Menu aberto | Fecha o menu |
| Clique numa fatia, teclas `1` a `4`, ou setas + `Enter` | Menu aberto | Escolhe a ação e fecha o menu |
| `ESC` | Menu aberto | Fecha sem escolher |

As fatias, no sentido horário a partir do topo. Todas podem ser escolhidas, com qualquer coisa na mão. Passe o mouse sobre uma fatia para ver o nome e a descrição (tooltip).

| Fatia | Tecla | O que faz |
|---|---|---|
| **Construir** | `1` | Liga o pincel em todas as áreas do alcance, cada uma no seu tipo. Não marca área. Direito coloca; esquerdo quebra |
| **Oco** | `2` | Marca uma área nova do tipo casca |
| **Arestas** | `3` | Marca uma área nova do tipo arestas |
| **Preencher** | `4` | Marca uma área nova do tipo volume inteiro. Só marca; quem constrói é o pincel |

## Marcar a área (Oco, Arestas, Preencher)

Cada uma dessas três fatias começa uma seleção nova, com três cliques. A região salva guarda o tipo e é independente: nunca altera as anteriores. Escolher qualquer fatia cancela o que estiver em curso (pincel ligado ou rascunho aberto) e, nas três de área, já entra no modo de seleção.

Depois de escolher uma fatia, o HUD mostra o modo e a tecla que cancela.

| Entrada | Quando | Efeito |
|---|---|---|
| Clique esquerdo ou direito | Escolha armada | Marca o canto A na mira. Não ataca nem usa item |
| Clique esquerdo ou direito | Canto A marcado | Fixa o canto B na mira e libera o scroll. Não ataca nem usa item |
| Roda do mouse | Canto B fixado (prévia) | Ajusta a dimensão do eixo da face da caixa que você olha (1 bloco por passo) |
| Clique esquerdo ou direito | Canto B fixado (prévia) | Salva a região com o tipo escolhido. Não ataca nem usa item |
| `ESC` ou tocar **MOD** | Escolha armada ou canto A marcado | Cancela. Nada é salvo |

## Atalhos com modificador

| Atalho | Quando | Efeito |
|---|---|---|
| **MOD + Z** | Qualquer estado | Desfaz a última região confirmada (o rascunho é mantido) |
| **MOD + F** | Qualquer estado | Remove as regiões do mundo atual e cancela o rascunho |
| **MOD + C** | Criativo, modo área ligado | Limpar a área. Primeira pressão mostra a contagem, segunda confirma |
| **MOD + V** | Criativo, bloco na mão | Preenche a última área (só se for de volume inteiro) com o bloco da mão. Mesma confirmação em duas pressões |
| **CTRL + MOD + Z** | Criativo | Desfaz a última operação em massa (limpar ou preencher) |
| **MOD + botão direito** | Ocioso, bloco na mão, mirando um bloco | Pede a troca do bloco mirado pelo bloco da mão |

## Modo pincel (Construir)

- Ligado por Construir no menu. Não cria área. Desliga com um toque em **MOD**, ou quando você escolhe outra ação.
- O pincel vale em **todas as áreas da dimensão**. Em cada área, só as células do tipo dela (volume, casca ou arestas) aceitam colocar ou quebrar. Áreas salvas antes desta versão contam como volume inteiro.
- **Direito** coloca o bloco da mão na célula livre **mais próxima** de você, dentro de uma área (e do tipo dela), até 6 blocos dos olhos. A mira só ajuda: a célula bem no centro da mira conta como até 35% mais perto, e fora da frente do olhar não ganha nada. Não precisa mirar num bloco. Sem camadas e sem ordem; nada obriga a remover blocos antes.
- **Esquerdo** quebra o bloco mirado (mira do vanilla), na hora, com drops e desgaste da ferramenta como no jogo.
- Segurando, os dois repetem a cada 90 ms (10 por segundo). Células recém-pedidas são puladas até o servidor responder, então o pincel avança sem pausa.
- O bloco-alvo é o bloco da mão principal, e acompanha a mão. Sem bloco na mão, o direito não faz nada; o esquerdo quebra do mesmo jeito.
- Fantasma azul: a próxima célula a receber bloco. Contorno vermelho: o bloco que o esquerdo quebraria. Só aparecem dentro de uma área.
- Fora das áreas o jogo é vanilla. Blocos protegidos (inquebráveis, com inventário, fluidos) não são quebrados.
- Em sobrevivência, cada colocação gasta 1 item do alvo.

## Regras

- Com tela aberta (inventário, chat, menu do jogo), nada disso age. O menu radial é a única exceção: ele é a tela do próprio mod.
- Enquanto o canto A ou B não está fixado, o canto segue a mira, até o alcance de colocação do jogador (4,5 blocos em sobrevivência, 5 no criativo). O canto fica na célula livre em frente à face mirada, então a caixa para na superfície e não entra no bloco. Sem bloco nesse alcance, cai no ponto do olhar, mesmo no ar.
- Os dois cliques são consumidos: não quebram o bloco mirado nem usam o item da mão.
- Com a seleção ativa (armada, marcando ou na prévia), a roda do mouse **não troca a hotbar**.
- Na prévia (depois do canto B fixado), a roda ajusta a dimensão do eixo da face da caixa que o olhar atinge (de fora, a face de entrada; de dentro, a face à frente). Se o olhar não toca a caixa, vale o eixo dominante da câmera. O terceiro clique salva.
- Com a seleção ociosa, a roda troca a hotbar normalmente.
- Operações em massa não tocam em baús, fornalhas e outros blocos com dados próprios, nem em líquidos, nem em blocos inquebráveis. Elas são contadas como "puladas" no fim.
- Sem o mod no servidor, as ações avisam e não fazem nada.
- O histórico de blocos desfeitos vale só dentro da sessão. Ao sair do servidor, o que já foi alterado fica como está.
- Ao trocar de mundo, o menu fecha, a escolha pendente é cancelada e o modo área desliga.
- O menu mostra um ícone pixel art em cada fatia. Nome e descrição aparecem só no tooltip, ao passar o mouse.

## Limitação conhecida

- Tocar **MOD** é a única forma de abrir o menu. Se a tecla for uma que o sistema operacional usa (por exemplo, Alt com arrasto de janela), troque em Controles.
