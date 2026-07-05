package patchlib.debug.data;

import java.util.ArrayList;
import java.util.List;

/** Turns registry member keys and JVM descriptors into readable labels. A member key looks like
 * "com.fs.starfarer.campaign.CampaignState#processInput(Lcom/fs/starfarer/util/A/new;F)V". Coupled to
 * PatchInstaller.memberKey; if that format changes, update this too. */
public final class DescriptorFormat {

    private DescriptorFormat() { }

    /** Class name portion of a member key (before the '#'). */
    public static String className(String memberKey) {
        int hash = memberKey.indexOf('#');
        return hash < 0 ? memberKey : memberKey.substring(0, hash);
    }

    /** Method label like "processInput(new, float)" from a member key. Falls back to the raw member on parse failure. */
    public static String memberLabel(String memberKey) {
        int hash = memberKey.indexOf('#');
        String rest = hash < 0 ? memberKey : memberKey.substring(hash + 1);
        int paren = rest.indexOf('(');
        if (paren < 0) return rest;
        String name = rest.substring(0, paren);
        return name + "(" + formatParams(rest.substring(paren)) + ")";
    }

    /** Simple-name class plus member label, like "SomeClass.callee(int)". */
    public static String memberWithClass(String memberKey) {
        if (memberKey == null || memberKey.isEmpty()) return "";
        return simpleName(className(memberKey)) + "." + memberLabel(memberKey);
    }

    public static String simpleName(String name) {
        int slash = Math.max(name.lastIndexOf('.'), name.lastIndexOf('/'));
        return slash < 0 ? name : name.substring(slash + 1);
    }

    private static String formatParams(String descriptor) {
        int end = descriptor.indexOf(')');
        String params = descriptor.substring(1, end < 0 ? descriptor.length() : end);
        List<String> types = new ArrayList<>();
        int i = 0;
        while (i < params.length()) {
            int arrays = 0;
            while (i < params.length() && params.charAt(i) == '[') { arrays++; i++; }
            if (i >= params.length()) break;
            char c = params.charAt(i);
            String type;
            if (c == 'L') {
                int semi = params.indexOf(';', i);
                if (semi < 0) { type = simpleName(params.substring(i + 1)); i = params.length(); }
                else { type = simpleName(params.substring(i + 1, semi).replace('/', '.')); i = semi + 1; }
            } else {
                type = primitive(c);
                i++;
            }
            for (int a = 0; a < arrays; a++) type += "[]";
            types.add(type);
        }
        return String.join(", ", types);
    }

    private static String primitive(char c) {
        return switch (c) {
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'D' -> "double";
            case 'F' -> "float";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'S' -> "short";
            case 'Z' -> "boolean";
            case 'V' -> "void";
            default -> String.valueOf(c);
        };
    }
}
