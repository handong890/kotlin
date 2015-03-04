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

package org.jetbrains.kotlin.idea.refactoring.introduce

import com.intellij.refactoring.actions.*
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*

public abstract class AbstractIntroduceAction : BasePlatformRefactoringAction() {
    {
        setInjectedContext(true)
    }

    override fun isAvailableInEditorOnly(): Boolean = true

    protected override fun isEnabledOnElements(elements: Array<out PsiElement>): Boolean =
            elements.all { it is JetElement }
}