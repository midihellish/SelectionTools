# Controles do Selection Tools

| Entrada | Quando | Efeito |
|---|---|---|
| `ALT + botão esquerdo` | Ocioso | Marca o canto A (não minera) |
| `ALT + botão esquerdo` | Esperando o segundo canto | Marca o canto B e abre a prévia |
| `ALT + botão esquerdo` | Prévia ativa | Ignorado (consumido), HUD avisa para confirmar ou cancelar |
| Scroll para cima | Prévia ativa | Aumenta 1 bloco no eixo ativo |
| Scroll para baixo | Prévia ativa | Diminui 1 bloco, nunca abaixo de 1 |
| Botão direito | Prévia ativa | Confirma e salva a região |
| Botão direito | Esperando o segundo canto | Consumido, sem ação |
| `ESC` | Com rascunho | Cancela só o rascunho |
| `ALT + Z` | Qualquer estado | Desfaz a última região confirmada (rascunho é mantido) |
| `ALT + F` | Qualquer estado | Remove as regiões do mundo atual e cancela o rascunho |
| `ALT + botão direito` | Ocioso, com bloco na mão, mirando um bloco | Pede a substituição do bloco mirado pelo bloco da mão |
| `ALT + B` | Ocioso, com região confirmada | Liga/desliga o modo área. Ao ligar, o alvo é o bloco da mão principal (estado padrão do bloco) |
| `botão direito` | Modo área ligado, bloco do alvo na mão principal | Coloca o bloco no fantasma mais próximo do olhar; segurado, repete a cada 120 ms |
| `ALT + C` | Modo área ligado, criativo | Limpar a área: primeira pressão mostra a contagem, segunda confirma |
| `ALT + V` | Modo área ligado, criativo, bloco na mão principal | Preencher a área com o bloco da mão (só ar e blocos substituíveis). Mesma confirmação em duas pressões |
| `CTRL + ALT + Z` | Criativo | Desfaz a última operação em massa (limpar ou preencher) |

## Regras

- Nada disso age com tela aberta (inventário, chat, menu).
- Scroll só é consumido durante a prévia. Fora dela a hotbar troca normalmente.
- `ALT + F` não dispara a troca de mão (`F`) do vanilla.
- O eixo do scroll é o componente dominante da câmera. Há histerese: a câmera perto de uma diagonal não troca de eixo a cada movimento.
- Scroll para cima sempre expande; o canto A nunca se move.

## Modo área

- A área usada é a **última região confirmada**. `ALT + Z` e `ALT + F` mudam qual é essa região.
- Os fantasmas aparecem só onde o bloco ainda difere do alvo, num raio de 8 blocos e 2 camadas verticais do jogador. Azul: falta colocar. Vermelho: bloco que não pertence ao alvo (só contorno).
- Colocar aceita a primeira célula-fantasma que o raio do olhar atinge, até 6 blocos. Não coloca atrás de um bloco sólido.
- Survival: cada colocação gasta 1 item do alvo. Quebrar continua pelo vanilla (ferramenta, drops, tempo); a área não quebra em massa.
- Operações em massa não tocam em baús, fornalhas e outros blocos com dados próprios, nem em líquidos, nem em blocos inquebráveis. Elas são contadas como "puladas" no fim.
- Sem o mod no servidor, `ALT + B` e as operações avisam e não fazem nada.
- Desfazer de blocos vale só dentro da sessão. Ao sair do servidor, o histórico some e o que já foi alterado fica como está.
- Ao trocar de mundo, o modo área desliga.

## Limitação conhecida

- `ALT + botão esquerdo` apontando para o ar não tem efeito (o Fabric só dispara o callback de ataque em bloco). O HUD não avisa "mire em um bloco" nesse caso.
