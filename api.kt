interface KCallable<R> {
    fun call(vararg arg: Any?): R
}



interface KProperty<R> : KCallable<R> {
    val getter: Getter<R>

    interface Accessor<R> {
        val property: KProperty<R>
    }
    interface Getter<R> : Accessor<R>, KFunction<R>
}

interface KProperty0<R> : KProperty<R> {
    fun get(): R

    override val getter: Getter<R>

    interface Accessor<R> : KProperty.Accessor<R> {
        override val property: KProperty0<R>
    }
    interface Getter<R> : Accessor<R>, KProperty.Getter<R>
}

interface KProperty1<T, R> : KProperty<R> {
    fun get(receiver: T): R

    ...
}

interface KProperty2<D, E, R> : KProperty<R> {
    fun get(instance: D, receiver: E): R

    ...
}





interface KFunction<R> : KCallable<R>






interface KDeclarationContainer {
    val properties: Collection<KProperty<*>>

    val functions: Collection<KFunction<*>>
}

interface KClass<T> : KDeclarationContainer

interface KPackage : KDeclarationContainer




val KClass<T>.staticProperties: Collection<KProperty0<*>>

val KClass<T>.memberProperties: Collection<KProperty1<T, *>>

val KClass<T>.memberExtensionProperties: Collection<KProperty2<T, *, *>>


val KPackage.variables: Collection<KProperty0<*>>

val KPackage.extensionProperties: Collection<KProperty1<*, *>>
