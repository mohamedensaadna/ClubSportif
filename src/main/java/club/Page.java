package club;

import javax.swing.JPanel;

abstract class Page extends JPanel {
    /** Recharge les données visibles de la page. */
    abstract void refresh();
}
