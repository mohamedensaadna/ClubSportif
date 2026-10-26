package club;

import java.awt.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

final class Util {
    static final Color BG = new Color(0xF5F6FA);

    private Util() {}

    @FunctionalInterface interface Act { void run() throws Exception; }

    // ---------- Conversion des valeurs saisies et affichées ----------
    static boolean blank(String s) { return s == null || s.isBlank(); }

    static LocalDate date(String s) { return blank(s) ? null : LocalDate.parse(s.trim()); }

    static LocalDateTime dateTime(String s) {
        String t = s.trim().replace(' ', 'T');
        if (t.length() == 10) t += "T00:00";
        return LocalDateTime.parse(t);
    }

    static List<String> list(String s, int max) {
        // Accepte plusieurs séparateurs et limite le nombre de valeurs fournies.
        List<String> out = new ArrayList<>();
        if (!blank(s)) for (String p : s.split("[,;/]")) if (!p.isBlank()) out.add(p.trim());
        if (out.size() > max) throw new IllegalArgumentException("Maximum " + max + " valeurs autorisées.");
        return out;
    }

    static String ph(int n) { return n == 0 ? "" : "?" + ",?".repeat(n - 1); }

    static String key(String comboValue) { return comboValue.substring(0, comboValue.indexOf(" — ")); }

    static Object cell(Object o) {
        if (o instanceof Timestamp ts) return str(ts);
        return o;
    }

    static String str(Object o) {
        if (o == null) return "";
        if (o instanceof Timestamp ts) {
            LocalDateTime d = ts.toLocalDateTime();
            return d.toLocalTime().equals(LocalTime.MIDNIGHT)
                    ? d.toLocalDate().toString()
                    : d.toLocalDate() + " " + String.format("%02d:%02d", d.getHour(), d.getMinute());
        }
        return o.toString();
    }

    static String v(Object[] a, int i) { return a == null ? "" : str(a[i]); }

    // ---------- Boîtes de dialogue et gestion des erreurs ----------
    static void info(Component c, String m) {
        JOptionPane.showMessageDialog(c, m, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component c, String m) {
        return JOptionPane.showConfirmDialog(c, m, "Confirmation", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static void error(Component c, Exception e) {
        e.printStackTrace();
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        if (e instanceof java.sql.SQLException) m = m.lines().findFirst().orElse(m);
        JOptionPane.showMessageDialog(c, m, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    static void run(Component c, Act a) {
        try { a.run(); } catch (Exception e) { error(c, e); }
    }

    // ---------- Création et style des composants Swing ----------
    static JButton button(String text, String kind, Act a) {
        JButton b = new JButton(text);
        String base = "arc:10; focusWidth:0; margin:7,16,7,16; ";
        switch (kind) {
            case "primary" -> b.putClientProperty("FlatLaf.style", base
                    + "background:#4F46E5; foreground:#fff; hoverBackground:#4338CA; pressedBackground:#3730A3; borderWidth:0");
            case "danger" -> b.putClientProperty("FlatLaf.style", base
                    + "background:#DC2626; foreground:#fff; hoverBackground:#B91C1C; pressedBackground:#991B1B; borderWidth:0");
            default -> b.putClientProperty("FlatLaf.style", base
                    + "background:#fff; foreground:#374151; borderColor:#D1D5DB; hoverBackground:#F3F4F6");
        }
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> run(b, a));
        return b;
    }

    static void styleTable(JTable t) {
        t.setRowHeight(34);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xEEF0F4));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(new Color(0xE0E7FF));
        t.setSelectionForeground(new Color(0x1E1B4B));
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().putClientProperty("FlatLaf.style",
                "height:38; font:bold; background:#F9FAFB; separatorColor:#F9FAFB; bottomSeparatorColor:#E5E7EB");
    }
}
