import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 生成 README 顶部的 upstream 徽章。
 * <p>
 * 版式与算法复刻 shields.io 的 flat 样式：文本宽度按 11px Verdana 的字宽表计算，因此同样的文字生成的 SVG 与 shields.io 逐字节一致。
 * 构建时由 Maven 以单文件源码的方式调用，徽章随版本号自动更新，无须手动维护；内容未变时不会改写文件。
 * <p>
 * 用法：{@code java UpstreamBadge.java <徽章文字> <输出路径>}
 */
public class UpstreamBadge {

    private static final String LABEL = "upstream";

    /** 11px Verdana 下 ASCII 可打印字符（32 至 126）的字宽，取自 shields.io 所用的 anafanafo。 */
    private static final double[] WIDTHS = {
            3.87, 4.33, 5.05, 9, 6.99, 11.84, 7.99, 2.95, 5, 5, 6.99, 9, 4, 5, 4, 5, 6.99, 6.99, 6.99, 6.99,
            6.99, 6.99, 6.99, 6.99, 6.99, 6.99, 5, 5, 9, 9, 9, 6, 11, 7.52, 7.54, 7.68, 8.48, 6.96, 6.32, 8.53,
            8.27, 4.63, 5, 7.62, 6.12, 9.27, 8.23, 8.66, 6.63, 8.66, 7.65, 7.52, 6.78, 8.05, 7.52, 10.88, 7.54,
            6.77, 7.54, 5, 5, 5, 9, 6.99, 6.99, 6.61, 6.85, 5.73, 6.85, 6.55, 3.87, 6.85, 6.96, 3.02, 3.79,
            6.51, 3.02, 10.7, 6.96, 6.68, 6.85, 6.85, 4.69, 5.73, 4.33, 6.96, 6.51, 9, 6.51, 6.51, 5.78, 6.98,
            5, 6.98, 9
    };

    /** 与 shields.io 相同：取整后向上凑成奇数，以便文字落在整像素上。 */
    private static int preferredWidth(String text) {
        double width = 0;
        for (char c : text.toCharArray()) {
            // 字宽表以外的字符按 “m” 估算，与 shields.io 的回退方式一致。
            width += c >= 32 && c <= 126 ? WIDTHS[c - 32] : WIDTHS['m' - 32];
        }
        int floor = (int) width;
        return floor % 2 == 0 ? floor + 1 : floor;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("用法：java UpstreamBadge.java <徽章文字> <输出路径>");
            System.exit(1);
        }
        String message = args[0];
        Path output = Path.of(args[1]);
        int labelText = preferredWidth(LABEL);
        int messageText = preferredWidth(message);
        int labelWidth = labelText + 9;
        int messageWidth = messageText + 9;
        int totalWidth = labelWidth + messageWidth;
        String label = escape(LABEL);
        String text = escape(message);
        String title = label + ": " + text;
        int labelX = labelWidth * 5;
        int messageX = labelWidth * 10 + messageWidth * 5;
        int labelLength = (labelText - 1) * 10;
        int messageLength = (messageText - 1) * 10;
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + totalWidth + "\" height=\"20\" role=\"img\" aria-label=\"" + title + "\">"
                + "<title>" + title + "</title>"
                + "<linearGradient id=\"s\" x2=\"0\" y2=\"100%\"><stop offset=\"0\" stop-color=\"#bbb\" stop-opacity=\".1\"/><stop offset=\"1\" stop-opacity=\".1\"/></linearGradient>"
                + "<clipPath id=\"r\"><rect width=\"" + totalWidth + "\" height=\"20\" rx=\"3\" fill=\"#fff\"/></clipPath>"
                + "<g clip-path=\"url(#r)\"><rect width=\"" + labelWidth + "\" height=\"20\" fill=\"#555\"/>"
                + "<rect x=\"" + labelWidth + "\" width=\"" + messageWidth + "\" height=\"20\" fill=\"#1976d2\"/>"
                + "<rect width=\"" + totalWidth + "\" height=\"20\" fill=\"url(#s)\"/></g>"
                + "<g fill=\"#fff\" text-anchor=\"middle\" font-family=\"Verdana,Geneva,DejaVu Sans,sans-serif\" text-rendering=\"geometricPrecision\" font-size=\"110\">"
                + "<text aria-hidden=\"true\" x=\"" + labelX + "\" y=\"150\" fill=\"#010101\" fill-opacity=\".3\" transform=\"scale(.1)\" textLength=\"" + labelLength + "\">" + label + "</text>"
                + "<text x=\"" + labelX + "\" y=\"140\" transform=\"scale(.1)\" fill=\"#fff\" textLength=\"" + labelLength + "\">" + label + "</text>"
                + "<text aria-hidden=\"true\" x=\"" + messageX + "\" y=\"150\" fill=\"#010101\" fill-opacity=\".3\" transform=\"scale(.1)\" textLength=\"" + messageLength + "\">" + text + "</text>"
                + "<text x=\"" + messageX + "\" y=\"140\" transform=\"scale(.1)\" fill=\"#fff\" textLength=\"" + messageLength + "\">" + text + "</text>"
                + "</g></svg>\n";
        byte[] bytes = svg.getBytes(StandardCharsets.UTF_8);
        if (Files.exists(output) && Arrays.equals(Files.readAllBytes(output), bytes)) {
            return;
        }
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(output, bytes);
    }
}
