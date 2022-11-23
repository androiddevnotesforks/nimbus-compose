package com.zup.nimbus.processor.codegen.function

import com.zup.nimbus.processor.utils.isEnum
import com.zup.nimbus.processor.utils.isList
import com.zup.nimbus.processor.utils.isMap
import com.zup.nimbus.processor.utils.isPrimitive

/**
 * Writes the code for deserializing a common property, i.e. a property that is not annotated with
 * `@Root` or `@Composable` and is not a DeserializationContext.
 */
internal object CommonProperty {
    fun write(ctx: FunctionWriterContext) {
        val deserializer = CustomDeserialized.findDeserializer(ctx)
        val type = ctx.property.type
        when {
            deserializer != null -> CustomDeserialized.write(ctx, deserializer)
            type.isPrimitive() -> PrimitiveType.write(ctx)
            type.isEnum() -> EnumType.write(ctx)
            type.isFunctionType -> EventType.write(ctx)
            type.isList() -> ListMapType.writeList(ctx)
            type.isMap() -> ListMapType.writeMap(ctx)
            else -> AutoDeserialized.write(ctx)
        }
    }
}
