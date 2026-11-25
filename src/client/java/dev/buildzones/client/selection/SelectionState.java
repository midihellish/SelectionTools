package dev.buildzones.client.selection;

/** Estados da máquina de seleção. Só {@link SelectionStateMachine} altera este valor. */
public enum SelectionState {
    IDLE,
    WAITING_SECOND_CORNER,
    PREVIEW
}
