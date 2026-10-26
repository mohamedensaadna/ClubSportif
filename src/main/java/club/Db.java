package club;

import java.sql.*;
import java.util.*;
import javax.swing.table.DefaultTableModel;

/** Accès à Oracle : connexion partagée et utilitaires pour les requêtes SQL. */
final class Db {
    
        static final String URL  = env("CLUB_DB_URL",  "jdbc:oracle:thin:@//localhost:1521/FREEPDB1");
        static final String USER = env("CLUB_DB_USER", "saadna");
        static final String PWD  = env("CLUB_DB_PWD",  null);

private static String env(String key, String def) {
    String v = System.getenv(key);
    if (v == null || v.isBlank()) {
        if (def == null) throw new IllegalStateException("Variable d'environnement manquante : " + key);
        return def;
    }
    return v;
}

    private static Connection cn;

    private Db() {}

    static synchronized Connection get() throws SQLException {
        // Ouvre une seule connexion et la recrée si elle a été fermée.
        if (cn == null || cn.isClosed()) {
            cn = DriverManager.getConnection(URL, USER, PWD);
            cn.setAutoCommit(true);
        }
        return cn;
    }

    private static void bind(PreparedStatement ps, Object[] p) throws SQLException {
        // Associe les paramètres en conservant les types de dates Java.
        for (int i = 0; i < p.length; i++) {
            Object v = p[i];
            if (v == null) ps.setNull(i + 1, Types.VARCHAR);
            else if (v instanceof java.time.LocalDate d) ps.setDate(i + 1, java.sql.Date.valueOf(d));
            else if (v instanceof java.time.LocalDateTime d) ps.setTimestamp(i + 1, Timestamp.valueOf(d));
            else ps.setObject(i + 1, v);
        }
    }

    private static Object read(ResultSet rs, ResultSetMetaData md, int i) throws SQLException {
        // Convertit les colonnes temporelles Oracle en horodatages JDBC.
        int t = md.getColumnType(i);
        if (t == Types.DATE || t == Types.TIMESTAMP) return rs.getTimestamp(i);
        return rs.getObject(i);
    }

    static DefaultTableModel table(String sql, Object... p) throws SQLException {
        // Transforme le résultat d'une requête en modèle non modifiable pour JTable.
        try (PreparedStatement ps = get().prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                DefaultTableModel m = new DefaultTableModel() {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };
                for (int i = 1; i <= n; i++) m.addColumn(md.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) row[i] = Util.cell(read(rs, md, i + 1));
                    m.addRow(row);
                }
                return m;
            }
        }
    }

    static List<Object[]> rows(String sql, Object... p) throws SQLException {
        // Retourne les lignes brutes, utilisées notamment pour préremplir les formulaires.
        try (PreparedStatement ps = get().prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<Object[]> out = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) row[i] = read(rs, md, i + 1);
                    out.add(row);
                }
                return out;
            }
        }
    }

    static Object scalar(String sql, Object... p) throws SQLException {
        List<Object[]> r = rows(sql, p);
        return r.isEmpty() ? null : r.get(0)[0];
    }

    static long nextId(String sql) throws SQLException {
        return ((Number) scalar(sql)).longValue();
    }

    static int exec(String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = get().prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate();
        }
    }

    /** Transaction : tout ou rien. */
    static void tx(Util.Act work) throws Exception {
        // Regroupe plusieurs opérations dans une transaction atomique.
        Connection c = get();
        c.setAutoCommit(false);
        try {
            work.run();
            c.commit();
        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }
}
