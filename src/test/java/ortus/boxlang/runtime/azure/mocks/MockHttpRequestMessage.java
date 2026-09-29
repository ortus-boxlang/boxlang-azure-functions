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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;

/**
 * Test implementation of the Azure {@link HttpRequestMessage} interface.
 * <p>
 * Provides a fluent builder API for constructing mock requests in unit and
 * integration tests without requiring a live Azure Functions host:
 *
 * <pre>
 *
 * MockHttpRequestMessage req = new MockHttpRequestMessage( "GET", "/products" )
 *     .withHeader( "Accept", "application/json" )
 *     .withQueryParam( "page", "1" )
 *     .withBody( "{\"id\":1}" );
 * </pre>
 */
public class MockHttpRequestMessage implements HttpRequestMessage<Optional<String>> {

	private final HttpMethod			method;
	private final URI					uri;
	private final Map<String, String>	headers		= new LinkedHashMap<>();
	private final Map<String, String>	queryParams	= new LinkedHashMap<>();
	private String						body		= null;

	/**
	 * Construct a minimal mock request with the given HTTP method and path.
	 *
	 * @param method The HTTP method (GET, POST, PUT, DELETE, …)
	 * @param path   The URI path (e.g. {@code /products/123})
	 */
	public MockHttpRequestMessage( String method, String path ) {
		this.method	= HttpMethod.value( method.toUpperCase() );
		this.uri	= toUri( path );
	}

	private static URI toUri( String path ) {
		try {
			return new URI( "https", "localhost", path, null );
		} catch ( URISyntaxException e ) {
			throw new IllegalArgumentException( "Invalid mock path: " + path, e );
		}
	}

	// =========================================================================
	// Builder methods
	// =========================================================================

	public MockHttpRequestMessage withHeader( String name, String value ) {
		headers.put( name, value );
		return this;
	}

	public MockHttpRequestMessage withQueryParam( String name, String value ) {
		queryParams.put( name, value );
		return this;
	}

	public MockHttpRequestMessage withBody( String body ) {
		this.body = body;
		return this;
	}

	// =========================================================================
	// HttpRequestMessage implementation
	// =========================================================================

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public HttpMethod getHttpMethod() {
		return method;
	}

	@Override
	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public Map<String, String> getQueryParameters() {
		return queryParams;
	}

	@Override
	public Optional<String> getBody() {
		return Optional.ofNullable( body );
	}

	@Override
	public HttpResponseMessage.Builder createResponseBuilder( HttpStatus status ) {
		return new MockHttpResponseMessage.Builder( status );
	}

	@Override
	public HttpResponseMessage.Builder createResponseBuilder( HttpStatusType status ) {
		return new MockHttpResponseMessage.Builder( status );
	}
}
