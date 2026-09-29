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

import java.util.LinkedHashMap;
import java.util.Map;

import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;

/**
 * Test implementation of the Azure {@link HttpResponseMessage} interface, plus
 * its {@link HttpResponseMessage.Builder}, so tests can inspect what a handler
 * produced without a live Azure Functions host.
 */
public class MockHttpResponseMessage implements HttpResponseMessage {

	private final HttpStatusType		status;
	private final Map<String, String>	headers;
	private final Object				body;

	private MockHttpResponseMessage( HttpStatusType status, Map<String, String> headers, Object body ) {
		this.status		= status;
		this.headers	= headers;
		this.body		= body;
	}

	@Override
	public HttpStatusType getStatus() {
		return status;
	}

	@Override
	public String getHeader( String key ) {
		return headers.get( key );
	}

	@Override
	public Object getBody() {
		return body;
	}

	/**
	 * Read-only view of all headers set on this response - useful for assertions.
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Builder implementation used by {@code MockHttpRequestMessage.createResponseBuilder}.
	 */
	public static class Builder implements HttpResponseMessage.Builder {

		private HttpStatusType				status	= HttpStatus.OK;
		private final Map<String, String>	headers	= new LinkedHashMap<>();
		private Object						body;

		public Builder( HttpStatusType status ) {
			this.status = status;
		}

		@Override
		public HttpResponseMessage.Builder status( HttpStatusType status ) {
			this.status = status;
			return this;
		}

		@Override
		public HttpResponseMessage.Builder header( String key, String value ) {
			this.headers.put( key, value );
			return this;
		}

		@Override
		public HttpResponseMessage.Builder body( Object body ) {
			this.body = body;
			return this;
		}

		@Override
		public HttpResponseMessage build() {
			return new MockHttpResponseMessage( status, headers, body );
		}
	}
}
