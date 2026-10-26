package club;

import java.util.List;
import javax.swing.table.TableModel;

class ClubsPanel extends CrudPanel {
    ClubsPanel() {
        super("Clubs", "Type club_t avec adresse_t (type composé partagé)");
        action("Ajouter", "primary", this::add);
        action("Modifier", "plain", this::edit);
        action("Supprimer", "danger", this::delete);
    }

    @Override TableModel load() throws Exception {
        // Lit les attributs de l'adresse composée et compte les salles liées par REF.
        return Db.table("""
            SELECT c.id_club AS "ID", c.nom_club AS "Club", c.adresse.rue AS "Rue",
                   c.adresse.ville AS "Ville", c.adresse.code_postal AS "Code postal",
                   (SELECT COUNT(*) FROM salles s WHERE s.club = REF(c)) AS "Salles"
            FROM clubs c ORDER BY c.id_club""");
    }

    private List<String> form(String title, Object[] i) {
        return Form.ask(this, title,
            Form.r("Nom du club", Util.v(i, 0), null),
            Form.t("Rue", Util.v(i, 1), null),
            Form.t("Ville", Util.v(i, 2), null),
            Form.t("Code postal", Util.v(i, 3), null));
    }

    private void add() throws Exception {
        // Crée le club avec un identifiant calculé et un objet adresse_t.
        List<String> f = form("Nouveau club", null);
        if (f == null) return;
        Db.exec("INSERT INTO clubs VALUES (club_t(?, ?, adresse_t(?,?,?)))",
                Db.nextId("SELECT NVL(MAX(id_club),0)+1 FROM clubs"), f.get(0), f.get(1), f.get(2), f.get(3));
        refresh();
    }

    private void edit() throws Exception {
        Object id = key();
        if (id == null) return;
        Object[] cur = Db.rows("SELECT c.nom_club, c.adresse.rue, c.adresse.ville, c.adresse.code_postal "
                + "FROM clubs c WHERE c.id_club = ?", id).get(0);
        List<String> f = form("Modifier le club " + id, cur);
        if (f == null) return;
        Db.exec("UPDATE clubs c SET c.nom_club=?, c.adresse=adresse_t(?,?,?) WHERE c.id_club=?",
                f.get(0), f.get(1), f.get(2), f.get(3), id);
        refresh();
    }

    private void delete() throws Exception {
        // Empêche la suppression si le club possède encore des salles.
        Object id = key();
        if (id == null) return;
        if (((Number) cell(5)).intValue() > 0) {
            Util.info(this, "Impossible : ce club possède encore des salles.");
            return;
        }
        if (!Util.confirm(this, "Supprimer le club « " + cell(1) + " » ?")) return;
        Db.exec("DELETE FROM clubs WHERE id_club = ?", id);
        refresh();
    }
}
