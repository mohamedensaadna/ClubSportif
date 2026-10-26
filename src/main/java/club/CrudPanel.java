package club;

import java.awt.*;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

/** Page standard : titre, recherche, boutons d'action et tableau. */
abstract class CrudPanel extends Page {
    final JTable table = new JTable();
    final JTextField search = new JTextField();
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    CrudPanel(String title, String subtitle) {
        // Assemble les éléments communs aux pages de gestion.
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(24, 28, 24, 28));
        setBackground(Util.BG);

        JLabel t = new JLabel(title);
        t.putClientProperty("FlatLaf.style", "font:bold +10; foreground:#111827");
        JLabel s = new JLabel(subtitle);
        s.putClientProperty("FlatLaf.style", "foreground:#6B7280");
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(t);
        head.add(s);

        search.putClientProperty("JTextField.placeholderText", "Rechercher…");
        search.putClientProperty("JTextField.showClearButton", true);
        search.setPreferredSize(new Dimension(280, 36));
        actions.setOpaque(false);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(search, BorderLayout.WEST);
        bar.add(actions, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(bar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        Util.styleTable(table);
        table.setAutoCreateRowSorter(true);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(new Color(0xE5E7EB)));
        sp.getViewport().setBackground(Color.WHITE);
        add(sp, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            // Met à jour le filtre à chaque modification du texte recherché.
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
    }

    final void action(String text, String kind, Util.Act a) {
        actions.add(Util.button(text, kind, a));
    }

    abstract TableModel load() throws Exception;

    @Override void refresh() {
        // Recharge le modèle depuis la base puis réapplique la recherche.
        try {
            table.setModel(load());
            filter();
        } catch (Exception e) {
            Util.error(this, e);
        }
    }

    private void filter() {
        if (table.getRowSorter() instanceof TableRowSorter<?> rs) {
            String q = search.getText().trim();
            rs.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        }
    }

    /** Valeur de la 1re colonne de la ligne choisie (ou null + message). */
    Object key() {
        // La première colonne est la clé utilisée par les actions CRUD.
        int r = table.getSelectedRow();
        if (r < 0) {
            Util.info(this, "Sélectionnez d'abord une ligne dans le tableau.");
            return null;
        }
        return table.getValueAt(r, 0);
    }

    Object cell(int col) { return table.getValueAt(table.getSelectedRow(), col); }
}
