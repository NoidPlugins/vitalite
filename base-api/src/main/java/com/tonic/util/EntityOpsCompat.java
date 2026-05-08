package com.tonic.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class EntityOpsCompat
{
    public static final class ActionResolution
    {
        public final int actionIndex;
        public final int subop;
        public final String actionName;
        public final boolean fromEntityOps;

        public ActionResolution(int actionIndex, int subop, String actionName, boolean fromEntityOps)
        {
            this.actionIndex = actionIndex;
            this.subop = subop;
            this.actionName = actionName;
            this.fromEntityOps = fromEntityOps;
        }
    }

    private static final int MAX_OPS = 5;

    private EntityOpsCompat()
    {
    }

    public static String[] groundItemActions(Object itemComposition)
    {
        return actions(resolveEntityOps(itemComposition), null);
    }

    public static String[] objectActions(Object composition)
    {
        return actions(resolveEntityOps(composition), invokeActions(composition));
    }

    public static String[] npcActions(Object composition)
    {
        return actions(resolveEntityOps(composition), invokeActions(composition));
    }

    private static String[] actions(Object entityOps, String[] legacyActions)
    {
        String[] actions = new String[MAX_OPS];
        for (int i = 0; i < MAX_OPS; i++)
        {
            if (entityOps != null)
            {
                actions[i] = TextUtil.sanitize(invokeString(entityOps, "getOp", new Class<?>[]{int.class}, new Object[]{i}));
            }

            if (actions[i] == null && legacyActions != null && i < legacyActions.length)
            {
                actions[i] = TextUtil.sanitize(legacyActions[i]);
            }
        }
        return actions;
    }

    public static List<String> groundItemActionList(Object itemComposition)
    {
        return listActions(resolveEntityOps(itemComposition), null);
    }

    public static List<String> objectActionList(Object composition)
    {
        return listActions(resolveEntityOps(composition), invokeActions(composition));
    }

    public static List<String> npcActionList(Object composition)
    {
        return listActions(resolveEntityOps(composition), invokeActions(composition));
    }

    public static ActionResolution resolveGroundItemAction(Object itemComposition, String... requestedActions)
    {
        return resolveAction(resolveEntityOps(itemComposition), null, requestedActions);
    }

    public static ActionResolution resolveObjectAction(Object composition, String... requestedActions)
    {
        return resolveAction(resolveEntityOps(composition), invokeActions(composition), requestedActions);
    }

    public static ActionResolution resolveNpcAction(Object composition, String... requestedActions)
    {
        return resolveAction(resolveEntityOps(composition), invokeActions(composition), requestedActions);
    }

    private static List<String> listActions(Object entityOps, String[] legacyActions)
    {
        List<String> actions = new ArrayList<>();
        if (entityOps != null)
        {
            for (int opIdx = 0; opIdx < MAX_OPS; opIdx++)
            {
                String op = TextUtil.sanitize(invokeString(entityOps, "getOp", new Class<?>[]{int.class}, new Object[]{opIdx}));
                if (op != null)
                {
                    actions.add(op);
                }

                Integer numSubOps = invokeInteger(entityOps, "getNumSubOps", new Class<?>[]{int.class}, new Object[]{opIdx});
                if (numSubOps == null || numSubOps <= 0)
                {
                    continue;
                }

                for (int subIdx = 0; subIdx < numSubOps; subIdx++)
                {
                    String subOp = TextUtil.sanitize(invokeString(entityOps, "getSubOp", new Class<?>[]{int.class, int.class}, new Object[]{opIdx, subIdx}));
                    if (subOp != null)
                    {
                        actions.add(subOp);
                    }
                }
            }
        }

        if (legacyActions != null)
        {
            for (String action : legacyActions)
            {
                action = TextUtil.sanitize(action);
                if (action != null && !containsIgnoreCase(actions, action))
                {
                    actions.add(action);
                }
            }
        }
        return actions;
    }

    private static ActionResolution resolveAction(Object entityOps, String[] legacyActions, String... requestedActions)
    {
        if (requestedActions == null || requestedActions.length == 0)
        {
            return null;
        }

        if (entityOps != null)
        {
            for (int opIdx = 0; opIdx < MAX_OPS; opIdx++)
            {
                String op = TextUtil.sanitize(invokeString(entityOps, "getOp", new Class<?>[]{int.class}, new Object[]{opIdx}));
                if (matchesAny(op, requestedActions))
                {
                    return new ActionResolution(opIdx, 0, op, true);
                }

                Integer numSubOps = invokeInteger(entityOps, "getNumSubOps", new Class<?>[]{int.class}, new Object[]{opIdx});
                if (numSubOps == null || numSubOps <= 0)
                {
                    continue;
                }

                for (int subIdx = 0; subIdx < numSubOps; subIdx++)
                {
                    String subOp = TextUtil.sanitize(invokeString(entityOps, "getSubOp", new Class<?>[]{int.class, int.class}, new Object[]{opIdx, subIdx}));
                    if (!matchesAny(subOp, requestedActions))
                    {
                        continue;
                    }

                    Integer subId = invokeInteger(entityOps, "getSubID", new Class<?>[]{int.class, int.class}, new Object[]{opIdx, subIdx});
                    int packedSubop = subId == null ? 0 : packSubId(opIdx, subId);
                    return new ActionResolution(opIdx, packedSubop, subOp, true);
                }
            }
        }

        if (legacyActions != null)
        {
            for (int i = 0; i < legacyActions.length; i++)
            {
                String action = TextUtil.sanitize(legacyActions[i]);
                if (matchesAny(action, requestedActions))
                {
                    return new ActionResolution(i, 0, action, false);
                }
            }
        }

        return null;
    }

    private static Object resolveEntityOps(Object composition)
    {
        Object entityOps = invoke(composition, "getOps", new Class<?>[0], new Object[0]);
        if (isEntityOps(entityOps))
        {
            return entityOps;
        }

        entityOps = findEntityOpsField(composition);
        if (entityOps != null)
        {
            return entityOps;
        }

        return findEntityOpsMethod(composition);
    }

    private static Object findEntityOpsField(Object composition)
    {
        if (composition == null)
        {
            return null;
        }

        Class<?> type = composition.getClass();
        while (type != null)
        {
            for (Field field : type.getDeclaredFields())
            {
                if (Modifier.isStatic(field.getModifiers()))
                {
                    continue;
                }

                try
                {
                    field.setAccessible(true);
                    Object value = field.get(composition);
                    if (isEntityOps(value))
                    {
                        return value;
                    }
                }
                catch (ReflectiveOperationException | RuntimeException ignored)
                {
                    // Keep scanning; this is a compatibility helper across obfuscated revisions.
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Object findEntityOpsMethod(Object composition)
    {
        if (composition == null)
        {
            return null;
        }

        for (Method method : composition.getClass().getMethods())
        {
            if (method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE)
            {
                continue;
            }

            try
            {
                Object value = method.invoke(composition);
                if (isEntityOps(value))
                {
                    return value;
                }
            }
            catch (ReflectiveOperationException | RuntimeException ignored)
            {
                // Keep scanning; some obfuscated accessors throw for optional state.
            }
        }
        return null;
    }

    private static boolean isEntityOps(Object value)
    {
        if (value == null)
        {
            return false;
        }

        Class<?> type = value.getClass();
        return hasMethod(type, "getOp", int.class)
            && hasMethod(type, "getNumSubOps", int.class)
            && hasMethod(type, "getSubID", int.class, int.class)
            && hasMethod(type, "getSubOp", int.class, int.class);
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes)
    {
        try
        {
            type.getMethod(name, parameterTypes);
            return true;
        }
        catch (NoSuchMethodException ignored)
        {
            return false;
        }
    }

    private static String[] invokeActions(Object composition)
    {
        Object value = invoke(composition, "getActions", new Class<?>[0], new Object[0]);
        return value instanceof String[] ? (String[]) value : null;
    }

    private static boolean matchesAny(String candidate, String... requestedActions)
    {
        if (candidate == null)
        {
            return false;
        }
        for (String requestedAction : requestedActions)
        {
            String requested = TextUtil.sanitize(requestedAction);
            if (requested != null && (candidate.equalsIgnoreCase(requested) || candidate.toLowerCase().contains(requested.toLowerCase())))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(List<String> actions, String action)
    {
        for (String candidate : actions)
        {
            if (candidate != null && candidate.equalsIgnoreCase(action))
            {
                return true;
            }
        }
        return false;
    }

    private static int packSubId(int opIdx, int subId)
    {
        return (subId + 1) << 8 | opIdx;
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args)
    {
        if (target == null)
        {
            return null;
        }

        try
        {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        }
        catch (ReflectiveOperationException | RuntimeException ignored)
        {
            return null;
        }
    }

    private static String invokeString(Object target, String methodName, Class<?>[] parameterTypes, Object[] args)
    {
        Object value = invoke(target, methodName, parameterTypes, args);
        return value instanceof String ? (String) value : null;
    }

    private static Integer invokeInteger(Object target, String methodName, Class<?>[] parameterTypes, Object[] args)
    {
        Object value = invoke(target, methodName, parameterTypes, args);
        return value instanceof Integer ? (Integer) value : null;
    }
}
