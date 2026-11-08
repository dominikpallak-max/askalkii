package pl.fullbright;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceParser {
    private static final Pattern NUM = Pattern.compile(
            "(\\d{1,3}(?:[ .,]\\d{3})+|\\d+(?:[.,]\\d+)?)\\s*(mld|mln|[kmbt])?(?![a-ząęóśżźćńł])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern THOUSANDS = Pattern.compile("\\d{1,3}([ .,]\\d{3})+");

    public static String clean(String s) { return s == null ? "" : s.replaceAll("§.", ""); }

    /** Parsuje "100k", "1,5m", "1 500 000" itp. Zwraca -1 gdy się nie da. */
    public static long parseNumber(String s) {
        if (s == null) return -1;
        Matcher m = NUM.matcher(s);
        if (!m.find()) return -1;
        String n = m.group(1);
        String suf = m.group(2) == null ? "" : m.group(2).toLowerCase(Locale.ROOT);
        double v;
        try {
            if (!suf.isEmpty()) v = Double.parseDouble(n.replace(" ", "").replace(',', '.'));
            else if (THOUSANDS.matcher(n).matches()) v = Double.parseDouble(n.replaceAll("[ .,]", ""));
            else v = Double.parseDouble(n.replace(',', '.'));
        } catch (Exception e) { return -1; }
        double mult = switch (suf) {
            case "k" -> 1e3;
            case "m", "mln" -> 1e6;
            case "b", "mld" -> 1e9;
            case "t" -> 1e12;
            default -> 1;
        };
        return Math.round(v * mult);
    }

    /** Szuka ceny w lore przedmiotu. */
    public static long priceFromLore(List<String> lines, String keyword) {
        for (String raw : lines) {
            String l = clean(raw);
            String low = l.toLowerCase(Locale.ROOT);
            int idx = -1;
            if (keyword != null && !keyword.isBlank()) {
                idx = low.indexOf(keyword.toLowerCase(Locale.ROOT));
                if (idx < 0) continue;
                idx += keyword.length();
            } else {
                int d = l.indexOf('$');
                if (d >= 0) idx = d + 1;
                else {
                    int c = low.indexOf("cena");
                    if (c >= 0) idx = c + 4;
                    else { c = low.indexOf("price"); if (c >= 0) idx = c + 5; }
                }
            }
            if (idx < 0) continue;
            long v = parseNumber(l.substring(idx));
            if (v < 0) v = parseNumber(l);
            if (v >= 0) return v;
        }
        return -1;
    }

    private static String trim(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        return String.format(Locale.ROOT, "%.2f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static String format(long v) {
        long[] units = {1_000_000_000L, 1_000_000L, 1_000L};
        String[] suf = {"b", "m", "k"};
        for (int i = 0; i < units.length; i++)
            if (v >= units[i] && v % (units[i] / 100) == 0) return trim(v / (double) units[i]) + suf[i];
        return String.valueOf(v);
    }
}
