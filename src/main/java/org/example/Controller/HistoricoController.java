package org.example.Controller;

import org.example.Models.FormaGeometrica;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Histórico de ações (Memento). Cada estado salvo é uma cópia profunda das formas,
 * garantindo que alterações posteriores não corrompam o passado.
 */
public class HistoricoController {

    private final Deque<Memento> pilhaDesfazer = new ArrayDeque<>();
    private final Deque<Memento> pilhaRefazer = new ArrayDeque<>();

    /** Salva o estado atual e limpa a pilha de refazer. */
    public void registrarEstado(List<FormaGeometrica> formas) {
        pilhaDesfazer.push(Memento.de(formas));
        pilhaRefazer.clear();
    }

    /**
     * Restaura o estado anterior.
     *
     * @param atual estado presente, empilhado para permitir refazer
     * @return estado restaurado ou {@code null} se não houver o que desfazer
     */
    public List<FormaGeometrica> desfazer(List<FormaGeometrica> atual) {
        if (pilhaDesfazer.isEmpty()) {
            return null;
        }
        pilhaRefazer.push(Memento.de(atual));
        return pilhaDesfazer.pop().restaurar();
    }

    /**
     * Reaplica o estado desfeito.
     *
     * @param atual estado presente, empilhado para permitir desfazer novamente
     * @return estado restaurado ou {@code null} se não houver o que refazer
     */
    public List<FormaGeometrica> refazer(List<FormaGeometrica> atual) {
        if (pilhaRefazer.isEmpty()) {
            return null;
        }
        pilhaDesfazer.push(Memento.de(atual));
        return pilhaRefazer.pop().restaurar();
    }

    public boolean podeDesfazer() {
        return !pilhaDesfazer.isEmpty();
    }

    public boolean podeRefazer() {
        return !pilhaRefazer.isEmpty();
    }

    public void limpar() {
        pilhaDesfazer.clear();
        pilhaRefazer.clear();
    }

    /** Memento: guarda cópias profundas das formas de um instante. */
    public static final class Memento {

        private final List<FormaGeometrica> estado;

        private Memento(List<FormaGeometrica> estado) {
            this.estado = estado;
        }

        static Memento de(List<FormaGeometrica> formas) {
            List<FormaGeometrica> copias = new ArrayList<>(formas.size());
            for (FormaGeometrica f : formas) {
                copias.add(f.clonar());
            }
            return new Memento(copias);
        }

        /** Devolve uma nova cópia para que o memento continue intacto. */
        List<FormaGeometrica> restaurar() {
            List<FormaGeometrica> copias = new ArrayList<>(estado.size());
            for (FormaGeometrica f : estado) {
                copias.add(f.clonar());
            }
            return copias;
        }
    }
}
