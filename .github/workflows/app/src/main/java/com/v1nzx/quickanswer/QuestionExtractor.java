package com.v1nzx.quickanswer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestionExtractor {

    public static String extractFirst(String html) {
        Document doc = Jsoup.parse(html);

        Element gf = doc.selectFirst("[role=heading], .M7eMe, .Qr7Oae");
        if (gf != null && gf.text().length() > 5) return gf.text().trim();

        Element qz = doc.selectFirst(".question-text, [class*=questionText]");
        if (qz != null) return qz.text().trim();

        Element gen = doc.selectFirst(".question-text, .soal, .pertanyaan, [class*=question]");
        if (gen != null && gen.text().length() > 5) return gen.text().trim();

        Elements ps = doc.select("p");
        for (Element p : ps) {
            String t = p.text().trim();
            if (t.length() > 20 && t.length() < 300) return t;
        }

        Pattern pat = Pattern.compile("(?:\\d+[\\.\\)]\\s*)(.{15,200}?)(?:\\?|\\n)", Pattern.DOTALL);
        Matcher m = pat.matcher(doc.text());
        if (m.find()) return m.group(1).trim();

        return "";
    }

    public static String parseGoogleAnswer(String html) {
        Document doc = Jsoup.parse(html);
        StringBuilder sb = new StringBuilder();

        Element snippet = doc.selectFirst(".hgKElc, .LGOjhe, [data-attrid=description]");
        if (snippet != null) {
            sb.append("SNIPPET:\n").append(snippet.text()).append("\n\n");
        }

        Elements results = doc.select(".g, .tF2Cxc");
        int count = 0;
        for (Element r : results) {
            if (count >= 3) break;
            Element title = r.selectFirst("h3");
            Element desc = r.selectFirst(".VwiC3b, .lEBKkf");
            if (title != null) {
                sb.append("> ").append(title.text()).append("\n");
                if (desc != null) sb.append("   ").append(desc.text()).append("\n\n");
                count++;
            }
        }

        if (sb.length() == 0) return "Ga nemu jawaban";
        return sb.toString();
    }
}
