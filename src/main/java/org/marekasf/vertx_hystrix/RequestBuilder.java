package org.marekasf.vertx_hystrix;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.logging.Logger;

import rx.Observable;
import rx.Subscriber;

public class RequestBuilder
{
	private Logger logger;
	private String name;
	private String group;
	private Handler<Response> responseHandler;
	private Observable<Response> request;
	private List<Consumer<HttpClientRequest>> commands = new LinkedList<>();
	private HttpClient client;
	private int timeoutMs = 1000;


	public RequestBuilder(final HttpClient client, final String name, final String group, final Logger logger)
	{
		this.name = name;
		this.group = group;
		this.client = client;
		this.logger = logger;
	}

	public RequestBuilder delete(final String uri, final Handler<Response> responseHandler)
	{
		request = Observable.create((Subscriber<? super Response> subscriber) -> request("DELETE", uri, subscriber));
		this.responseHandler = responseHandler;
		return this;
	}

	public RequestBuilder post(final String uri, final Handler<Response> responseHandler)
	{
		request = Observable.create((Subscriber<? super Response> subscriber) -> request("POST", uri, subscriber));
		this.responseHandler = responseHandler;
		return this;
	}

	public RequestBuilder get(final String uri, final Handler<Response> responseHandler)
	{
		request = Observable.create((Subscriber<? super Response> subscriber) -> request("GET", uri, subscriber));
		this.responseHandler = responseHandler;
		return this;
	}

	private void request(final String method, final String uri, final Subscriber<? super Response> subscriber)
	{
		try
		{
			if (logger.isDebugEnabled())
				logger.debug("request " + method + " of " + name + ":" + group);
			final HttpClientRequest rc = client.request(method, uri, event -> handle(event, subscriber));
			commands.forEach(a -> a.accept(rc));
			rc.end();
		} catch (Throwable t)
		{
			subscriber.onError(t);
		}
	}

	public Observable<Response> end()
	{
		return new HystrixCommand<>(name, group, timeoutMs, logger, () -> request).toObservable();
	}

	private void handle(final HttpClientResponse response, final Subscriber<? super Response> subscriber)
	{
		response.bodyHandler(body -> {
			try
			{
				final Response ret = new Response(response, body);
				responseHandler.handle(ret);
				subscriber.onNext(ret);
				subscriber.onCompleted();
			}
			catch (Throwable t)
			{
				subscriber.onError(t);
			}
		});
	}

	public RequestBuilder putHeader(final CharSequence name, final String value)
	{
		commands.add(requestClient -> requestClient.putHeader(name, value));
		return this;
	}

	public RequestBuilder write(final Buffer payload)
	{
		commands.add(requestClient -> requestClient.write(payload));
		return this;
	}

	public RequestBuilder timeoutMs(final int timeoutMs)
	{
		this.timeoutMs = timeoutMs;
		return this;
	}
}
