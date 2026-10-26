package club;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

/** Formulaire générique avec champs texte, choix prédéfinis et validation obligatoire. */
final class Form {
    record F(String label, String value, String hint, boolean req, String[] opts) {}

    private Form() {}

    static F t(String label, String value, String hint) { return new F(label, value, hint, false, null); }
    static F r(String label, String value, String hint) { return new F(label, value, hint, true, null); }
    static F c(String label, String value, String... opts) { return new F(label, value, null, true, opts); }

    /** Retourne les valeurs dans l'ordre des champs, ou null si annule. */
    static List<String> ask(Component parent, String title, F... fs) {
        // Réaffiche le formulaire tant qu'un champ obligatoire reste vide.
        String[] vals = new String[fs.length];
        for (int i = 0; i < fs.length; i++) vals[i] = fs[i].value() == null ? "" : fs[i].value();
        while (true) {
            JPanel p = new JPanel(new GridBagLayout());
            JComponent[] comps = new JComponent[fs.length];
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(5, 4, 5, 4);
            g.anchor = GridBagConstraints.WEST;
            for (int i = 0; i < fs.length; i++) {
                F f = fs[i];
                g.gridx = 0; g.gridy = i; g.fill = GridBagConstraints.NONE; g.weightx = 0;
                p.add(new JLabel(f.label() + (f.req() ? " *" : "")), g);
                g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
                if (f.opts() != null) {
                    JComboBox<String> cb = new JComboBox<>(f.opts());
                    if (!vals[i].isEmpty()) cb.setSelectedItem(vals[i]);
                    cb.setPreferredSize(new Dimension(320, 34));
                    comps[i] = cb;
                } else {
                    JTextField tf = new JTextField(vals[i], 26);
                    if (f.hint() != null) tf.putClientProperty("JTextField.placeholderText", f.hint());
                    tf.setPreferredSize(new Dimension(320, 34));
                    comps[i] = tf;
                }
                p.add(comps[i], g);
            }
            int r = JOptionPane.showConfirmDialog(parent, p, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return null;
            boolean ok = true;
            for (int i = 0; i < fs.length; i++) {
                vals[i] = comps[i] instanceof JComboBox<?> cb
                        ? (cb.getSelectedItem() == null ? "" : cb.getSelectedItem().toString())
                        : ((JTextField) comps[i]).getText().trim();
            }
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].req() && vals[i].isBlank()) {
                    Util.info(parent, "Champ obligatoire : " + fs[i].label());
                    ok = false;
                    break;
                }
            }
            if (ok) return Arrays.asList(vals);
        }
    }
}
