package club;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableModel;

class SeancesPanel extends CrudPanel {
    SeancesPanel() {
        super("Séances", "REF vers salle et entraîneur, Nested Table de REF membre pour les participants");
        action("Nouvelle séance", "primary", this::add);
        action("Participants", "plain", this::participants);
        action("Supprimer", "danger", this::delete);
    }

    @Override TableModel load() throws Exception {
        // Résout les références vers la salle et l'entraîneur pour l'affichage.
        return Db.table("""
            SELECT s.id_seance AS "ID", s.date_seance AS "Début", s.date_fin() AS "Fin",
                   s.duree_minutes AS "Durée (min)", s.salle.code_salle AS "Salle",
                   s.salle.club.nom_club AS "Club",
                   s.entraineur.prenom || ' ' || s.entraineur.nom AS "Entraîneur",
                   s.nb_participants() AS "Inscrits", s.salle.capacite AS "Capacité"
            FROM seances s ORDER BY s.date_seance""");
    }

    private void add() throws Exception {
        // Crée une séance liée à une salle et à un entraîneur existants.
        List<Object[]> salles = Db.rows("SELECT s.code_salle, s.type_salle, s.capacite FROM salles s ORDER BY 1");
        List<Object[]> coaches = Db.rows("SELECT e.id_personne, e.nom_complet() FROM entraineurs e ORDER BY 2");
        if (salles.isEmpty() || coaches.isEmpty()) {
            Util.info(this, "Il faut au moins une salle et un entraîneur.");
            return;
        }
        String[] so = salles.stream().map(r -> r[0] + " — " + r[1] + " (" + r[2] + " places)").toArray(String[]::new);
        String[] eo = coaches.stream().map(r -> r[0] + " — " + r[1]).toArray(String[]::new);
        List<String> f = Form.ask(this, "Nouvelle séance",
                Form.r("Début", LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)
                        .toString().replace('T', ' '), "AAAA-MM-JJ HH:mm"),
                Form.r("Durée (minutes)", "60", null),
                Form.c("Salle", null, so),
                Form.c("Entraîneur", null, eo));
        if (f == null) return;
        long id = Db.nextId("SELECT NVL(MAX(id_seance),0)+1 FROM seances");
        Db.exec("""
            INSERT INTO seances
            SELECT seance_t(?, ?, ?, REF(s), REF(e), membres_ref_nt())
            FROM salles s, entraineurs e WHERE s.code_salle = ? AND e.id_personne = ?""",
                id, Util.dateTime(f.get(0)), Integer.parseInt(f.get(1)),
                Util.key(f.get(2)), Long.parseLong(Util.key(f.get(3))));
        refresh();
    }

    private void delete() throws Exception {
        Object id = key();
        if (id == null) return;
        if (!Util.confirm(this, "Supprimer la séance " + id + " ?")) return;
        Db.exec("DELETE FROM seances WHERE id_seance = ?", id);
        refresh();
    }

    private void participants() {
        // Gère les références des membres dans la collection de participants.
        Object id = key();
        if (id == null) return;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Participants — séance " + id, Dialog.ModalityType.APPLICATION_MODAL);
        JTable t = new JTable();
        Util.styleTable(t);
        Runnable load = () -> {
            try {
                t.setModel(Db.table("""
                    SELECT m.id_personne AS "ID", m.nom_complet() AS "Participant", m.num_licence AS "Licence"
                    FROM seances s, TABLE(s.participants) p, membres m
                    WHERE s.id_seance = ? AND p.COLUMN_VALUE = REF(m) ORDER BY 2""", id));
            } catch (Exception e) { Util.error(dlg, e); }
        };
        JButton add = Util.button("Ajouter", "primary", () -> {
            // Ne propose que les membres qui ne participent pas déjà à cette séance.
            List<Object[]> rows = Db.rows("""
                SELECT m.id_personne, m.nom_complet() FROM membres m
                WHERE NOT EXISTS (SELECT 1 FROM seances s, TABLE(s.participants) p
                                  WHERE s.id_seance = ? AND p.COLUMN_VALUE = REF(m)) ORDER BY 2""", id);
            if (rows.isEmpty()) { Util.info(dlg, "Tous les membres participent déjà."); return; }
            String[] opts = rows.stream().map(r -> r[0] + " — " + r[1]).toArray(String[]::new);
            List<String> f = Form.ask(dlg, "Ajouter un participant", Form.c("Membre", null, opts));
            if (f == null) return;
            Number left = (Number) Db.scalar(
                    "SELECT s.nb_participants() - s.salle.capacite FROM seances s WHERE s.id_seance = ?", id);
            if (left.intValue() >= 0 && !Util.confirm(dlg, "La salle est déjà pleine. Ajouter quand même ?")) return;
            Db.exec("INSERT INTO TABLE(SELECT s.participants FROM seances s WHERE s.id_seance = ?) "
                    + "SELECT REF(m) FROM membres m WHERE m.id_personne = ?", id, Long.parseLong(Util.key(f.get(0))));
            load.run();
        });
        JButton remove = Util.button("Retirer", "danger", () -> {
            int r = t.getSelectedRow();
            if (r < 0) { Util.info(dlg, "Sélectionnez un participant."); return; }
            Db.exec("DELETE FROM TABLE(SELECT s.participants FROM seances s WHERE s.id_seance = ?) p "
                    + "WHERE p.COLUMN_VALUE = (SELECT REF(m) FROM membres m WHERE m.id_personne = ?)",
                    id, t.getValueAt(r, 0));
            load.run();
        });
        JButton close = Util.button("Fermer", "plain", dlg::dispose);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        btns.add(add);
        btns.add(remove);
        btns.add(close);
        dlg.setLayout(new BorderLayout());
        dlg.add(new JScrollPane(t), BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setSize(520, 420);
        dlg.setLocationRelativeTo(this);
        load.run();
        dlg.setVisible(true);
        refresh();
    }
}
