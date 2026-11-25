<p align="center">
  <img src="docs/assets/banner.svg" alt="Selection Tools: desenhe o volume, construa dentro dele" width="100%">
</p>

<p align="center">
  <a href="https://github.com/midihellish/SelectionTools/releases/latest">
    <img src="https://img.shields.io/badge/Baixar-selection-tools--0.2.0.jar-e8466f?style=for-the-badge&labelColor=0e1016" alt="Baixar selection-tools-0.2.0.jar">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-f088b3?style=flat-square&labelColor=0e1016" alt="Minecraft 1.20.1">
  <img src="https://img.shields.io/badge/Fabric-Loader_0.20.1-fbeee0?style=flat-square&labelColor=0e1016" alt="Fabric Loader 0.20.1">
  <img src="https://img.shields.io/badge/Java-17+-f4a07a?style=flat-square&labelColor=0e1016" alt="Java 17+">
  <img src="https://img.shields.io/badge/licen%C3%A7a-todos_os_direitos-6fd6c8?style=flat-square&labelColor=0e1016" alt="Licença">
</p>

---

## Instalação

1. Baixe o `selection-tools-0.2.0.jar` na [página de releases](https://github.com/midihellish/SelectionTools/releases/latest).
2. Coloque o arquivo na pasta `mods/` da sua instância Fabric.
3. Use Fabric Loader `0.20.1` e Fabric API `0.92.12+1.20.1`, com Minecraft `1.20.1`.

> O jar é só para Fabric. Não serve para NeoForge.

---

## O que faz

<table>
  <tr>
    <td width="33%" valign="top">
      <h4>Seleção por dois cantos</h4>
      <sub>Marque dois blocos e veja o volume em translúcido.</sub>
    </td>
    <td width="33%" valign="top">
      <h4>Ajuste pelo scroll</h4>
      <sub>Cresce ou diminui no eixo que a câmera aponta.</sub>
    </td>
    <td width="33%" valign="top">
      <h4>Várias regiões</h4>
      <sub>Regiões independentes e sobrepostas, salvas por mundo.</sub>
    </td>
  </tr>
  <tr>
    <td width="33%" valign="top">
      <h4>Desfazer e limpar</h4>
      <sub>Volta a última região ou limpa o mundo atual.</sub>
    </td>
    <td width="33%" valign="top">
      <h4>Smart Replacement</h4>
      <sub>Troca um bloco por outro, validado pelo servidor.</sub>
    </td>
    <td width="33%" valign="top">
      <h4>Modo área</h4>
      <sub>Coloque blocos nos fantasmas da área e preencha ou limpe no criativo.</sub>
    </td>
  </tr>
</table>

<p align="center">
  <img src="docs/assets/fluxo.svg" alt="Fluxo: canto A, canto B, scroll e confirmação" width="100%">
</p>

---

## Controles

| Ação | Atalho |
|---|---|
| Marcar canto A e canto B | <kbd>ALT</kbd> + <kbd>clique esquerdo</kbd> |
| Ajustar o volume | <kbd>scroll</kbd> |
| Confirmar a região | <kbd>clique direito</kbd> |
| Cancelar o rascunho | <kbd>ESC</kbd> |
| Desfazer a última região | <kbd>ALT</kbd> + <kbd>Z</kbd> |
| Limpar as regiões do mundo | <kbd>ALT</kbd> + <kbd>F</kbd> |
| Trocar o bloco mirado | <kbd>ALT</kbd> + <kbd>clique direito</kbd> |
| Ligar o modo área | <kbd>ALT</kbd> + <kbd>B</kbd> |

Lista completa, com regras e casos de borda, em [docs/CONTROLS.md](docs/CONTROLS.md).

---

## Multiplayer

As seleções são visuais e locais, então funcionam em qualquer servidor. A substituição de blocos e o modo área precisam do mod nos dois lados.

---

## Para desenvolver

```bash
./gradlew build      # compila, testa e gera o jar em build/libs/
./gradlew runClient  # abre um cliente de desenvolvimento
```

Use JDK 21. Mais detalhes em [CLAUDE.md](CLAUDE.md).

---

## Documentação

| Arquivo | Conteúdo |
|---|---|
| [Controles](docs/CONTROLS.md) | Todas as entradas e regras |
| [Arquitetura](docs/ARCHITECTURE.md) | Camadas, fluxo e persistência |
| [Substituição](docs/REPLACEMENT_COMPATIBILITY.md) | Smart Replacement e compatibilidade |
| [Renderização](docs/RENDERING_NOTES.md) | Camadas, geometria e animações |
| [Testes](docs/TEST_PLAN.md) | Testes automáticos e manuais |
| [Changelog](CHANGELOG.md) | O que mudou em cada versão |

---

<sub>Todos os direitos reservados.</sub>
