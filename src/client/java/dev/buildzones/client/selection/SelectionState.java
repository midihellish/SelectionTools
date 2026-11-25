package dev.buildzones.client.selection;

/** Estados da máquina de seleção. Só {@link SelectionStateMachine} altera este valor. */
public enum SelectionState {
    IDLE,
    /** Escolha de Preencher feita no menu. O próximo clique marca o canto A. */
    ARMED,
    /** Canto A marcado. O canto B segue a mira até o segundo clique fixá-lo. */
    WAITING_SECOND_CORNER,
    /** Canto B fixado. O scroll ajusta a caixa e o próximo clique confirma a região. */
    PREVIEW
}
