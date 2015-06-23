/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.scopes.JetScope
import kotlin.jvm.internal.FunctionImpl
import kotlin.jvm.internal.FunctionReference
import kotlin.reflect.KDeclarationContainer
import kotlin.reflect.KFunction
import kotlin.reflect.KotlinReflectionInternalError

open class KFunctionImpl protected constructor(
        container: KCallableContainerImpl,
        signature: String,
        descriptorInitialValue: FunctionDescriptor?
) : KFunction<Any?>, FunctionImpl() {
    constructor(container: KCallableContainerImpl, signature: String): this(container, signature, null)

    constructor(container: KCallableContainerImpl, descriptor: FunctionDescriptor): this(
            container, RuntimeTypeMapper.mapSignature(descriptor), descriptor
    )

    protected val descriptor: FunctionDescriptor by ReflectProperties.lazySoft<FunctionDescriptor>(descriptorInitialValue) {
        container.findFunctionDescriptor(signature)
    }

    // TODO: test
    override val name: String get() = descriptor.getName().asString()

    override fun getArity(): Int {
        // TODO: test?
        return descriptor.getValueParameters().size() +
               (if (descriptor.getDispatchReceiverParameter() != null) 1 else 0) +
               (if (descriptor.getExtensionReceiverParameter() != null) 1 else 0)
    }

    override fun equals(other: Any?): Boolean =
            other is KFunctionImpl && descriptor == other.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderFunction(descriptor)
}

class KFunctionFromReferenceImpl(
        val reference: FunctionReference
): KFunctionImpl(
        reference.getOwner() as? KCallableContainerImpl ?: EmptyContainerForLocal,
        reference.getSignature()
) {
    override fun getArity() = reference.getArity()

    override fun invoke(): Any? = reference()

    override fun invoke(p1: Any?): Any? = reference(p1)

    override fun invoke(p1: Any?, p2: Any?): Any? = reference(p1, p2)

    override fun invoke(p1: Any?, p2: Any?, p3: Any?): Any? = reference(p1, p2, p3)

    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?): Any? = reference(p1, p2, p3, p4)

    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?): Any? = reference(p1, p2, p3, p4, p5)

    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?): Any? = reference(p1, p2, p3, p4, p5, p6)

    // TODO
}

object EmptyContainerForLocal : KCallableContainerImpl() {
    override val jClass: Class<*>
        get() = fail()

    override val scope: JetScope
        get() = fail()

    private fun fail() = throw KotlinReflectionInternalError("Introspecting local functions is not yet supported in Kotlin reflection")
}
