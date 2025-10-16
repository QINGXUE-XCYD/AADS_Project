import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class SimpleJsonParser {
    private final String text;
    private int index = 0;

    SimpleJsonParser(String text) {
        this.text = text;
    }

    // 从标准输入读取完整 JSON
    static Object parseFromInput(InputStream in) throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        SimpleJsonParser parser = new SimpleJsonParser(text);
        return parser.parseValue();
    }

    // --- 核心入口 ---
    Object parseValue() {
        skipWhitespace();
        char c = text.charAt(index);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == '-' || Character.isDigit(c)) return parseNumber();
        if (text.startsWith("true", index)) { index += 4; return Boolean.TRUE; }
        if (text.startsWith("false", index)) { index += 5; return Boolean.FALSE; }
        if (text.startsWith("null", index)) { index += 4; return null; }
        throw new RuntimeException("Unexpected character at " + index + ": " + c);
    }

    // --- 解析对象 ---
    Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        index++; // skip '{'
        skipWhitespace();
        if (text.charAt(index) == '}') { index++; return map; }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            if (text.charAt(index) != ':')
                throw new RuntimeException("Expected ':' at " + index);
            index++; // skip ':'
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char c = text.charAt(index);
            if (c == '}') { index++; break; }
            if (c == ',') { index++; continue; }
            throw new RuntimeException("Expected ',' or '}' at " + index);
        }
        return map;
    }

    // --- 解析数组 ---
    List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        index++; // skip '['
        skipWhitespace();
        if (text.charAt(index) == ']') { index++; return list; }
        while (true) {
            Object val = parseValue();
            list.add(val);
            skipWhitespace();
            char c = text.charAt(index);
            if (c == ']') { index++; break; }
            if (c == ',') { index++; continue; }
            throw new RuntimeException("Expected ',' or ']' at " + index);
        }
        return list;
    }

    // --- 解析字符串 ---
    String parseString() {
        StringBuilder sb = new StringBuilder();
        index++; // skip '"'
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '\\') {
                char next = text.charAt(index++);
                if (next == '"' || next == '\\' || next == '/') sb.append(next);
                else if (next == 'b') sb.append('\b');
                else if (next == 'f') sb.append('\f');
                else if (next == 'n') sb.append('\n');
                else if (next == 'r') sb.append('\r');
                else if (next == 't') sb.append('\t');
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // --- 解析数字 ---
    Double parseNumber() {
        int start = index;
        while (index < text.length() &&
              ("0123456789+-.eE".indexOf(text.charAt(index)) >= 0)) {
            index++;
        }
        return Double.valueOf(text.substring(start, index));
    }

    void skipWhitespace() {
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') index++;
            else break;
        }
    }
}
