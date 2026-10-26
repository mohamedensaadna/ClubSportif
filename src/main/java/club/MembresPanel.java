package club;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableModel;

class MembresPanel extends CrudPanel {
    MembresPanel() {
        super("Membres", "Type membre_t (hérite de personne_t) avec VARRAY de téléphones et Nested Table de paiements");
        action("Ajouter", "primary", this::add);
        action("Modifier", "plain", this::edit);
        action("Paiements", "plain", this::paiements);
        action("Supprimer", "danger", this::delete);
    }

    @Override TableModel load() throws Exception {
        // Calcule les informations affichées, dont le total de la collection de paiements.
        return Db.table("""
            SELECT m.id_personne AS "ID", m.nom_complet() AS "Membre", m.age() AS "Âge",
                   m.adresse.ville AS "Ville",
                   (SELECT LISTAGG(t.COLUMN_VALUE, ' / ') WITHIN GROUP (ORDER BY t.COLUMN_VALUE)
                      FROM TABLE(m.telephones) t) AS "Téléphones",
                   m.num_licence AS "Licence", m.date_inscription AS "Inscription",
                   m.anciennete_mois() AS "Ancienneté (mois)",
                   (SELECT NVL(SUM(p.montant),0) FROM TABLE(m.paiements) p) AS "Total payé (DT)"
            FROM membres m ORDER BY m.id_personne""");
    }

    private List<String> form(String title, Object[] i) {
        return Form.ask(this, title,
            Form.r("Nom", Util.v(i, 0), null),
            Form.r("Prénom", Util.v(i, 1), null),
            Form.t("Téléphones", Util.v(i, 2), "séparés par virgule (max 3)"),
            Form.t("Date de naissance", Util.v(i, 3), "AAAA-MM-JJ"),
            Form.t("Rue", Util.v(i, 4), null),
            Form.t("Ville", Util.v(i, 5), null),
            Form.t("Code postal", Util.v(i, 6), null),
            Form.r("N° licence", Util.v(i, 7), null),
            Form.r("Date d'inscription", i == null ? LocalDate.now().toString() : Util.v(i, 8), "AAAA-MM-JJ"));
    }

    private List<Object> base(List<String> f) {
        // Prépare dans l'ordre les valeurs communes à l'insertion et à la modification.
        List<Object> p = new ArrayList<>();
        p.add(f.get(0));
        p.add(f.get(1));
        p.addAll(Util.list(f.get(2), 3));
        p.add(Util.date(f.get(3)));
        p.add(f.get(4));
        p.add(f.get(5));
        p.add(f.get(6));
        p.add(f.get(7));
        p.add(Util.date(f.get(8)));
        return p;
    }

    private void add() throws Exception {
        List<String> f = form("Nouveau membre", null);
        if (f == null) return;
        List<Object> p = base(f);
        p.add(0, Db.nextId("SELECT NVL(MAX(id),0)+1 FROM (SELECT id_personne id FROM membres UNION ALL SELECT id_personne FROM entraineurs)"));
        String ph = Util.ph(Util.list(f.get(2), 3).size());
        Db.exec("INSERT INTO membres VALUES (membre_t(?,?,?,telephones_va(" + ph
                + "),?,adresse_t(?,?,?),?,?,paiements_nt()))", p.toArray());
        refresh();
    }

    private void edit() throws Exception {
        Object id = key();
        if (id == null) return;
        Object[] cur = Db.rows("""
            SELECT m.nom, m.prenom,
                   (SELECT LISTAGG(t.COLUMN_VALUE, ', ') WITHIN GROUP (ORDER BY t.COLUMN_VALUE) FROM TABLE(m.telephones) t),
                   m.date_naissance, m.adresse.rue, m.adresse.ville, m.adresse.code_postal,
                   m.num_licence, m.date_inscription
            FROM membres m WHERE m.id_personne = ?""", id).get(0);
        List<String> f = form("Modifier le membre " + id, cur);
        if (f == null) return;
        List<Object> p = base(f);
        p.add(id);
        String ph = Util.ph(Util.list(f.get(2), 3).size());
        Db.exec("UPDATE membres m SET m.nom=?, m.prenom=?, m.telephones=telephones_va(" + ph
                + "), m.date_naissance=?, m.adresse=adresse_t(?,?,?), m.num_licence=?, m.date_inscription=? "
                + "WHERE m.id_personne=?", p.toArray());
        refresh();
    }

    private void delete() throws Exception {
        Object id = key();
        if (id == null) return;
        if (!Util.confirm(this, "Supprimer le membre « " + cell(1) + " » et le retirer de toutes les séances ?")) return;
        // Retire d'abord le membre des collections de participants, puis le supprime.
        Db.tx(() -> {
            for (Object[] s : Db.rows("SELECT id_seance FROM seances"))
                Db.exec("DELETE FROM TABLE(SELECT s.participants FROM seances s WHERE s.id_seance = ?) t "
                        + "WHERE t.COLUMN_VALUE = (SELECT REF(m) FROM membres m WHERE m.id_personne = ?)", s[0], id);
            Db.exec("DELETE FROM membres WHERE id_personne = ?", id);
        });
        refresh();
    }

    private void paiements() {
        // Ouvre une fenêtre dédiée à la consultation et à l'ajout de paiements.
        Object id = key();
        if (id == null) return;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Paiements — " + cell(1), Dialog.ModalityType.APPLICATION_MODAL);
        JTable t = new JTable();
        Util.styleTable(t);
        JLabel total = new JLabel();
        total.putClientProperty("FlatLaf.style", "font:bold +2");
        Runnable load = () -> {
            // Actualise la liste et son total après chaque ajout.
            try {
                t.setModel(Db.table("""
                    SELECT p.date_paiement AS "Date", p.montant AS "Montant (DT)"
                    FROM membres m, TABLE(m.paiements) p WHERE m.id_personne = ? ORDER BY 1""", id));
                total.setText("Total : " + Db.scalar("""
                    SELECT NVL(SUM(p.montant),0) FROM membres m, TABLE(m.paiements) p
                    WHERE m.id_personne = ?""", id) + " DT");
            } catch (Exception e) { Util.error(dlg, e); }
        };
        JButton add = Util.button("Ajouter un paiement", "primary", () -> {
            List<String> f = Form.ask(dlg, "Nouveau paiement",
                    Form.r("Date", LocalDate.now().toString(), "AAAA-MM-JJ"),
                    Form.r("Montant (DT)", "50", null));
            if (f == null) return;
            Db.exec("INSERT INTO TABLE(SELECT m.paiements FROM membres m WHERE m.id_personne = ?) "
                    + "VALUES (paiement_t(?, ?))", id, Util.date(f.get(0)), new BigDecimal(f.get(1).replace(',', '.')));
            load.run();
        });
        JButton close = Util.button("Fermer", "plain", dlg::dispose);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.add(add);
        btns.add(close);
        bottom.add(total, BorderLayout.WEST);
        bottom.add(btns, BorderLayout.EAST);
        dlg.setLayout(new BorderLayout());
        dlg.add(new JScrollPane(t), BorderLayout.CENTER);
        dlg.add(bottom, BorderLayout.SOUTH);
        dlg.setSize(480, 400);
        dlg.setLocationRelativeTo(this);
        load.run();
        dlg.setVisible(true);
        refresh();
    }
}
