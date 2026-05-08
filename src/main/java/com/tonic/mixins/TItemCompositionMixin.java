package com.tonic.mixins;

import com.tonic.api.TItemComposition;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.EntityOpsCompat;

@Mixin("ItemComposition")
public abstract class TItemCompositionMixin implements TItemComposition
{
    @Override
    public String[] getGroundActions()
    {
        return EntityOpsCompat.groundItemActions(this);
    }
}
