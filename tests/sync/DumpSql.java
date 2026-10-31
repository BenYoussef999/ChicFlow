import com.chicflow.tunisia.db.ItemSql;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class DumpSql {
    public static void main(String[] a) throws Exception {
        for (Field f : ItemSql.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                System.out.println("SQL\t" + f.getName() + "\t" + f.get(null));
            }
        }
        // wardrobe screen queries: userId, search, category, color, sort
        Object[][] cases = {
            {1, "", null, null, 0}, {1, null, null, null, 1}, {1, "navy", null, null, 0},
            {1, "white", null, null, 1}, {1, "", "shirt", null, 2}, {1, "", null, "White", 3},
            {1, "", "shoes", "White", 0}, {1, "100%", null, null, 0}, {1, "_", null, null, 0},
            {1, "a\\b", null, null, 0}, {1, "' OR '1'='1", null, null, 0}, {1, "'; DROP TABLE clothing_items;--", null, null, 0},
            {2, "", null, null, 0}
        };
        for (Object[] c : cases) {
            ItemSql.Query q = ItemSql.wardrobeQuery((Integer) c[0], (String) c[1], (String) c[2], (String) c[3], (Integer) c[4]);
            StringBuilder sb = new StringBuilder("Q\t" + String.join("|", String.valueOf(c[0]), String.valueOf(c[1]), String.valueOf(c[2]), String.valueOf(c[3]), String.valueOf(c[4])) + "\t" + q.sql);
            for (String arg : q.args) sb.append("\t").append(arg);
            System.out.println(sb);
        }
    }
}
