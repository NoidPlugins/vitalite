package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import com.tonic.util.EntityOpsCompat;

/**
 * NPC API
 */
public class NpcAPI extends ActorAPI
{
    /**
     * Creates an instance of NpcQuery
     * @return NpcQuery
     */
    public static NpcQuery search()
    {
        return new NpcQuery();
    }

    /**
     * interact with an npc by int option
     * @param npc npc
     * @param option option
     */
    public static void interact(NpcEx npc, int option)
    {
        if (npc == null)
            return;

        interact(npc.getIndex(), option);
    }

    /**
     * interact with an npc by first matching action
     * @param npc npc
     * @param actions actions list
     */
    public static void interact(NpcEx npc, String... actions)
    {
        if(npc == null)
            return;

        TClient client = Static.getClient();
        Static.invoke(() ->
        {
            EntityOpsCompat.ActionResolution resolvedAction = EntityOpsCompat.resolveNpcAction(npc.getComposition(), actions);
            if (resolvedAction == null)
                return;

            ClickManager.click(ClickType.ACTOR);
            client.getPacketWriter().npcActionPacket(resolvedAction.actionIndex, npc.getIndex(), false, resolvedAction.subop);
        });
    }

    /**
     * interact with an npc by its index
     * @param npcIndex npc index
     * @param option option
     */
    public static void interact(int npcIndex, int option)
    {
        TClient client = Static.getClient();
        Static.invoke(() ->
        {
            ClickManager.click(ClickType.ACTOR);
            client.getPacketWriter().npcActionPacket(option, npcIndex, false);
        });
    }
}
