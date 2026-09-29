/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ortus.boxlang.runtime.azure;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Unit tests for the compile/load/cache logic in {@link AzureFunctionRunner} —
 * focuses on compilation correctness and cache behaviour (cold vs. warm invocations).
 */
public class HandlerCacheTest {

	/** Shared runner whose static block starts the BoxLang runtime once */
	private static final AzureFunctionRunner runner = new AzureFunctionRunner(
	    Path.of( "src", "test", "resources", "Lambda.bx" ), true
	);

	@BeforeEach
	public void clearCache() {
		AzureFunctionRunner.clearHandlerCache();
	}

	private IBoxContext buildContext( Path classPath ) {
		return new ScriptingRequestBoxContext(
		    runner.getRuntime().getRuntimeContext(),
		    FileSystemUtil.createFileUri( classPath.toAbsolutePath().toString() )
		);
	}

	@Test
	@DisplayName( "Compiles Lambda.bx on first access and caches it (cold invocation, production mode)" )
	public void testColdCompilation() {
		Path				path		= Path.of( "src", "test", "resources", "Lambda.bx" ).toAbsolutePath();
		ResolvedFilePath	resolved	= ResolvedFilePath.of( path );
		IBoxContext			context		= buildContext( path );

		assertThat( AzureFunctionRunner.isHandlerCached( path.toString() ) ).isFalse();

		IClassRunnable compiled = AzureFunctionRunner.getOrCompileHandler( resolved, context, false );

		assertThat( compiled ).isNotNull();
		assertThat( AzureFunctionRunner.isHandlerCached( path.toString() ) ).isTrue();
	}

	@Test
	@DisplayName( "Returns the same instance on subsequent warm invocations (production mode)" )
	public void testWarmInvocationReturnsCachedInstance() {
		Path				path		= Path.of( "src", "test", "resources", "Lambda.bx" ).toAbsolutePath();
		ResolvedFilePath	resolved	= ResolvedFilePath.of( path );
		IBoxContext			context		= buildContext( path );

		IClassRunnable		first		= AzureFunctionRunner.getOrCompileHandler( resolved, context, false );
		IClassRunnable		second		= AzureFunctionRunner.getOrCompileHandler( resolved, context, false );

		// Same reference — no recompilation on warm call
		assertThat( first ).isSameInstanceAs( second );
	}

	@Test
	@DisplayName( "Debug mode skips cache — always recompiles" )
	public void testDebugModeSkipsCache() {
		Path				path		= Path.of( "src", "test", "resources", "Lambda.bx" ).toAbsolutePath();
		ResolvedFilePath	resolved	= ResolvedFilePath.of( path );
		IBoxContext			context		= buildContext( path );

		IClassRunnable		first		= AzureFunctionRunner.getOrCompileHandler( resolved, context, true );
		IClassRunnable		second		= AzureFunctionRunner.getOrCompileHandler( resolved, context, true );

		// Always a fresh compile — never cached in debug mode
		assertThat( AzureFunctionRunner.isHandlerCached( path.toString() ) ).isFalse();
		assertThat( first ).isNotSameInstanceAs( second );
	}

	@Test
	@DisplayName( "Compiles Products.bx independently from Lambda.bx" )
	public void testMultipleClassesInCache() {
		Path			lambdaPath		= Path.of( "src", "test", "resources", "Lambda.bx" ).toAbsolutePath();
		Path			productsPath	= Path.of( "src", "test", "resources", "Products.bx" ).toAbsolutePath();
		IBoxContext		lambdaCtx		= buildContext( lambdaPath );
		IBoxContext		productsCtx		= buildContext( productsPath );

		IClassRunnable	lambda			= AzureFunctionRunner.getOrCompileHandler( ResolvedFilePath.of( lambdaPath ), lambdaCtx, false );
		IClassRunnable	products		= AzureFunctionRunner.getOrCompileHandler( ResolvedFilePath.of( productsPath ), productsCtx, false );

		assertThat( lambda ).isNotNull();
		assertThat( products ).isNotNull();
		assertThat( lambda ).isNotSameInstanceAs( products );
		assertThat( AzureFunctionRunner.isHandlerCached( lambdaPath.toString() ) ).isTrue();
		assertThat( AzureFunctionRunner.isHandlerCached( productsPath.toString() ) ).isTrue();
	}

	@Test
	@DisplayName( "clearHandlerCache removes all entries" )
	public void testClearCache() {
		Path				path		= Path.of( "src", "test", "resources", "Lambda.bx" ).toAbsolutePath();
		ResolvedFilePath	resolved	= ResolvedFilePath.of( path );
		IBoxContext			context		= buildContext( path );

		AzureFunctionRunner.getOrCompileHandler( resolved, context, false );
		assertThat( AzureFunctionRunner.isHandlerCached( path.toString() ) ).isTrue();

		AzureFunctionRunner.clearHandlerCache();

		assertThat( AzureFunctionRunner.isHandlerCached( path.toString() ) ).isFalse();
	}

	@Test
	@DisplayName( "Throws when .bx file path does not exist" )
	public void testNonExistentFileThrows() {
		Path				badPath		= Path.of( "src", "test", "resources", "DoesNotExist.bx" ).toAbsolutePath();
		ResolvedFilePath	resolved	= ResolvedFilePath.of( badPath );
		IBoxContext			context		= buildContext( badPath );

		try {
			AzureFunctionRunner.getOrCompileHandler( resolved, context, false );
			throw new AssertionError( "Expected an exception for a non-existent .bx file" );
		} catch ( Exception e ) {
			// Expected — BoxLang cannot load a file that doesn't exist
			assertThat( e ).isNotNull();
		}
	}
}
