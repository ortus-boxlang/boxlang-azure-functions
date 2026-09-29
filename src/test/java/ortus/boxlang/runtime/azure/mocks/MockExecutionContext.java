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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.azure.mocks;

import java.util.UUID;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;

/**
 * Test implementation of the Azure {@link ExecutionContext} interface.
 */
public class MockExecutionContext implements ExecutionContext {

	private final String	invocationId;
	private final String	functionName;
	private final Logger	logger	= Logger.getLogger( "MockExecutionContext" );

	public MockExecutionContext() {
		this( UUID.randomUUID().toString(), "BoxLangFunction" );
	}

	public MockExecutionContext( String invocationId, String functionName ) {
		this.invocationId	= invocationId;
		this.functionName	= functionName;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public String getInvocationId() {
		return invocationId;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}
}
