package org.marekasf.vertx_hystrix;

import java.util.List;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClientResponse;

public class Response
{
	public final Buffer body;
	private final HttpClientResponse response;

	public int statusCode()
	{
		return response.statusCode();
	}

	public String statusMessage()
	{
		return response.statusMessage();
	}

	public MultiMap headers()
	{
		return response.headers();
	}

	public List<String> cookies()
	{
		return response.cookies();
	}

	public Response(final HttpClientResponse response, final Buffer body)
	{
		this.response = response;
		this.body = body;
	}
}
