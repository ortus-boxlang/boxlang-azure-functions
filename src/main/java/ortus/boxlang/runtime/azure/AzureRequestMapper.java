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
package ortus.boxlang.runtime.azure;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;

import ortus.boxlang.runtime.azure.util.KeyDictionary;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Converts an incoming Azure Functions {@link HttpRequestMessage} into a
 * BoxLang-compatible event {@link IStruct}.
 * <p>
 * The produced struct intentionally mirrors the AWS API Gateway v2.0 (HTTP API)
 * event shape so that {@code .bx} handler files can run unmodified on the AWS,
 * Google Cloud, and Azure runtimes:
 *
 * <pre>
 * {
 *   method:               "GET",
 *   path:                 "/products",
 *   rawPath:              "/products",
 *   queryStringParameters: { page: "1" },
 *   headers:              { "content-type": "application/json" },
 *   body:                 "",
 *   requestContext: {
 *     http: {
 *       method: "GET",
 *       path:   "/products"
 *     }
 *   }
 * }
 * </pre>
 */
public final class AzureRequestMapper {

	/**
	 * Private constructor — this is a stateless utility class.
	 */
	private AzureRequestMapper() {
	}

	/**
	 * Convert an Azure {@link HttpRequestMessage} into a BoxLang event struct.
	 *
	 * @param request The incoming HTTP request from the Azure Functions host
	 *
	 * @return A BoxLang {@link IStruct} containing all relevant request fields
	 */
	public static IStruct toEventStruct( HttpRequestMessage<Optional<String>> request ) {
		String				method			= extractMethod( request );
		String				path			= extractPath( request );

		// --- Headers (lower-cased keys; Azure headers are already single-valued) ---
		IStruct				headersStruct	= new Struct();
		Map<String, String>	rawHeaders		= request.getHeaders();
		if ( rawHeaders != null ) {
			for ( Map.Entry<String, String> entry : rawHeaders.entrySet() ) {
				if ( entry.getKey() != null ) {
					headersStruct.put( entry.getKey().toLowerCase( Locale.ROOT ), entry.getValue() );
				}
			}
		}

		// --- Query parameters (Azure already pre-parses these) ---
		IStruct				queryParams	= new Struct();
		Map<String, String>	rawQuery	= request.getQueryParameters();
		if ( rawQuery != null ) {
			for ( Map.Entry<String, String> entry : rawQuery.entrySet() ) {
				queryParams.put( entry.getKey(), entry.getValue() );
			}
		}

		// --- Body ---
		String	body		= request.getBody() != null ? request.getBody().orElse( "" ) : "";

		// --- requestContext mirrors AWS API Gateway v2.0 HTTP context ---
		IStruct	httpContext	= Struct.of(
		    Key.method, method,
		    Key.path, path
		);

		return Struct.of(
		    Key.method, method,
		    Key.path, path,
		    KeyDictionary.rawPath, path,
		    KeyDictionary.queryStringParameters, queryParams,
		    Key.headers, headersStruct,
		    Key.body, body,
		    KeyDictionary.requestContext, Struct.of( Key.HTTP, httpContext )
		);
	}

	/**
	 * Converts the Azure {@link HttpMethod} enum value to an upper-case string.
	 */
	private static String extractMethod( HttpRequestMessage<Optional<String>> request ) {
		HttpMethod method = request.getHttpMethod();
		return method != null ? method.toString().toUpperCase( Locale.ROOT ) : "GET";
	}

	/**
	 * Extracts the raw path from the request URI, without query string.
	 * Defaults to {@code "/"} when the URI or path is unavailable.
	 */
	private static String extractPath( HttpRequestMessage<Optional<String>> request ) {
		URI uri = request.getUri();
		if ( uri == null || uri.getPath() == null || uri.getPath().isEmpty() ) {
			return "/";
		}
		return uri.getPath();
	}
}
