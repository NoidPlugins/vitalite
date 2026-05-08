package com.tonic.mixins;

import com.tonic.injector.annotations.*;

import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin("Client")
public class TLoginHashMixin
{
    @Shadow("packedCallStack1")
    private static String packedClassStack1;

    @Shadow("packedCallStack2")
    private static String packedClassStack2;

    @MethodOverride("callStackPacker1")
    public static void callStackPacker1()
    {
        packedClassStack1 = packStackPart((System.currentTimeMillis() % 1000) + "\0", 3);
    }

    @MethodOverride("callStackPacker2")
    public static void callStackPacker2()
    {
        packedClassStack2 = packStackPart((System.currentTimeMillis() % 1000) + "\0", 3);
    }

    @MethodOverride("callStackCheck")
    public static String callStackCheck(long l) {
        try {
            Pattern stackFramePattern = Pattern.compile("\\[?([^,]*/)?([^,]*\\.)?([^.,]*)\\.([^,.]+)\\(([^,:]+:)?([^,:)]*)\\)(, |\\])");
            Matcher matcher = stackFramePattern.matcher(Arrays.toString(new RuntimeException().getStackTrace()));
            StringBuffer packedStack = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(packedStack, packStackFrame(matcher.toMatchResult()));
            }
            matcher.appendTail(packedStack);
            return packStackPart(packedStack.toString(), 119);
        } catch (Exception e) {
            return String.valueOf(e);
        }
    }

    @Inject
    private static String packStackPart(String value, int maxLength) {
        if (value.length() > maxLength) {
            return value.substring(0, maxLength) + "+";
        }
        return value;
    }

    @Inject
    private static String packStackFrame(MatchResult match) {
        String packageName = match.group(2);
        if (isSuppressedStackPackage(packageName)) {
            return "";
        }

        String abbreviatedPackage = packageName == null
                ? ""
                : Pattern.compile("(.)[^.]*\\.").matcher(packageName).replaceAll("$1") + ".";

        return Matcher.quoteReplacement(
                abbreviatedPackage
                        + packStackPart(match.group(3), 12)
                        + match.group(6)
                        + packStackPart(match.group(4), 12)
                        + "\n");
    }

    @Inject
    private static boolean isSuppressedStackPackage(String packageName) {
        if (packageName == null) {
            return false;
        }

        switch (packageName.hashCode()) {
            case -688050619: // java.lang.reflect.
            case 575645442:  // java.lang.invoke.
            case 1227444965: // jdk.internal.reflect.
            case 1675929244:
                return true;
            default:
                return false;
        }
    }
}
