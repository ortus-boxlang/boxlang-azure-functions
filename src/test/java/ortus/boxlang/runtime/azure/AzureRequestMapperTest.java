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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.azure.mocks.MockHttpRequestMessage;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Unit tests for {@link AzureRequestMapper}.
 */
public class AzureRequestMapperTest {

	@Test
	@DisplayName( "Maps HTTP method and path into event struct" )
	public void testBasicFieldMapping() {
		MockHttpRequestMessage	request	= new MockHttpRequestMessage( "GET", "/products" );
		IStruct					event	= AzureRequestMapper.toEventStruct( request );

		assertThat( event.getAsString( Key.of( "method" ) ) ).isEqualTo( "GET" );
		assertThat( event.getAsString( Key.of( "path" ) ) ).isEqualTo( "/products" );
		assertThat( event.getAsString( Key.of( "rawPath" ) ) ).isEqualTo( "/products" );
	}

	@Test
	@DisplayName( "Maps headers into event struct with lowercase keys" )
	public void testHeaderMapping() {
		MockHttpRequestMessage	request	= new MockHttpRequestMessage( "GET", "/" )
		    .withHeader( "Content-Type", "application/json" )
		    .withHeader( "X-Custom-Header", "my-value" );
		IStruct					event	= AzureRequestMapper.toEventStruct( request );

		IStruct					headers	= ( IStruct ) event.get( Key.of( "headers" ) );
		assertThat( headers ).isNotNull();
		assertThat( headers.getAsString( Key.of( "content-type" ) ) ).isEqualTo( "application/json" );
		assertThat( headers.getAsString( Key.of( "x-custom-header" ) ) ).isEqualTo( "my-value" );
	}

	@Test
	@DisplayName( "Maps query parameters into event struct" )
	public void testQueryParameterMapping() {
		MockHttpRequestMessage	request		= new MockHttpRequestMessage( "GET", "/products" )
		    .withQueryParam( "page", "2" )
		    .withQueryParam( "limit", "10" );
		IStruct					event		= AzureRequestMapper.toEventStruct( request );

		IStruct					queryParams	= ( IStruct ) event.get( Key.of( "queryStringParameters" ) );
		assertThat( queryParams ).isNotNull();
		assertThat( queryParams.getAsString( Key.of( "page" ) ) ).isEqualTo( "2" );
		assertThat( queryParams.getAsString( Key.of( "limit" ) ) ).isEqualTo( "10" );
	}

	@Test
	@DisplayName( "Maps request body into event struct" )
	public void testBodyMapping() {
		String					body	= "{\"name\":\"Test Product\"}";
		MockHttpRequestMessage	request	= new MockHttpRequestMessage( "POST", "/products" )
		    .withBody( body );
		IStruct					event	= AzureRequestMapper.toEventStruct( request );

		assertThat( event.getAsString( Key.of( "body" ) ) ).isEqualTo( body );
	}

	@Test
	@DisplayName( "Maps empty body to empty string" )
	public void testEmptyBody() {
		MockHttpRequestMessage	request	= new MockHttpRequestMessage( "GET", "/" );
		IStruct					event	= AzureRequestMapper.toEventStruct( request );

		assertThat( event.getAsString( Key.of( "body" ) ) ).isEmpty();
	}

	@Test
	@DisplayName( "Builds requestContext with http.method and http.path mirroring AWS API Gateway v2.0 shape" )
	public void testRequestContextShape() {
		MockHttpRequestMessage	request			= new MockHttpRequestMessage( "POST", "/customers" );
		IStruct					event			= AzureRequestMapper.toEventStruct( request );

		IStruct					requestContext	= ( IStruct ) event.get( Key.of( "requestContext" ) );
		assertThat( requestContext ).isNotNull();

		IStruct http = ( IStruct ) requestContext.get( Key.of( "http" ) );
		assertThat( http ).isNotNull();
		assertThat( http.getAsString( Key.of( "method" ) ) ).isEqualTo( "POST" );
		assertThat( http.getAsString( Key.of( "path" ) ) ).isEqualTo( "/customers" );
	}

	@Test
	@DisplayName( "Handles requests with no headers or query params gracefully" )
	public void testMinimalRequest() {
		MockHttpRequestMessage	request	= new MockHttpRequestMessage( "DELETE", "/items/42" );
		IStruct					event	= AzureRequestMapper.toEventStruct( request );

		assertThat( event.getAsString( Key.of( "method" ) ) ).isEqualTo( "DELETE" );
		assertThat( ( IStruct ) event.get( Key.of( "headers" ) ) ).isNotNull();
		assertThat( ( IStruct ) event.get( Key.of( "queryStringParameters" ) ) ).isNotNull();
	}
}
