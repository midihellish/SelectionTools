# Changelog

## 0.1.0

Primeira versão jogável.

- Seleção com dois cliques (`ALT + botão esquerdo`), prévia translúcida e ajuste por scroll no eixo da câmera, com histerese.
- Confirmação com botão direito; várias regiões independentes e sobrepostas.
- `ALT + Z` (desfazer), `ALT + F` (limpar o mundo atual), `ESC` (cancelar rascunho).
- Persistência em JSON por perfil de mundo e dimensão, com troca atômica.
- Animações de convergência, pulso na confirmação e fantasma no undo/clear.
- HUD com dimensões, volume, eixo ativo e toasts. Textos em pt_br e en_us.
- Smart Replacement (`ALT + botão direito`): validação e troca no servidor, drops e consumo conforme o modo de jogo.

## Não lançado

- Modo área (`ALT + B`): alvo = bloco da mão. Dentro do modo, `RMB` coloca no fantasma mirado (sobrevivência e criativo), com consumo de 1 item e sons/eventos reais.
- Operações em massa no criativo: `ALT + C` limpa a área e `ALT + V` preenche com o bloco da mão. Duas pressões: a primeira mostra a contagem, a segunda confirma. `CTRL + ALT + Z` desfaz a última operação.
- O servidor passa a conhecer a região ativa (`sync_region`) e revalida tudo contra ela. Limites: 512 por dimensão, 262.144 blocos por operação.
- Operações em massa rodam em fatias de tick (4096 células por tick), uma por jogador, com desfazer limitado a 1.048.576 células.
