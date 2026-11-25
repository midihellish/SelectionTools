# BuildZones

Mod Fabric para Minecraft Java 1.20.1. Desenhe volumes retangulares no mundo, ajuste pelo scroll, mantenha várias seleções visíveis e substitua blocos com validação no servidor.

- Minecraft 1.20.1, Fabric Loader 0.20.1, Fabric API 0.92.12+1.20.1
- Java 17 (compilado com `release 17`)

## Compilar

```bash
./gradlew build
```

O jar sai em `build/libs/buildzones-0.1.0.jar`. Os testes rodam com `./gradlew test`.

## Uso rápido

- `ALT + clique esquerdo` em dois blocos: cria a seleção.
- Scroll: ajusta o eixo da câmera. Botão direito: confirma.
- `ALT + Z` desfaz, `ALT + F` limpa, `ESC` cancela o rascunho.
- `ALT + botão direito` com um bloco na mão: substitui o bloco mirado (precisa do mod no servidor).
- `ALT + B` com uma região confirmada: liga o modo área. Com o bloco na mão, o botão direito coloca nos fantasmas da área. No criativo, `ALT + C` limpa a área e `ALT + V` preenche, com confirmação em duas pressões. `CTRL + ALT + Z` desfaz a última operação em massa.

Lista completa em [docs/CONTROLS.md](docs/CONTROLS.md).

## Multiplayer

As seleções são visuais e locais, então funcionam sem o mod no servidor. Só a substituição exige o mod instalado nos dois lados.

## Documentação

- [Controles](docs/CONTROLS.md)
- [Arquitetura](docs/ARCHITECTURE.md)
- [Substituição e compatibilidade](docs/REPLACEMENT_COMPATIBILITY.md)
- [Notas de renderização](docs/RENDERING_NOTES.md)
- [Plano de testes](docs/TEST_PLAN.md)
