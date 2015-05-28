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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.di.InjectorForBodyResolve
import org.jetbrains.kotlin.idea.project.TargetPlatform
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.utils.Profiler

public interface IDEResolveTaskManager: ResolveTaskManager {
    public fun hasElementAdditionalResolveCached(function: JetNamedFunction): Boolean = false
}

class IDEResolveTaskManagerImpl(val globalContext: GlobalContext,
                                val project: Project,
                                val platform: TargetPlatform,
                                val resolveSession: ResolveSession): IDEResolveTaskManager {

    private val bodyResolve = InjectorForBodyResolve(
            globalContext.withProject(project).withModule(resolveSession.getModuleDescriptor()),
            resolveSession.getTrace(), platform.getAdditionalCheckerProvider(), StatementFilter.NONE,
            platform.getDynamicTypesSettings()
    ).getBodyResolver()

    val bodyResolveCache = CachedValuesManager.getManager(project).createCachedValue<MemoizedFunctionToNotNull<JetNamedFunction, BodyResolveResult>>(
            CachedValueProvider {
                val manager = resolveSession.getStorageManager()
                val functionsCacheFunction = manager.createMemoizedFunction<JetNamedFunction, BodyResolveResult> { function ->
                    doResolveFunctionBody(function)
                }

                CachedValueProvider.Result.create<MemoizedFunctionToNotNull<JetNamedFunction, BodyResolveResult>>(
                        functionsCacheFunction, PsiModificationTracker.MODIFICATION_COUNT, resolveSession.getExceptionTracker())
            }, false)

    override fun resolveFunctionBody(function: JetNamedFunction): BodyResolveResult {
        return bodyResolveCache.getValue().invoke(function)
    }

    override fun hasElementAdditionalResolveCached(function: JetNamedFunction): Boolean {
        if (!bodyResolveCache.hasUpToDateValue()) return false
        return bodyResolveCache.getValue().isComputed(function)
    }

    private fun doResolveFunctionBody(function: JetNamedFunction): BodyResolveResult {
        val profiler = Profiler.create("-- IDE -- ${Thread.currentThread().getName()} ${function.getName()} ${function.hashCode()} $this " +
                                       "${PsiManager.getInstance(function.getProject()).getModificationTracker().getModificationCount()}").start()

        val scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(function)
        val functionDescriptor = resolveSession.resolveToDescriptor(function) as FunctionDescriptor
        val dataFlowInfo = DataFlowInfo.EMPTY

        val bodyResolveResult = BodyResolveTaskManager.resolveFunctionBody(
                function, bodyResolve, BodyResolveContext(dataFlowInfo, resolveSession.getTrace(), functionDescriptor, scope))

        profiler.end()

        return bodyResolveResult
    }
}