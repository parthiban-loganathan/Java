package lab.sql;

import java.util.ArrayList;
import java.util.List;

/** Outcome of one SQL statement. */
public final class Result {
    public boolean query;
    public String[] labels = new String[0];
    public String[] names = new String[0];
    public int[] sqlTypes = new int[0];
    public String[] typeNames = new String[0];
    public int[] scales = new int[0];          // DECIMAL(p,s) columns: s; otherwise -1
    public String table = "";
    public List<Object[]> rows = new ArrayList<>();
    public long updateCount = -1;
    public List<Long> generatedKeys = new ArrayList<>();
}
