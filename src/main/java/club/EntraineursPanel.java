package club;

import java.util.*;
import java.util.List;
import javax.swing.table.TableModel;

class EntraineursPanel extends CrudPanel {
    EntraineursPanel() {
        super("Entraîneurs", "Type entraineur_t (hérite de personne_t) avec VARRAY de spécialités");
        action("Ajouter", "primary", this::add);
        action("Modifier", "plain", this::edit);
        action("Supprimer", "danger", this::delete);
    }

    @Override TableModel load() throws Exception {
        // Exploite les collections Oracle pour afficher téléphones et spécialités.
        return Db.table("""
            SELECT e.id_personne AS "ID", e.nom_complet() AS "Entraîneur", e.age() AS "Âge",
                   e.adresse.ville AS "Ville",
                   (SELECT LISTAGG(t.COLUMN_VALUE, ' / ') WITHIN GROUP (ORDER BY t.COLUMN_VALUE)
                      FROM TABLE(e.telephones) t) AS "Téléphones",
                   (SELECT LISTAGG(sp.COLUMN_VALUE, ', ') FROM TABLE(e.specialites) sp) AS "Spécialités",
                   (SELECT COUNT(*) FROM seances s WHERE s.entraineur = REF(e)) AS "Séances"
            FROM entraineurs e ORDER BY e.id_personne""");
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
            Form.r("Spécialités", Util.v(i, 7), "séparées par virgule (max 5)"));
    }

    private void add() throws Exception {
        // Construit les VARRAY à partir des listes saisies dans le formulaire.
        List<String> f = form("Nouvel entraîneur", null);
        if (f == null) return;
        List<String> tel = Util.list(f.get(2), 3), sp = Util.list(f.get(7), 5);
        List<Object> p = new ArrayList<>();
        p.add(Db.nextId("SELECT NVL(MAX(id),0)+1 FROM (SELECT id_personne id FROM membres UNION ALL SELECT id_personne FROM entraineurs)"));
        p.add(f.get(0));
        p.add(f.get(1));
        p.addAll(tel);
        p.add(Util.date(f.get(3)));
        p.add(f.get(4));
        p.add(f.get(5));
        p.add(f.get(6));
        p.addAll(sp);
        Db.exec("INSERT INTO entraineurs VALUES (entraineur_t(?,?,?,telephones_va(" + Util.ph(tel.size())
                + "),?,adresse_t(?,?,?),specialites_va(" + Util.ph(sp.size()) + ")))", p.toArray());
        refresh();
    }

    private void edit() throws Exception {
        Object id = key();
        if (id == null) return;
        Object[] cur = Db.rows("""
            SELECT e.nom, e.prenom,
                   (SELECT LISTAGG(t.COLUMN_VALUE, ', ') WITHIN GROUP (ORDER BY t.COLUMN_VALUE) FROM TABLE(e.telephones) t),
                   e.date_naissance, e.adresse.rue, e.adresse.ville, e.adresse.code_postal,
                   (SELECT LISTAGG(sp.COLUMN_VALUE, ', ') FROM TABLE(e.specialites) sp)
            FROM entraineurs e WHERE e.id_personne = ?""", id).get(0);
        List<String> f = form("Modifier l'entraîneur " + id, cur);
        if (f == null) return;
        List<String> tel = Util.list(f.get(2), 3), sp = Util.list(f.get(7), 5);
        List<Object> p = new ArrayList<>();
        p.add(f.get(0));
        p.add(f.get(1));
        p.addAll(tel);
        p.add(Util.date(f.get(3)));
        p.add(f.get(4));
        p.add(f.get(5));
        p.add(f.get(6));
        p.addAll(sp);
        p.add(id);
        Db.exec("UPDATE entraineurs e SET e.nom=?, e.prenom=?, e.telephones=telephones_va(" + Util.ph(tel.size())
                + "), e.date_naissance=?, e.adresse=adresse_t(?,?,?), e.specialites=specialites_va("
                + Util.ph(sp.size()) + ") WHERE e.id_personne=?", p.toArray());
        refresh();
    }

    private void delete() throws Exception {
        // Protège les séances qui référencent encore cet entraîneur.
        Object id = key();
        if (id == null) return;
        Number n = (Number) Db.scalar("SELECT COUNT(*) FROM seances s WHERE s.entraineur.id_personne = ?", id);
        if (n.intValue() > 0) {
            Util.info(this, "Impossible : cet entraîneur anime encore " + n + " séance(s).");
            return;
        }
        if (!Util.confirm(this, "Supprimer l'entraîneur « " + cell(1) + " » ?")) return;
        Db.exec("DELETE FROM entraineurs WHERE id_personne = ?", id);
        refresh();
    }
}
