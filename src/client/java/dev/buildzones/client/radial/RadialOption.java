package dev.buildzones.client.radial;

import dev.buildzones.client.area.AreaShape;

/**
 * Fatias do menu radial, na ordem horária a partir do topo. A ordem define a posição
 * de cada fatia e a tecla de 1 a 4. Oco, Arestas e Preencher iniciam uma seleção nova; Construir só liga o pincel.
 */
public enum RadialOption {
    /** Só liga o pincel, sem marcar área. Direito coloca na camada ativa; esquerdo quebra. Sem forma própria. */
    BUILD("buildzones.radial.build", "buildzones.radial.desc_build", null),
    HOLLOW("buildzones.radial.hollow", "buildzones.radial.desc_hollow", AreaShape.HOLLOW),
    EDGES("buildzones.radial.edges", "buildzones.radial.desc_edges", AreaShape.EDGES),
    /** Área de volume inteiro. Só marca. */
    FILL("buildzones.radial.fill", "buildzones.radial.desc_fill", AreaShape.FILL);

    private final String labelKey;
    private final String descriptionKey;
    private final AreaShape shape;

    RadialOption(String labelKey, String descriptionKey, AreaShape shape) {
        this.labelKey = labelKey;
        this.descriptionKey = descriptionKey;
        this.shape = shape;
    }

    public String labelKey() {
        return labelKey;
    }

    public String descriptionKey() {
        return descriptionKey;
    }

    /** Tipo da área que a opção marca; nulo em Construir, que só liga o pincel. */
    public AreaShape shape() {
        return shape;
    }

    /** Verdadeiro para Construir: liga o pincel e não cria seleção. */
    public boolean isBrush() {
        return shape == null;
    }
}
