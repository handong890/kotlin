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

package org.jetbrains.kotlin.container

import java.beans.Introspector
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.ArrayList

/**
 * Thrown by the form builder if an invalid model property is specified.
 */
class InvalidPropertyException(val modelClass : Class<out Any>, val property : String) : RuntimeException("Invalid property ${property} on type ${modelClass.getName()}") {

}

fun Class<out Any>.objectInstance(): Any? {
    try {
        val field = getDeclaredField("INSTANCE\$")
        if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
            return field.get(null)
        }
        return null
    }
    catch (e: NoSuchFieldException) {
        return null
    }
}

fun Class<out Any>.classObjectInstance(): Any? {
    try {
        val field = getDeclaredField("OBJECT\$")
        if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
            return field.get(null)
        }
        return null
    }
    catch (e: NoSuchFieldException) {
        return null
    }
}

fun String?.asNotEmpty(): String? = if (this == null) null else if (!isEmpty()) this else null

fun String.appendPathElement(part : String) : String {
    val b = StringBuilder()
    b.append(this)
    if (!this.endsWith("/")) {
        b.append("/")
    }

    if (part.startsWith('/')) {
        b.append(part.substring(1))
    }
    else {
        b.append(part)
    }

    return b.toString()
}

fun Class<out Any>.propertyGetter(property: String): Method {
    try {
        return getMethod("get${when {
            property.length() == 1 && property[0].isLowerCase() -> property.capitalize()
            property.length() > 2 && property[1].isLowerCase() -> property.capitalize()
            else -> property
        }}")
    }
    catch (e: Exception) {
        throw InvalidPropertyException(this, property)
    }
}

fun Class<out Any>.propertySetter(property: String): Method {
    try {
        return getMethod("set${when {
            property.length() == 1 && property[0].isLowerCase() -> property.capitalize()
            property.length() > 2 && property[1].isLowerCase() -> property.capitalize()
            else -> property
        }}")
    }
    catch (e: Exception) {
        throw InvalidPropertyException(this, property)
    }
}

fun Class<out Any>.propertyReadonly(property: String): Boolean {
    try {
        val getter = propertyGetter(property)
        val valueType = getter.getReturnType()
        if (valueType == null)
            return true

        getMethod("set${when {
            property.length() == 1 && property[0].isLowerCase() -> property.capitalize()
            property.length() > 2 && property[1].isLowerCase() -> property.capitalize()
            else -> property
        }}", valueType)
        return false
    }
    catch (e: Exception) {
        return true
    }
}

fun Any.propertyValue(property: String): Any? {
    val getter = javaClass.propertyGetter(property)
    return getter.invoke(this)
}

fun Class<out Any>.properties(): List<String> {
    val answer = ArrayList<String>()

    for (method in getDeclaredMethods()) {
        val name = method.getName()
        if (name.startsWith("get") && method.getParameterTypes().size() == 0) {
            answer.add(Introspector.decapitalize(name.substring(3)))
        }
    }

    return answer.sort()
}
