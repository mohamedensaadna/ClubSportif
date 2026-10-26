package club;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

class DashboardPanel extends Page {
    private final JLabel[] values = new JLabel[4];
    private final JTable next = new JTable(), pay = new JTable();

    DashboardPanel() {
        // Présente des indicateurs synthétiques et deux tableaux de suivi.
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));
        setBackground(Util.BG);

        JLabel t = new JLabel("Tableau de bord");
        t.putClientProperty("FlatLaf.style", "font:bold +10; foreground:#111827");
        JLabel s = new JLabel("Vue d'ensemble du club");
        s.putClientProperty("FlatLaf.style", "foreground:#6B7280");
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(t);
        head.add(s);

        String[] titles = {"Membres", "Entraîneurs", "Séances à venir", "Revenus (DT)"};
        String[] colors = {"#4F46E5", "#0891B2", "#D97706", "#059669"};
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            JPanel c = new JPanel(new GridLayout(2, 1, 0, 4));
            c.setBackground(Color.WHITE);
            c.setBorder(new CompoundBorder(new LineBorder(new Color(0xE5E7EB)), new EmptyBorder(16, 18, 16, 18)));
            JLabel l = new JLabel(titles[i]);
            l.putClientProperty("FlatLaf.style", "foreground:#6B7280");
            values[i] = new JLabel("0");
            values[i].putClientProperty("FlatLaf.style", "font:bold +14; foreground:" + colors[i]);
            c.add(l);
            c.add(values[i]);
            cards.add(c);
        }

        JPanel tables = new JPanel(new GridLayout(1, 2, 16, 0));
        tables.setOpaque(false);
        tables.add(section("Prochaines séances", next));
        tables.add(section("Paiements par membre", pay));

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(tables, BorderLayout.CENTER);

        add(head, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    private JPanel section(String title, JTable t) {
        Util.styleTable(t);
        JLabel l = new JLabel(title);
        l.putClientProperty("FlatLaf.style", "font:bold +2; foreground:#111827");
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(new Color(0xE5E7EB)));
        sp.getViewport().setBackground(Color.WHITE);
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(l, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    @Override void refresh() {
        try {
            // Les indicateurs et tableaux sont recalculés à chaque affichage.
            Object[] r = Db.rows("""
                SELECT (SELECT COUNT(*) FROM membres),
                       (SELECT COUNT(*) FROM entraineurs),
                       (SELECT COUNT(*) FROM seances WHERE date_seance >= TRUNC(SYSDATE)),
                       (SELECT NVL(SUM(p.montant),0) FROM membres m, TABLE(m.paiements) p)
                FROM dual""").get(0);
            for (int i = 0; i < 4; i++) values[i].setText(String.valueOf(r[i]));
            next.setModel(Db.table("""
                SELECT s.id_seance AS "N°", s.date_seance AS "Début", s.salle.code_salle AS "Salle",
                       s.entraineur.prenom || ' ' || s.entraineur.nom AS "Entraîneur",
                       s.nb_participants() AS "Inscrits"
                FROM seances s WHERE s.date_seance >= TRUNC(SYSDATE)
                ORDER BY s.date_seance FETCH FIRST 8 ROWS ONLY"""));
            pay.setModel(Db.table("""
                SELECT m.prenom || ' ' || m.nom AS "Membre", NVL(SUM(p.montant),0) AS "Total payé (DT)"
                FROM membres m LEFT JOIN TABLE(m.paiements) p ON 1=1
                GROUP BY m.id_personne, m.prenom, m.nom ORDER BY 2 DESC"""));
        } catch (Exception e) {
            Util.error(this, e);
        }
    }
}
