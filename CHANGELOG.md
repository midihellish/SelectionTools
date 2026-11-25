# Changelog

## 0.3.0

- Menu radial, tipos de área (construir, oco, arestas, preencher), pincel e quebra pelo servidor, consolidados num único commit.
- README refeito, com gráficos novos em `docs/assets/` (fluxo, tipos de área e menu radial).

## 0.2.4

- Pincel mais lento: uma ação a cada 90 ms (10 por segundo), metade da velocidade anterior.
- A seleção para nos blocos: o canto marcado é a célula livre em frente à face mirada, então a caixa não entra no bloco. No ar, continua o ponto do olhar.
- O scroll da prévia ajusta a dimensão do eixo da face da caixa que o olhar atinge (de fora, a face de entrada; de dentro, a face à frente), com histerese perto das quinas. Se o olhar não toca a caixa, vale o eixo dominante da câmera.

## 0.2.3

- Colocação do pincel por proximidade + mira: o direito coloca na célula livre mais perto do jogador, dentro de uma área e do tipo dela. A mira só dá um desconto na distância (até 35%), então perto sempre pesa mais. Não precisa mirar num bloco nem no ar.
- Células já pedidas não são escolhidas de novo enquanto o servidor não responde, para o pincel seguir à próxima sem pausa.
- O esquerdo continua quebrando o bloco mirado.

## 0.2.2

- Pincel refeito: botão direito coloca, esquerdo quebra o bloco mirado. Os dois repetem a cada tick enquanto segurados, sem camadas e sem obrigar a remover blocos antes.
- Corrige "O bloco não pode ser colocado ali" dentro de uma área: o pincel escolhia a célula da própria cabeça do jogador.
- Quebra do pincel passa pelo servidor (`area_break_request`): instantânea, com drops, durabilidade e proteção do jogo. Só vale dentro de áreas, no tipo de cada uma.
- Alcance do pincel medido a partir dos olhos, no cliente e no servidor.
- Recusas repetidas do servidor viram um aviso por janela, em vez de um por tick.

## Não lançado

- Menu radial: tocar ALT (tecla configurável em Controles, categoria Selection Tools) abre o menu com Construir, Oco, Arestas e Preencher. Tocar de novo ou ESC fecha. Com ação em curso, tocar ALT para a ação.
- Seleção em dois cliques depois da escolha: o primeiro marca o canto A, o segundo salva a região e aplica a escolha. O ajuste por scroll e a prévia saíram.
- `ALT + B` saiu: o modo área é ligado pelo menu. `ALT + botão esquerdo` saiu: a marcação é pelo menu.
- Painel do HUD mostra o modo atual e a tecla que para ou cancela.
- Menu radial com ícones pixel art, menor, com textos reduzidos. Todas as fatias aceitam escolha, com ou sem bloco na mão.
- Oco, Arestas e Preencher marcam uma área nova (três cliques), cada uma com o próprio tipo, sem mexer nas anteriores. Preencher só marca. Construir liga o pincel, que respeita o tipo de cada área.
- Quebrar saiu do menu; a remoção em massa continua em MOD + C.

## 0.2.1

- Seleção em três etapas: o primeiro clique fixa o canto B na mira, o scroll ajusta a caixa e o segundo clique confirma.
- Canto B segue a mira até o alcance de colocação do jogador (4,5 blocos em sobrevivência, 5 no criativo), mesmo no ar.
- Scroll só age depois que o canto B está fixado; antes disso a hotbar troca normalmente.
- Seleção em prévia em branco e mais transparente.

## 0.2.0

- Mod renomeado para Selection Tools (nome no jogo, HUD e jar `selection-tools-0.2.0.jar`). O id interno continua `buildzones`.
- Modo área (`ALT + B`): alvo = bloco da mão. Dentro do modo, `RMB` coloca no fantasma mirado (sobrevivência e criativo), com consumo de 1 item e sons/eventos reais.
- Operações em massa no criativo: `ALT + C` limpa a área e `ALT + V` preenche com o bloco da mão. Duas pressões: a primeira mostra a contagem, a segunda confirma. `CTRL + ALT + Z` desfaz a última operação.
- O servidor passa a conhecer a região ativa (`sync_region`) e revalida tudo contra ela. Limites: 512 por dimensão, 262.144 blocos por operação.
- Operações em massa rodam em fatias de tick (4096 células por tick), uma por jogador, com desfazer limitado a 1.048.576 células.

## 0.1.0

Primeira versão jogável.

- Seleção com dois cliques (`ALT + botão esquerdo`), prévia translúcida e ajuste por scroll no eixo da câmera, com histerese.
- Confirmação com botão direito; várias regiões independentes e sobrepostas.
- `ALT + Z` (desfazer), `ALT + F` (limpar o mundo atual), `ESC` (cancelar rascunho).
- Persistência em JSON por perfil de mundo e dimensão, com troca atômica.
- Animações de convergência, pulso na confirmação e fantasma no undo/clear.
- HUD com dimensões, volume, eixo ativo e toasts. Textos em pt_br e en_us.
- Smart Replacement (`ALT + botão direito`): validação e troca no servidor, drops e consumo conforme o modo de jogo.
