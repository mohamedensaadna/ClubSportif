package club;

import java.util.List;
import javax.swing.table.TableModel;

class SallesPanel extends CrudPanel {
    SallesPanel() {
        super("Salles", "Type salle_t avec REF club_t (SCOPE FOR clubs)");
        action("Ajouter", "primary", this::add);
        action("Modifier", "plain", this::edit);
        action("Supprimer", "danger", this::delete);
    }

    @Override TableModel load() throws Exception {
        // Affiche les salles, leur club référencé et le nombre de séances associées.
        return Db.table("""
            SELECT s.code_salle AS "Code", s.type_salle AS "Type", s.capacite AS "Capacité",
                   s.club.nom_club AS "Club",
                   (SELECT COUNT(*) FROM seances x WHERE x.salle = REF(s)) AS "Séances"
            FROM salles s ORDER BY s.code_salle""");
    }

    private void add() throws Exception {
        // Propose un club existant puis enregistre sa référence Oracle (REF).
        List<Object[]> clubs = Db.rows("SELECT c.id_club, c.nom_club FROM clubs c ORDER BY 2");
        if (clubs.isEmpty()) { Util.info(this, "Créez d'abord un club."); return; }
        String[] opts = clubs.stream().map(r -> r[0] + " — " + r[1]).toArray(String[]::new);
        List<String> f = Form.ask(this, "Nouvelle salle",
                Form.r("Code", "", "ex. S03"),
                Form.r("Type", "", "Fitness, Yoga, Boxe…"),
                Form.r("Capacité", "20", null),
                Form.c("Club", null, opts));
        if (f == null) return;
        Db.exec("INSERT INTO salles SELECT salle_t(?, ?, ?, REF(c)) FROM clubs c WHERE c.id_club = ?",
                f.get(0), Integer.parseInt(f.get(2)), f.get(1), Long.parseLong(Util.key(f.get(3))));
        refresh();
    }

    private void edit() throws Exception {
        Object id = key();
        if (id == null) return;
        List<String> f = Form.ask(this, "Modifier la salle " + id,
                Form.r("Type", String.valueOf(cell(1)), null),
                Form.r("Capacité", String.valueOf(cell(2)), null));
        if (f == null) return;
        Db.exec("UPDATE salles s SET s.type_salle=?, s.capacite=? WHERE s.code_salle=?",
                f.get(0), Integer.parseInt(f.get(1)), id);
        refresh();
    }

    private void delete() throws Exception {
        // Une salle utilisée par une séance ne peut pas être supprimée.
        Object id = key();
        if (id == null) return;
        if (((Number) cell(4)).intValue() > 0) {
            Util.info(this, "Impossible : cette salle est utilisée par des séances.");
            return;
        }
        if (!Util.confirm(this, "Supprimer la salle « " + id + " » ?")) return;
        Db.exec("DELETE FROM salles WHERE code_salle = ?", id);
        refresh();
    }
}
