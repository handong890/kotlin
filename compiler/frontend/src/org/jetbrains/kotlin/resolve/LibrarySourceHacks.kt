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

package org.jetbrains.kotlin.resolve

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor

import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.callableDescriptorToDeclaration

public object LibrarySourceHacks {
    public val SKIP_TOP_LEVEL_MEMBERS: Key<Boolean> = Key.create<Boolean>("SKIP_TOP_LEVEL_MEMBERS") // used when analyzing library source

    public fun shouldSkip(member: CallableDescriptor): Boolean {
        val original = member.getOriginal() as? CallableMemberDescriptor ?: return false

        if (original.getContainingDeclaration() !is PackageFragmentDescriptor) return false

        val declaration = callableDescriptorToDeclaration(original) ?: return false

        val file = declaration.getContainingFile()
        return file != null && (file.getUserData<Boolean>(SKIP_TOP_LEVEL_MEMBERS) ?: false)
    }
}