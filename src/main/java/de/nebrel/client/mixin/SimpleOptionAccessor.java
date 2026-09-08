package de.nebrel.client.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Writes a {@link SimpleOption}'s value past its validator.
 *
 * <p>The brightness option clamps to 0.0-1.0 through its callbacks, so
 * {@code setValue} cannot express full bright. Setting the backing field
 * directly is the least invasive way to do it: no render hook, no lightmap
 * mixin, and the user's own value is restored untouched when the module is
 * switched off.</p>
 *
 * @param <T> the option's value type
 */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {

    @Accessor("value")
    void nebrel$setValue(T value);

    @Accessor("value")
    T nebrel$getValue();
}
