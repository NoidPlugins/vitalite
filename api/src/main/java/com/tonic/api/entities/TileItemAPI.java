package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.queries.TileItemQuery;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import com.tonic.util.EntityOpsCompat;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

/**
 * TileItem API
 */
public class TileItemAPI
{
    /**
     * Creates an instance of TileItemQuery
     * @return TileItemQuery
     */
    public static TileItemQuery search()
    {
        return new TileItemQuery();
    }

    /**
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItemEx item, int action)
    {
        interact(item, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param action action
     * @param ctrlDown is control held
     */

    public static void interact(TileItemEx item, int action, boolean ctrlDown)
    {
        if (item == null)
            return;

        interact(action, item.getId(), item.getWorldPoint().getX(), item.getWorldPoint().getY(), ctrlDown);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param actions actions
     */
    public static void interact(TileItemEx item, String... actions)
    {
        if (item == null)
            return;

        EntityOpsCompat.ActionResolution action = getActionResolution(item, actions);
        if (action == null)
            return;

        interact(action.actionIndex, item.getId(), item.getWorldPoint().getX(), item.getWorldPoint().getY(), false, action.subop);
    }

    /**
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItemEx item, String action)
    {
        interact(item, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param action action
     * @param ctrlDown is control held
     */
    public static void interact(TileItemEx item, String action, boolean ctrlDown)
    {
        if (item == null)
            return;

        EntityOpsCompat.ActionResolution resolvedAction = getActionResolution(item, action);
        if (resolvedAction == null)
            return;

        interact(resolvedAction.actionIndex, item.getId(), item.getWorldPoint().getX(), item.getWorldPoint().getY(), ctrlDown, resolvedAction.subop);
    }

    /**
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItem item, WorldPoint location, int action)
    {
        interact(item, location, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param location world point
     * @param action action index
     * @param ctrlDown is control held
     */
    public static void interact(TileItem item, WorldPoint location, int action, boolean ctrlDown)
    {
        if (item == null)
            return;

        interact(action, item.getId(), location.getX(), location.getY(), ctrlDown);
    }

    /**
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItem item, WorldPoint location, String action)
    {
        interact(item, location, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param location world point
     * @param action action
     * @param ctrl is control held
     */
    public static void interact(TileItem item, WorldPoint location, String action, boolean ctrl)
    {
        if (item == null)
            return;

        EntityOpsCompat.ActionResolution resolvedAction = Static.invoke(() -> {
            Client client = Static.getClient();
            ItemComposition itemComp = client.getItemDefinition(item.getId());
            return EntityOpsCompat.resolveGroundItemAction(itemComp, action);
        });
        if (resolvedAction == null)
            return;

        interact(resolvedAction.actionIndex, item.getId(), location.getX(), location.getY(), ctrl, resolvedAction.subop);
    }

    /**
     * interact with a tile item without holding down control
     * @param action action index
     * @param identifier item id
     * @param worldX world point x
     * @param worldY world point y
     */
    public static void interact(int action, int identifier, int worldX, int worldY)
    {
        interact(action, identifier, worldX, worldY, false);
    }

    /**
     * interact with a tile item
     * @param action action index
     * @param identifier item id
     * @param worldX world point x
     * @param worldY world point y
     * @param ctrlDown is control held
     */

    public static void interact(int action, int identifier, int worldX, int worldY, boolean ctrlDown)
    {
        interact(action, identifier, worldX, worldY, ctrlDown, 0);
    }

    /**
     * interact with a tile item
     * @param action action index
     * @param identifier item id
     * @param worldX world point x
     * @param worldY world point y
     * @param ctrlDown is control held
     * @param subop packed subop index
     */
    public static void interact(int action, int identifier, int worldX, int worldY, boolean ctrlDown, int subop)
    {
        Client client = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN) || action < 0)
            return;

        TClient tClient = Static.getClient();
        Static.invoke(() ->
        {
            ClickManager.click(ClickType.GROUND_ITEM);
            tClient.getPacketWriter().groundItemActionPacket(action, identifier, worldX, worldY, ctrlDown, subop);
        });
    }

    private static EntityOpsCompat.ActionResolution getActionResolution(TileItemEx item, String... actions)
    {
        if(item == null || actions == null || actions.length == 0)
            return null;

        return Static.invoke(() -> {
            Client client = Static.getClient();
            ItemComposition itemComp = client.getItemDefinition(item.getId());
            EntityOpsCompat.ActionResolution resolved = EntityOpsCompat.resolveGroundItemAction(itemComp, actions);
            if (resolved != null)
                return resolved;

            for (String action : actions)
            {
                int actionIndex = getActionIndex(item.getActions(), action);
                if (actionIndex != -1)
                    return new EntityOpsCompat.ActionResolution(actionIndex, 0, action, false);
            }
            return null;
        });
    }

    private static int getActionIndex(TileItemEx item, String... actions)
    {
        if(item == null || item.getActions() == null || actions == null || actions.length == 0)
            return -1;

        for(String action : actions)
        {
            int index = getActionIndex(item.getActions(), action);
            if(index != -1)
                return index;
        }
        return -1;
    }

    private static int getActionIndex(TileItemEx item, String action)
    {
        if(item == null || item.getActions() == null || action == null)
            return -1;

        return getActionIndex(item.getActions(), action);
    }

    private static int getActionIndex(String[] actions, String action)
    {
        for(int i = 0; i < actions.length; i++)
        {
            if(actions[i] == null)
                continue;

            if(action.equalsIgnoreCase(actions[i]))
                return i;
        }

        return -1;
    }
}
