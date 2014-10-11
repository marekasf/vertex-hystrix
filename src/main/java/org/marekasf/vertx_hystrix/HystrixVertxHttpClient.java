package org.marekasf.vertx_hystrix;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.logging.Logger;

public class HystrixVertxHttpClient
{

	private final int timeoutMs;
	private final HttpClient client;
	private final String group;
	private final Logger logger;

	public HystrixVertxHttpClient(final HttpClient client, final int timeoutMs, final String group, final Logger logger)
	{
		this.timeoutMs = timeoutMs;
		this.client = client;
		this.group = group;
		this.logger = logger;
	}

	public String getHost()
	{
		return client.getHost();
	}

	public RequestBuilder delete(String name, String uri, Handler<Response> responseHandler)
	{
		return new RequestBuilder(client, name, group, logger).timeoutMs(timeoutMs).delete(uri, responseHandler);
	}

	public RequestBuilder post(String name, String uri, Handler<Response> responseHandler)
	{
		return new RequestBuilder(client, name, group, logger).timeoutMs(timeoutMs).post(uri, responseHandler);
	}

	public RequestBuilder get(String name, String uri, Handler<Response> responseHandler)
	{
		return new RequestBuilder(client, name, group, logger).timeoutMs(timeoutMs).get(uri, responseHandler);
	}
}
