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

import java.util.Map;
import java.util.Optional;

import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.JSONUtil;

/**
 * Builds an Azure Functions {@link HttpResponseMessage} from a BoxLang response
 * {@link IStruct}.
 * <p>
 * The response struct contract is:
 * <ul>
 * <li>{@code statusCode} – integer HTTP status code (default 200)</li>
 * <li>{@code headers} – a nested struct of response headers</li>
 * <li>{@code body} – the response body; may be a {@link String}, a BoxLang
 * struct/array (auto-serialized to JSON), or empty</li>
 * <li>{@code cookies} – array of cookie strings (written as {@code Set-Cookie}
 * headers)</li>
 * </ul>
 * <p>
 * Unlike AWS's {@code Map} response or GCF's mutable {@code HttpResponse}, Azure
 * responses are built via an immutable {@link HttpResponseMessage.Builder}, so
 * this mapper builds and returns the final message rather than writing into an
 * existing object.
 */
public final class AzureResponseMapper {

	/**
	 * Private constructor — this is a stateless utility class.
	 */
	private AzureResponseMapper() {
	}

	/**
	 * Build the Azure {@link HttpResponseMessage} from the BoxLang response struct.
	 *
	 * @param responseStruct The BoxLang response struct produced by the handler
	 * @param request        The original Azure request (needed to obtain a response builder)
	 *
	 * @return The built {@link HttpResponseMessage}
	 */
	public static HttpResponseMessage write( IStruct responseStruct, HttpRequestMessage<Optional<String>> request ) {
		int							statusCode	= extractStatusCode( responseStruct );

		HttpResponseMessage.Builder	builder		= request.createResponseBuilder( HttpStatus.valueOf( statusCode ) );

		// --- Headers ---
		Object						headersObj	= responseStruct.getOrDefault( Key.headers, new Struct() );
		if ( headersObj instanceof IStruct castedHeaders ) {
			for ( Map.Entry<Key, Object> entry : castedHeaders.entrySet() ) {
				builder.header( entry.getKey().getName(), StringCaster.cast( entry.getValue() ) );
			}
		}

		// --- Cookies → Set-Cookie headers ---
		Object cookiesObj = responseStruct.getOrDefault( Key.cookies, null );
		if ( cookiesObj instanceof Iterable<?> castedCookies ) {
			for ( Object cookie : castedCookies ) {
				builder.header( "Set-Cookie", StringCaster.cast( cookie ) );
			}
		}

		// --- Body ---
		builder.body( serializeBody( responseStruct.get( Key.body ) ) );

		return builder.build();
	}

	/**
	 * Extracts the HTTP status code from the response struct; defaults to 200.
	 */
	private static int extractStatusCode( IStruct responseStruct ) {
		Object statusObj = responseStruct.get( Key.statusCode );
		if ( statusObj instanceof Number castedStatus ) {
			return castedStatus.intValue();
		}
		if ( statusObj != null ) {
			try {
				return Integer.parseInt( statusObj.toString() );
			} catch ( NumberFormatException ignored ) {
				// keep default
			}
		}
		return 200;
	}

	/**
	 * Serializes the body value to a string.
	 * <ul>
	 * <li>Null → empty string</li>
	 * <li>Plain {@link String} → written verbatim</li>
	 * <li>Any other type (struct, array, number …) → JSON-serialized via Jackson</li>
	 * </ul>
	 */
	private static String serializeBody( Object body ) {
		if ( body == null ) {
			return "";
		}
		if ( body instanceof String castedBody ) {
			return castedBody;
		}
		try {
			return JSONUtil.getJSONBuilder().asString( body );
		} catch ( Exception e ) {
			// Last-resort fallback
			return body.toString();
		}
	}
}
