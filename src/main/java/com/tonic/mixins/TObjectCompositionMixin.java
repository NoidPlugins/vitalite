package com.tonic.mixins;

import com.tonic.api.TBuffer;
import com.tonic.api.TObjectComposition;

import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.MethodHook;
import com.tonic.injector.annotations.Mixin;
import lombok.Getter;

@Getter
@Mixin("ObjectComposition")
public class TObjectCompositionMixin implements TObjectComposition
{
    @Inject
    private static int blockAccessFlags;

    @MethodHook("decodeNext")
    public static void decodeNext(TBuffer buffer, int opcode)
    {
        if(opcode == 69)
        {
            byte[] array = buffer.getArray();
            int offset = buffer.getOffset();
            if(array != null && offset >= 0 && offset < array.length)
            {
                blockAccessFlags = array[offset] & 0xFF;
            }
        }
    }

    @Override
    public int getBlockAccessFlags() {
        return blockAccessFlags;
    }
}
